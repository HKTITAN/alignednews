package ai.aligned.ui.research

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ai.aligned.net.AlignedApi
import ai.aligned.net.dto.Insight
import ai.aligned.net.dto.ResearchStep
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ResearchState(
    val researchId: String? = null,
    val status: String = "idle",   // idle | running | complete | error
    val steps: List<ResearchStep> = emptyList(),
    val insights: List<Insight> = emptyList(),
    val answer: String? = null,
    val error: String? = null
)

@HiltViewModel
class ResearchViewModel @Inject constructor(
    private val api: AlignedApi
) : ViewModel() {
    val query: MutableState<String> = mutableStateOf("")
    private val _state = MutableStateFlow(ResearchState())
    val state: StateFlow<ResearchState> = _state.asStateFlow()
    private var pollJob: Job? = null

    fun setQuery(s: String) { query.value = s }

    fun start() {
        val q = query.value.trim()
        if (q.isBlank()) return
        pollJob?.cancel()
        _state.value = ResearchState(status = "running")
        viewModelScope.launch {
            runCatching { api.startResearch(q) }
                .onSuccess { r ->
                    _state.update { it.copy(researchId = r.id) }
                    pollJob = launch { pollLoop(r.id) }
                }
                .onFailure { e ->
                    _state.update { it.copy(status = "error", error = e.message) }
                }
        }
    }

    private suspend fun pollLoop(id: String) {
        while (true) {
            val r = runCatching { api.research(id) }.getOrNull()
            if (r != null) {
                _state.update {
                    it.copy(
                        steps = r.steps,
                        insights = r.insights,
                        answer = r.summaryAnswer
                    )
                }
                if (r.status == "complete") {
                    _state.update { it.copy(status = "complete") }
                    return
                }
            }
            delay(3500)
        }
    }

    override fun onCleared() {
        pollJob?.cancel()
        super.onCleared()
    }
}
