package ai.aligned.ui.story

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ai.aligned.net.AlignedApi
import ai.aligned.net.Vote
import ai.aligned.net.dto.StoryDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StoryState(
    val loading: Boolean = true,
    val story: StoryDto? = null,
    val error: String? = null,
    val lastVote: String = ""
)

@HiltViewModel
class StoryViewModel @Inject constructor(
    private val api: AlignedApi,
    savedState: SavedStateHandle
) : ViewModel() {
    private val storyId: String = checkNotNull(savedState["id"])
    private val _state = MutableStateFlow(StoryState())
    val state: StateFlow<StoryState> = _state.asStateFlow()

    init { reload() }

    fun reload() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            runCatching { api.story(storyId) }
                .onSuccess { s -> _state.update { it.copy(loading = false, story = s) } }
                .onFailure { e -> _state.update { it.copy(loading = false, error = e.message) } }
        }
    }

    fun vote(up: Boolean) {
        val s = _state.value.story ?: return
        viewModelScope.launch {
            runCatching { api.feedback(s.id, s.category, if (up) Vote.Up else Vote.Down) }
                .onSuccess { _state.update { it.copy(lastVote = if (up) "Marked useful" else "Marked not useful") } }
                .onFailure { e -> _state.update { it.copy(error = e.message) } }
        }
    }
}
