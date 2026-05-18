package ai.aligned.ui.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ai.aligned.data.AlignedDb
import ai.aligned.data.SyncLogRow
import ai.aligned.net.AlignedApi
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SyncLogUi(val tier: String, val ok: Boolean, val message: String, val ago: String)

data class SettingsState(
    val health: String = "—",
    val storyCount: Int? = null,
    val lastUpdated: String? = null,
    val syncLog: List<SyncLogUi> = emptyList(),
    val breakingEnabled: Boolean = false,
    val topicsEnabled: Boolean = true,
    val briefEnabled: Boolean = true,
    val researchEnabled: Boolean = true
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val api: AlignedApi,
    private val db: AlignedDb,
    @ApplicationContext private val ctx: Context
) : ViewModel() {
    private val prefs: SharedPreferences = ctx.getSharedPreferences("aligned.settings", Context.MODE_PRIVATE)

    private val _state = MutableStateFlow(
        SettingsState(
            breakingEnabled  = prefs.getBoolean("n_breaking", false),
            topicsEnabled    = prefs.getBoolean("n_topics", true),
            briefEnabled     = prefs.getBoolean("n_brief", true),
            researchEnabled  = prefs.getBoolean("n_research", true)
        )
    )
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    init {
        refreshHealth()
        loadSyncLog()
    }

    private fun refreshHealth() {
        viewModelScope.launch {
            runCatching { api.health() }.onSuccess { h ->
                _state.update {
                    it.copy(
                        health = h.status,
                        storyCount = h.storyCount,
                        lastUpdated = relativeFromIso(h.lastUpdated)
                    )
                }
            }
        }
    }

    private fun loadSyncLog() {
        viewModelScope.launch {
            val rows = runCatching { db.syncLog().recent(10) }.getOrDefault(emptyList())
            _state.update { it.copy(syncLog = rows.map { it.toUi() }) }
        }
    }

    fun toggleBreaking() { persistFlip("n_breaking") { it.copy(breakingEnabled = !it.breakingEnabled) } }
    fun toggleTopics()   { persistFlip("n_topics")   { it.copy(topicsEnabled   = !it.topicsEnabled) } }
    fun toggleBrief()    { persistFlip("n_brief")    { it.copy(briefEnabled    = !it.briefEnabled) } }
    fun toggleResearch() { persistFlip("n_research") { it.copy(researchEnabled = !it.researchEnabled) } }

    private fun persistFlip(key: String, mutate: (SettingsState) -> SettingsState) {
        _state.update(mutate)
        val v = when (key) {
            "n_breaking" -> _state.value.breakingEnabled
            "n_topics"   -> _state.value.topicsEnabled
            "n_brief"    -> _state.value.briefEnabled
            "n_research" -> _state.value.researchEnabled
            else         -> return
        }
        prefs.edit().putBoolean(key, v).apply()
    }
}

private fun SyncLogRow.toUi() = SyncLogUi(
    tier = tier, ok = ok, message = message, ago = relativeFromEpoch(tsEpochMs)
)

private fun relativeFromEpoch(ts: Long): String {
    val secs = (System.currentTimeMillis() - ts) / 1000
    return when {
        secs < 60 -> "${secs}s"
        secs < 3600 -> "${secs / 60}m"
        secs < 86400 -> "${secs / 3600}h"
        else -> "${secs / 86400}d"
    }
}

private fun relativeFromIso(iso: String): String = iso
