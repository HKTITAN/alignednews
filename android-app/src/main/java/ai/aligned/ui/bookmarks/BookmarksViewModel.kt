package ai.aligned.ui.bookmarks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ai.aligned.data.StoryRepository
import ai.aligned.net.dto.StoryDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BookmarksViewModel @Inject constructor(
    private val repo: StoryRepository
) : ViewModel() {
    private val _bookmarks = MutableStateFlow<List<StoryDto>>(emptyList())
    val bookmarks: StateFlow<List<StoryDto>> = _bookmarks.asStateFlow()

    init {
        viewModelScope.launch {
            repo.observeBookmarks().collect { _bookmarks.value = it }
        }
    }
}
