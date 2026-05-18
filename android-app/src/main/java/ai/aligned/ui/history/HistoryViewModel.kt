package ai.aligned.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ai.aligned.data.StoryRepository
import ai.aligned.net.dto.HistoryEntryDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface HistoryState {
    data object Loading : HistoryState
    data class Ready(val entries: List<HistoryEntryDto>) : HistoryState
    data class Error(val message: String) : HistoryState
}

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val repo: StoryRepository
) : ViewModel() {
    private val _state = MutableStateFlow<HistoryState>(HistoryState.Loading)
    val state: StateFlow<HistoryState> = _state.asStateFlow()

    init { refresh() }

    fun refresh(force: Boolean = false) {
        viewModelScope.launch {
            if (_state.value !is HistoryState.Ready) _state.value = HistoryState.Loading
            runCatching { repo.history(forceRefresh = force) }
                .onSuccess { _state.value = HistoryState.Ready(it) }
                .onFailure { _state.value = HistoryState.Error(it.message ?: "Couldn't load history") }
        }
    }
}
