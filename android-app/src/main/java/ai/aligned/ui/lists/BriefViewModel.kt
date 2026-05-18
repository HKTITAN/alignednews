package ai.aligned.ui.lists

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ai.aligned.data.StoryRepository
import ai.aligned.ui.theme.Tokens
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BriefGroup(
    val id: String, val name: String,
    val summary: String, val storyCount: Int,
    val color: Color
)

sealed interface BriefState {
    data object Loading : BriefState
    data class Ready(
        val dateLabel: String,
        val execSummary: String?,
        val groups: List<BriefGroup>
    ) : BriefState
    data class Error(val message: String) : BriefState
}

@HiltViewModel
class BriefViewModel @Inject constructor(
    private val repo: StoryRepository
) : ViewModel() {
    private val _state = MutableStateFlow<BriefState>(BriefState.Loading)
    val state: StateFlow<BriefState> = _state.asStateFlow()

    init { refresh() }

    fun refresh(force: Boolean = false) {
        viewModelScope.launch {
            if (_state.value !is BriefState.Ready) _state.value = BriefState.Loading
            runCatching { repo.lists(forceRefresh = force) }
                .onSuccess { dto ->
                    val groups = dto.groups.map { g ->
                        BriefGroup(
                            id = g.groupId,
                            name = g.groupName,
                            summary = g.execSummary,
                            storyCount = g.storyCount,
                            color = Tokens.categoryColor(g.groupId)
                        )
                    }
                    _state.value = BriefState.Ready(
                        dateLabel = dto.overview?.date ?: "Today",
                        execSummary = dto.overview?.execSummary,
                        groups = groups
                    )
                }
                .onFailure { e ->
                    _state.value = BriefState.Error(e.message ?: "Couldn't load brief")
                }
        }
    }
}
