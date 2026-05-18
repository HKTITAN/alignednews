package ai.aligned.net.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

// ─── Health / settings ──────────────────────────────────────────────────────

@Serializable data class HealthDto(
    val status: String, val version: String, val storyCount: Int,
    val lastUpdated: String, val services: Services
) { @Serializable data class Services(val twitter: Boolean, val openai: Boolean, val tavily: Boolean, val cerebras: Boolean) }

@Serializable data class SettingsResponseDto(
    val settings: SettingsDto,
    val availableModels: List<ModelOption> = emptyList(),
    val availableLanguages: List<LanguageOption> = emptyList()
)
@Serializable data class SettingsDto(
    val synthesisModel: String, val chatModel: String,
    val language: String, val customXApiKey: String, val algorithm: String
)
@Serializable data class ModelOption(val id: String, val label: String)
@Serializable data class LanguageOption(val id: String, val label: String, val dir: String)

// ─── Categories ─────────────────────────────────────────────────────────────

@Serializable data class CategoriesDto(
    val categories: List<CategoryDto> = emptyList(),
    val custom: List<CategoryDto>     = emptyList(),
    val all: List<CategoryDto>        = emptyList()
)
@Serializable data class CategoryDto(
    val id: String, val label: String, val color: String,
    val bgColor: String? = null, val section: String? = null
)

// ─── News / stories / tweets ────────────────────────────────────────────────

@Serializable data class NewsDto(val stories: List<StoryDto> = emptyList())

@Serializable data class StoryDto(
    val id: String, val headline: String, val summary: String,
    val category: String,
    val tweets: List<TweetDto> = emptyList(),
    val citations: List<JsonElement> = emptyList(),
    val createdAt: String, val updatedAt: String,
    val tweetCount: Int = 0, val totalEngagement: Long = 0,
    val signals: Signals? = null
) { @Serializable data class Signals(
        val topScore: Int = 0,
        val categories: List<String> = emptyList(),
        val flags: JsonObject = JsonObject(emptyMap())
   )
}

@Serializable data class TweetDto(
    val id: String, val text: String,
    val authorName: String, val authorUsername: String,
    val authorProfileImage: String, val authorFollowers: Long = 0,
    val authorLocation: String? = null,
    val createdAt: String,
    val likes: Long = 0, val retweets: Long = 0, val replies: Long = 0,
    val bookmarks: Long = 0, val views: Long = 0, val quotes: Long = 0,
    val url: String,
    val media: List<TweetMedia> = emptyList()
)
@Serializable data class TweetMedia(
    val type: String, val url: String,
    val width: Int = 0, val height: Int = 0
)

// ─── Search ─────────────────────────────────────────────────────────────────

@Serializable data class SearchDto(
    val stories: List<StoryDto> = emptyList(),
    val query: String = ""
)

// ─── Lists (editorial roll-up) ──────────────────────────────────────────────

@Serializable data class ListsDto(
    val overview: ListsOverview? = null,
    val groups: List<GroupDto> = emptyList(),
    val historyDates: List<String> = emptyList()
)
@Serializable data class ListsOverview(
    val timestamp: String, val date: String, val execSummary: String,
    val execBriefing: ExecBriefing? = null,
    val groupSummaries: List<GroupSummary> = emptyList(),
    val topStories: List<TopStoryRef> = emptyList()
)
@Serializable data class ExecBriefing(
    val leadHeadline: String, val leadGroupId: String, val leadBody: String,
    val sections: List<BriefingSection> = emptyList()
)
@Serializable data class BriefingSection(val subhead: String, val groupId: String, val body: String)
@Serializable data class GroupSummary(
    val groupId: String, val groupName: String, val color: String,
    val summary: String, val storyCount: Int
)
@Serializable data class TopStoryRef(
    val headline: String, val groupId: String,
    val sourceHandle: String, val engagement: Long, val tweetUrl: String
)
@Serializable data class GroupDto(
    val groupId: String, val groupName: String, val timestamp: String,
    val execSummary: String, val storyCount: Int,
    val subcategories: List<SubcategoryDto> = emptyList(),
    val topStories: List<GroupStoryDto>     = emptyList()
)
@Serializable data class SubcategoryDto(
    val id: String, val name: String, val summary: String,
    val storyCount: Int, val stories: List<GroupStoryDto> = emptyList()
)
@Serializable data class GroupStoryDto(
    val id: String, val headline: String, val summary: String,
    val analysis: String, val sourceHandle: String,
    val tweets: List<TweetDto> = emptyList(),
    val engagement: Long = 0, val createdAt: String
)

