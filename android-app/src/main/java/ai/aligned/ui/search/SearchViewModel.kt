package ai.aligned.ui.search

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ai.aligned.net.AlignedApi
import ai.aligned.net.dto.StoryDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface SearchState {
    data object Idle : SearchState
    data object Searching : SearchState
    data class Results(val query: String, val stories: List<StoryDto>) : SearchState
    data class Error(val message: String) : SearchState
}

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val api: AlignedApi
) : ViewModel() {
    val query: MutableState<String> = mutableStateOf("")
    private val _state = MutableStateFlow<SearchState>(SearchState.Idle)
    val state: StateFlow<SearchState> = _state.asStateFlow()

    private var debouncer: Job? = null

    fun onQuery(text: String) {
        query.value = text
        debouncer?.cancel()
        if (text.isBlank()) {
            _state.value = SearchState.Idle
            return
        }
        debouncer = viewModelScope.launch {
            delay(280)
            _state.value = SearchState.Searching
            runCatching { api.search(text) }
                .onSuccess { dto -> _state.value = SearchState.Results(text, dto.stories) }
                .onFailure { e -> _state.value = SearchState.Error(e.message ?: "Search failed") }
        }
    }
}
