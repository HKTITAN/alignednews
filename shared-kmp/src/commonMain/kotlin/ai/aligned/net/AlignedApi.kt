package ai.aligned.net

import ai.aligned.net.dto.*
import io.ktor.client.*
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.http.encodeURLQueryComponent
import io.ktor.serialization.kotlinx.json.json
import io.ktor.utils.io.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * The complete reverse-engineered surface of https://alignednews.ai/api,
 * documented in 4. aligned news/ALIGNEDNEWS_API.md.
 *
 * Hard rules (enforced here, not optional):
 *
 *   1. We never `POST /api/settings`. The backend stores it globally and unauth'd;
 *      writing it would change every user's site. There is no `updateSettings(...)`
 *      method on this class. User preferences live in [ai.aligned.settings.SettingsStore].
 *   2. We never `POST /api/refresh`. Triggering server-side ingest is operator-only.
 *
 * Everything else is read-only or per-session.
 */
class AlignedApi(
    private val baseUrl: String = DEFAULT_BASE_URL,
    private val httpClient: HttpClient = defaultClient()
) {
    companion object {
        const val DEFAULT_BASE_URL = "https://alignednews.ai"
        const val RAILWAY_ORIGIN   = "https://x-news-stream-robert-production.up.railway.app"

        private val json = Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
            explicitNulls = false
        }

        fun defaultClient(): HttpClient = HttpClient {
            install(ContentNegotiation) { json(json) }
            install(Logging) { level = LogLevel.INFO }
        }
    }

    // ─── Liveness & catalogs ─────────────────────────────────────────────────

    suspend fun health(): HealthDto =
        httpClient.get("$baseUrl/api/health").body()

    suspend fun categories(): CategoriesDto =
        httpClient.get("$baseUrl/api/categories").body()

    suspend fun accounts(): AccountsDto =
        httpClient.get("$baseUrl/api/accounts").body()

    suspend fun settings(): SettingsResponseDto =
        httpClient.get("$baseUrl/api/settings").body()

    // ─── Feed ────────────────────────────────────────────────────────────────

    suspend fun news(category: String? = null, limit: Int? = null): NewsDto =
        httpClient.get("$baseUrl/api/news") {
            category?.let { parameter("category", it) }
            limit?.let { parameter("limit", it) }
        }.body()

    suspend fun story(id: String): StoryDto =
        httpClient.get("$baseUrl/api/news/$id").body()

    suspend fun search(query: String, limit: Int = 12): SearchDto =
        httpClient.get("$baseUrl/api/search") {
            parameter("q", query)
            parameter("limit", limit)
        }.body()

    // ─── Editorial roll-up ───────────────────────────────────────────────────

    suspend fun lists(group: String? = null, date: String? = null): ListsDto =
        httpClient.get("$baseUrl/api/lists") {
            group?.let { parameter("group", it) }
            date?.let  { parameter("date",  it) }
        }.body()

    // ─── Geo, events, history, feedback ──────────────────────────────────────

    suspend fun map(): MapDto       = httpClient.get("$baseUrl/api/map").body()
    suspend fun events(): EventsDto = httpClient.get("$baseUrl/api/events").body()
    suspend fun history(): List<HistoryEntryDto> = httpClient.get("$baseUrl/api/history").body()
    suspend fun feedbackStats(): FeedbackStatsDto = httpClient.get("$baseUrl/api/feedback").body()

    /** Cast a thumbs-up/down on a story. */
    suspend fun feedback(storyId: String, category: String, vote: Vote): FeedbackPostDto =
        httpClient.post("$baseUrl/api/feedback") {
            contentType(ContentType.Application.Json)
            setBody(FeedbackRequest(storyId, category, vote.wire))
        }.body()

    // ─── Summarize (stateless LLM proxy on the backend) ──────────────────────

    suspend fun summarize(tweets: List<TweetInput>): SummarizeDto =
        httpClient.post("$baseUrl/api/summarize") {
            contentType(ContentType.Application.Json)
            setBody(SummarizeRequest(tweets))
        }.body()

    // ─── Chat (Server-Sent Events) ───────────────────────────────────────────

    /**
     * Streams chat tokens as they arrive. Each emission is one [ChatEvent].
     * Terminates when the server emits `{"type":"done"}`.
     */
    fun chat(message: String, storyId: String? = null, history: List<ChatTurn> = emptyList()): Flow<ChatEvent> = flow {
        val response = httpClient.post("$baseUrl/api/chat") {
            contentType(ContentType.Application.Json)
            setBody(ChatRequest(message, storyId, history))
        }
        val channel: ByteReadChannel = response.bodyAsChannel()
        val buf = StringBuilder()
        while (!channel.isClosedForRead) {
            val line = channel.readUTF8Line() ?: break
            if (!line.startsWith("data:")) continue
            val payload = line.removePrefix("data:").trim()
            if (payload.isEmpty() || payload == "[DONE]") continue
            val evt = parseChatEvent(payload) ?: continue
            emit(evt)
            if (evt is ChatEvent.Done) break
            buf.append(evt.toString())
        }
    }

    // ─── Research (kick off + poll) ─────────────────────────────────────────

    suspend fun startResearch(query: String): ResearchStartDto =
        httpClient.post("$baseUrl/api/research") {
            contentType(ContentType.Application.Json)
            setBody(ResearchStartRequest(query))
        }.body()

    suspend fun research(id: String): ResearchDto =
        httpClient.get("$baseUrl/api/research") { parameter("id", id) }.body()

    // ─── Share-card image (binary) ───────────────────────────────────────────

    fun ogUrl(title: String? = null, subtitle: String? = null, storyId: String? = null): String =
        buildString {
            append(baseUrl); append("/api/og")
            val q = listOfNotNull(
                title?.let    { "title="    + it.urlEncode() },
                subtitle?.let { "subtitle=" + it.urlEncode() },
                storyId?.let  { "storyId="  + it.urlEncode() }
            )
            if (q.isNotEmpty()) { append("?"); append(q.joinToString("&")) }
        }

    fun infographicUrl(storyId: String): String = "$baseUrl/api/infographic?id=${storyId.urlEncode()}"
}

enum class Vote(val wire: String) { Up("up"), Down("down") }

private fun String.urlEncode(): String = this.encodeURLQueryComponent()

private fun parseChatEvent(payload: String): ChatEvent? = runCatching {
    val root = chatJson.parseToJsonElement(payload) as? JsonObject ?: return null
    when (root["type"]?.jsonPrimitive?.contentOrNull) {
        "token" -> ChatEvent.Token(root["text"]?.jsonPrimitive?.contentOrNull.orEmpty())
        "done" -> ChatEvent.Done
        "citations" -> ChatEvent.Citations(root["data"])
        else -> null
    }
}.getOrNull()

private val JsonElement.jsonPrimitive: JsonPrimitive
    get() = this as? JsonPrimitive ?: JsonPrimitive("")

private val JsonPrimitive.contentOrNull: String?
    get() = runCatching { content }.getOrNull()

private val chatJson = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
    explicitNulls = false
}
