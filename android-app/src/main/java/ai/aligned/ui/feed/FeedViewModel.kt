package ai.aligned.ui.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ai.aligned.data.StoryRepository
import ai.aligned.net.dto.StoryDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface FeedState {
    data object Loading : FeedState
    data class Ready(val stories: List<StoryDto>, val refreshing: Boolean = false) : FeedState
    data class Error(val message: String) : FeedState
}

@HiltViewModel
class FeedViewModel @Inject constructor(
    private val repo: StoryRepository
) : ViewModel() {
    private val _state = MutableStateFlow<FeedState>(FeedState.Loading)
    val state: StateFlow<FeedState> = _state.asStateFlow()

    init {
        observeCache()
        refresh()
    }

    private fun observeCache() {
        viewModelScope.launch {
            repo.observe().collect { cached ->
                if (cached.isNotEmpty()) {
                    val refreshing = (_state.value as? FeedState.Ready)?.refreshing == true
                    _state.value = FeedState.Ready(cached, refreshing = refreshing)
                }
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            val current = _state.value
            if (current is FeedState.Ready) _state.value = current.copy(refreshing = true)
            runCatching { repo.refresh() }
                .onFailure { e ->
                    val s = _state.value
                    if (s is FeedState.Ready) _state.update { (it as FeedState.Ready).copy(refreshing = false) }
                    else _state.value = FeedState.Error(e.message ?: "Network error")
                }
        }
    }
}
