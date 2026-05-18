package ai.aligned.ui.feed

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ai.aligned.data.StoryRepository
import ai.aligned.net.dto.CategoryDto
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

    val selectedCategory: MutableState<String?> = mutableStateOf(null)
    var knownCategories: List<String> = emptyList()
        private set

    /** Live /api/categories result — empty until first network success; cached afterwards. */
    var liveCategories: List<CategoryDto> = emptyList()
        private set

    fun categoryLabel(id: String): String =
        liveCategories.firstOrNull { it.id == id }?.label
            ?: id.split('-').joinToString(" ") { it.replaceFirstChar(Char::uppercase) }

    fun categoryColor(id: String): Color {
        val hex = liveCategories.firstOrNull { it.id == id }?.color
        return parseHex(hex) ?: ai.aligned.ui.theme.Tokens.categoryColor(id)
    }

    init {
        observeCache()
        refresh()
        loadCategories()
    }

    private fun loadCategories() {
        viewModelScope.launch {
            runCatching { repo.categories() }.onSuccess { dto ->
                liveCategories = dto.all.ifEmpty { dto.categories }
            }
        }
    }

    private fun observeCache() {
        viewModelScope.launch {
            repo.observe().collect { cached ->
                if (cached.isNotEmpty()) {
                    val refreshing = (_state.value as? FeedState.Ready)?.refreshing == true
                    _state.value = FeedState.Ready(cached, refreshing = refreshing)
                    knownCategories = cached
                        .map { it.category }
                        .distinct()
                        .sorted()
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

    fun selectCategory(cat: String?) {
        selectedCategory.value = cat
    }
}

private fun parseHex(hex: String?): Color? {
    if (hex.isNullOrBlank()) return null
    val v = hex.removePrefix("#")
    return runCatching {
        val long = v.toLong(16)
        when (v.length) {
            6 -> Color(0xFF000000 or long)
            8 -> Color(long)
            else -> null
        }
    }.getOrNull()
}
