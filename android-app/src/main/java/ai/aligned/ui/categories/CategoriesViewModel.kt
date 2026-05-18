package ai.aligned.ui.categories

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ai.aligned.data.StoryRepository
import ai.aligned.ui.theme.Tokens
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CategoryRow(
    val id: String, val label: String, val color: Color
)

sealed interface CategoriesState {
    data object Loading : CategoriesState
    data class Ready(val categories: List<CategoryRow>) : CategoriesState
    data class Error(val message: String) : CategoriesState
}

@HiltViewModel
class CategoriesViewModel @Inject constructor(
    private val repo: StoryRepository
) : ViewModel() {
    private val _state = MutableStateFlow<CategoriesState>(CategoriesState.Loading)
    val state: StateFlow<CategoriesState> = _state.asStateFlow()

    val pinnedIds: StateFlow<Set<String>> = repo.observePinnedTopics()
        .map { it.toSet() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    init {
        viewModelScope.launch {
            runCatching { repo.categories() }
                .onSuccess { dto ->
                    val list = (dto.all.ifEmpty { dto.categories }).map { c ->
                        CategoryRow(
                            id = c.id,
                            label = c.label,
                            color = parseHex(c.color) ?: Tokens.categoryColor(c.id)
                        )
                    }
                    _state.value = CategoriesState.Ready(list)
                }
                .onFailure { e ->
                    _state.value = CategoriesState.Error(e.message ?: "Couldn't load categories")
                }
        }
    }

    fun togglePin(id: String) {
        viewModelScope.launch { repo.togglePinnedTopic(id) }
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