// ─── Map / Events / Accounts / History ──────────────────────────────────────

@Serializable data class MapDto(val markers: List<MapMarker> = emptyList())
@Serializable data class MapMarker(
    val lat: Double, val lng: Double, val city: String, val country: String,
    val groupId: String, val groupName: String, val groupColor: String,
    val stories: List<MapStoryRef> = emptyList()
)
@Serializable data class MapStoryRef(val headline: String, val sourceHandle: String, val tweetUrl: String)

@Serializable data class EventsDto(val events: List<EventDto> = emptyList())
@Serializable data class EventDto(
    val id: String, val name: String, val date: String, val location: String,
    val category: String, val description: String, val source: EventSource? = null
)
@Serializable data class EventSource(val headline: String, val authorUsername: String, val tweetUrl: String)

@Serializable data class AccountsDto(val accounts: List<AccountDto> = emptyList())
@Serializable data class AccountDto(
    val id: String, val username: String, val name: String,
    val profileImage: String, val followers: Long,
    val description: String, val addedAt: String
)

@Serializable data class HistoryEntryDto(
    val id: String, val type: String, val query: String,
    val confidence: Int = 0, val insightCount: Int = 0, val completedAt: String
)

// ─── Feedback ───────────────────────────────────────────────────────────────

@Serializable data class FeedbackRequest(val storyId: String, val category: String, val vote: String)
@Serializable data class FeedbackPostDto(val feedback: FeedbackEcho)
@Serializable data class FeedbackEcho(
    val storyId: String, val headline: String, val category: String,
    val sentiment: String, val timestamp: String
)
@Serializable data class FeedbackStatsDto(
    val total: Int, val thumbsUp: Int, val thumbsDown: Int,
    val byCategory: Map<String, FeedbackCount> = emptyMap()
)
@Serializable data class FeedbackCount(val up: Int = 0, val down: Int = 0)

// ─── Summarize ──────────────────────────────────────────────────────────────

@Serializable data class TweetInput(val text: String, val authorUsername: String)
@Serializable data class SummarizeRequest(val tweets: List<TweetInput>)
@Serializable data class SummarizeDto(val overview: String, val individual: List<String> = emptyList())

// ─── Chat (SSE) ─────────────────────────────────────────────────────────────

@Serializable data class ChatRequest(
    val message: String,
    val storyId: String? = null,
    val history: List<ChatTurn> = emptyList()
)
@Serializable data class ChatTurn(val role: String, val content: String)

@Serializable sealed class ChatEvent {
    @Serializable @SerialName("token")
    data class Token(val text: String) : ChatEvent()
    @Serializable @SerialName("done")
    object Done : ChatEvent()
}

// ─── Research ───────────────────────────────────────────────────────────────

@Serializable data class ResearchStartRequest(val query: String)
@Serializable data class ResearchStartDto(val id: String, val status: String)

@Serializable data class ResearchDto(
    val id: String, val query: String, val status: String,
    val steps: List<ResearchStep> = emptyList(),
    val currentStep: Int = 0,
    val insights: List<Insight> = emptyList(),
    val sourceNetwork: List<JsonElement> = emptyList(),
    val activityLog: List<String> = emptyList(),
    val tweets: List<TweetDto> = emptyList(),
    val startedAt: String? = null,
    val completedAt: String? = null,
    val summaryAnswer: String? = null
)
@Serializable data class ResearchStep(val name: String, val status: String, val detail: String? = null)
@Serializable data class Insight(
    val title: String, val summary: String,
    val confidence: Int = 0, val sources: List<String> = emptyList()
)
