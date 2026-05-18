package ai.aligned.sync

import ai.aligned.net.AlignedApi
import ai.aligned.net.dto.NewsDto
import ai.aligned.net.dto.ListsDto
import ai.aligned.net.dto.MapDto
import ai.aligned.net.dto.EventsDto
import ai.aligned.net.dto.CategoriesDto
import ai.aligned.net.dto.AccountsDto
import ai.aligned.net.dto.SettingsResponseDto
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll

/**
 * Pure-Kotlin tier scheduler. The platform layer (WorkManager on Android,
 * BackgroundTasks on Windows — though Windows has its own C# mirror)
 * is responsible for actually firing [runHot]/[runWarm]/[runCold] on a cadence.
 *
 * Tiers (see PLAN.md §4):
 *   - HOT  every 5 min  → /api/news, /api/health
 *   - WARM every 30 min → /api/lists overview, /api/events, /api/map
 *   - COLD every 6 h    → /api/categories, /api/accounts, /api/settings, /api/history
 */
class SyncEngine(
    private val api: AlignedApi,
    private val cache: SyncCache
) {
    suspend fun runHot(): HotResult = coroutineScope {
        val news   = async { runCatching { api.news() }.getOrNull() }
        val health = async { runCatching { api.health() }.getOrNull() }
        val n = news.await();   if (n != null) cache.saveNews(n)
        val h = health.await(); if (h != null) cache.saveHealth(h)
        HotResult(news = n, healthOk = h?.status == "ok")
    }

    suspend fun runWarm(): WarmResult = coroutineScope {
        val lists  = async { runCatching { api.lists() }.getOrNull() }
        val events = async { runCatching { api.events() }.getOrNull() }
        val map    = async { runCatching { api.map() }.getOrNull() }
        awaitAll(lists, events, map)
        WarmResult(lists.await(), events.await(), map.await()).also { cache.saveWarm(it) }
    }

    suspend fun runCold(): ColdResult = coroutineScope {
        val cats      = async { runCatching { api.categories() }.getOrNull() }
        val accounts  = async { runCatching { api.accounts() }.getOrNull() }
        val settings  = async { runCatching { api.settings() }.getOrNull() }
        ColdResult(cats.await(), accounts.await(), settings.await()).also { cache.saveCold(it) }
    }
}

data class HotResult(val news: NewsDto?, val healthOk: Boolean)
data class WarmResult(val lists: ListsDto?, val events: EventsDto?, val map: MapDto?)
data class ColdResult(val categories: CategoriesDto?, val accounts: AccountsDto?, val settings: SettingsResponseDto?)

/**
 * Platform-implemented cache contract. Android uses Room, Windows uses Microsoft.Data.Sqlite.
 */
interface SyncCache {
    suspend fun saveNews(n: NewsDto)
    suspend fun saveHealth(h: ai.aligned.net.dto.HealthDto)
    suspend fun saveWarm(w: WarmResult)
    suspend fun saveCold(c: ColdResult)
}
