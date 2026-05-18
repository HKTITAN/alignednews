package ai.aligned.ui.events

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ai.aligned.data.StoryRepository
import ai.aligned.net.dto.EventDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface EventsState {
    data object Loading : EventsState
    data class Ready(val events: List<EventDto>) : EventsState
    data class Error(val message: String) : EventsState
}

@HiltViewModel
class EventsViewModel @Inject constructor(
    private val repo: StoryRepository
) : ViewModel() {
    private val _state = MutableStateFlow<EventsState>(EventsState.Loading)
    val state: StateFlow<EventsState> = _state.asStateFlow()

    init { refresh() }

    fun refresh(force: Boolean = false) {
        viewModelScope.launch {
            if (_state.value !is EventsState.Ready) _state.value = EventsState.Loading
            runCatching { repo.events(forceRefresh = force) }
                .onSuccess { _state.value = EventsState.Ready(it.events) }
                .onFailure { _state.value = EventsState.Error(it.message ?: "Couldn't load events") }
        }
    }
}
