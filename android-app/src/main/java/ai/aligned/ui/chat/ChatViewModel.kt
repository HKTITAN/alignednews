package ai.aligned.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ai.aligned.net.AlignedApi
import ai.aligned.net.dto.ChatEvent
import ai.aligned.net.dto.ChatTurn
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class Msg(val role: String, val content: String)

data class ChatState(
    val messages: List<Msg> = emptyList(),
    val input: String = "",
    val streaming: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val api: AlignedApi
) : ViewModel() {
    private val _state = MutableStateFlow(ChatState())
    val state: StateFlow<ChatState> = _state.asStateFlow()
    private var streamJob: Job? = null

    fun setInput(s: String) = _state.update { it.copy(input = s) }

    fun send() {
        val s = _state.value
        if (s.input.isBlank() || s.streaming) return
        val prompt = s.input.trim()
        val history = s.messages.map { ChatTurn(it.role, it.content) }
        _state.update {
            it.copy(
                input = "",
                streaming = true,
                error = null,
                messages = it.messages + Msg("user", prompt) + Msg("assistant", "")
            )
        }
        streamJob = viewModelScope.launch {
            val buf = StringBuilder()
            try {
                api.chat(message = prompt, history = history).collect { evt ->
                    when (evt) {
                        is ChatEvent.Token -> {
                            buf.append(evt.text)
                            val snap = buf.toString()
                            _state.update { st ->
                                val msgs = st.messages.toMutableList()
                                msgs[msgs.lastIndex] = msgs.last().copy(content = snap)
                                st.copy(messages = msgs)
                            }
                        }
                        ChatEvent.Done -> Unit
                    }
                }
            } catch (e: Throwable) {
                _state.update { it.copy(error = e.message) }
            } finally {
                _state.update { it.copy(streaming = false) }
            }
        }
    }

    fun cancel() { streamJob?.cancel(); _state.update { it.copy(streaming = false) } }

    fun clear() {
        streamJob?.cancel()
        _state.value = ChatState()
    }
}
