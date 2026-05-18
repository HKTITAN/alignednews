package ai.aligned.ui.map

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

data class UiMarker(
    val city: String, val country: String,
    val groupId: String, val groupName: String,
    val color: Color,
    val headlines: List<String>
)

sealed interface MapState {
    data object Loading : MapState
    data class Ready(val markers: List<UiMarker>) : MapState
    data class Error(val message: String) : MapState
}

@HiltViewModel
class MapViewModel @Inject constructor(
    private val repo: StoryRepository
) : ViewModel() {
    private val _state = MutableStateFlow<MapState>(MapState.Loading)
    val state: StateFlow<MapState> = _state.asStateFlow()

    init { refresh() }

    fun refresh(force: Boolean = false) {
        viewModelScope.launch {
            if (_state.value !is MapState.Ready) _state.value = MapState.Loading
            runCatching { repo.map(forceRefresh = force) }
                .onSuccess { dto ->
                    val ui = dto.markers.map { m ->
                        UiMarker(
                            city = m.city, country = m.country,
                            groupId = m.groupId, groupName = m.groupName,
                            color = parseHex(m.groupColor) ?: Tokens.categoryColor(m.groupId),
                            headlines = m.stories.map { it.headline }
                        )
                    }
                    _state.value = MapState.Ready(ui)
                }
                .onFailure { e ->
                    _state.value = MapState.Error(e.message ?: "Couldn't load map")
                }
        }
    }
}

private fun parseHex(hex: String): Color? = runCatching {
    val v = hex.removePrefix("#")
    val long = v.toLong(16)
    when (v.length) {
        6 -> Color(0xFF000000 or long)
        8 -> Color(long)
        else -> null
    }
}.getOrNull()
