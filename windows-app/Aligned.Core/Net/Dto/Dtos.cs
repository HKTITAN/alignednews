using System.Text.Json.Serialization;

namespace Aligned.Core.Net.Dto;

// XAML binding (x:DataType) requires regular get/set properties, not init-only.
// All DTOs are mutable classes with default values so they round-trip JSON ⇄ XAML.

// ─── Health / settings ──────────────────────────────────────────────────────

public class HealthDto
{
    public string Status { get; set; } = "";
    public string Version { get; set; } = "";
    public int StoryCount { get; set; }
    public string LastUpdated { get; set; } = "";
    public ServicesDto Services { get; set; } = new();
    public class ServicesDto
    {
        public bool Twitter { get; set; }
        public bool OpenAi { get; set; }
        public bool Tavily { get; set; }
        public bool Cerebras { get; set; }
    }
}

public class SettingsResponseDto
{
    public SettingsDto Settings { get; set; } = new();
    public List<ModelOption> AvailableModels { get; set; } = new();
    public List<LanguageOption> AvailableLanguages { get; set; } = new();
}
public class SettingsDto
{
    public string SynthesisModel { get; set; } = "";
    public string ChatModel { get; set; } = "";
    public string Language { get; set; } = "en";
    public string CustomXApiKey { get; set; } = "";
    public string Algorithm { get; set; } = "";
}
public class ModelOption    { public string Id { get; set; } = ""; public string Label { get; set; } = ""; }
public class LanguageOption { public string Id { get; set; } = ""; public string Label { get; set; } = ""; public string Dir { get; set; } = "ltr"; }

// ─── Categories ─────────────────────────────────────────────────────────────

public class CategoriesDto
{
    public List<CategoryDto> Categories { get; set; } = new();
    public List<CategoryDto> Custom { get; set; } = new();
    public List<CategoryDto> All { get; set; } = new();
}
public class CategoryDto
{
    public string Id { get; set; } = "";
    public string Label { get; set; } = "";
    public string Color { get; set; } = "#999999";
    public string? BgColor { get; set; }
    public string? Section { get; set; }
}

// ─── News / story / tweet ───────────────────────────────────────────────────

public class NewsDto { public List<StoryDto> Stories { get; set; } = new(); }

public class StoryDto
{
    public string Id { get; set; } = "";
    public string Headline { get; set; } = "";
    public string Summary { get; set; } = "";
    public string Category { get; set; } = "";
    public List<TweetDto> Tweets { get; set; } = new();
    public string CreatedAt { get; set; } = "";
    public string UpdatedAt { get; set; } = "";
    public int TweetCount { get; set; }
    public long TotalEngagement { get; set; }
}

public class TweetDto
{
    public string Id { get; set; } = "";
    public string Text { get; set; } = "";
    [JsonPropertyName("authorName")]         public string AuthorName { get; set; } = "";
    [JsonPropertyName("authorUsername")]     public string AuthorUsername { get; set; } = "";
    [JsonPropertyName("authorProfileImage")] public string AuthorProfileImage { get; set; } = "";
    [JsonPropertyName("authorFollowers")]    public long AuthorFollowers { get; set; }
    public string CreatedAt { get; set; } = "";
    public long Likes { get; set; }
    public long Retweets { get; set; }
    public long Replies { get; set; }
    public long Bookmarks { get; set; }
    public long Views { get; set; }
    public long Quotes { get; set; }
    public string Url { get; set; } = "";
    public List<TweetMedia> Media { get; set; } = new();
}

public class TweetMedia
{
    public string Type { get; set; } = "";
    public string Url { get; set; } = "";
    public int Width { get; set; }
    public int Height { get; set; }
}

// ─── Search ─────────────────────────────────────────────────────────────────

public class SearchDto
{
    public List<StoryDto> Stories { get; set; } = new();
    public string Query { get; set; } = "";
}

// ─── Lists ──────────────────────────────────────────────────────────────────

public class ListsDto
{
    public ListsOverview? Overview { get; set; }
    public List<GroupDto> Groups { get; set; } = new();
    public List<string> HistoryDates { get; set; } = new();
}

public class ListsOverview
{
    public string Timestamp { get; set; } = "";
    public string Date { get; set; } = "";
    public string ExecSummary { get; set; } = "";
    public ExecBriefing? ExecBriefing { get; set; }
    public List<GroupSummary> GroupSummaries { get; set; } = new();
    public List<TopStoryRef> TopStories { get; set; } = new();
}
public class ExecBriefing
{
    public string LeadHeadline { get; set; } = "";
    public string LeadGroupId { get; set; } = "";
    public string LeadBody { get; set; } = "";
    public List<BriefingSection> Sections { get; set; } = new();
}
public class BriefingSection { public string Subhead { get; set; } = ""; public string GroupId { get; set; } = ""; public string Body { get; set; } = ""; }
public class GroupSummary
{
    public string GroupId { get; set; } = "";
    public string GroupName { get; set; } = "";
    public string Color { get; set; } = "";
    public string Summary { get; set; } = "";
    public int StoryCount { get; set; }
}
public class TopStoryRef
{
    public string Headline { get; set; } = "";
    public string GroupId { get; set; } = "";
    public string SourceHandle { get; set; } = "";
    public long Engagement { get; set; }
    public string TweetUrl { get; set; } = "";
}
public class GroupDto
{
    public string GroupId { get; set; } = "";
    public string GroupName { get; set; } = "";
    public string Timestamp { get; set; } = "";
    public string ExecSummary { get; set; } = "";
    public int StoryCount { get; set; }
    public List<SubcategoryDto> Subcategories { get; set; } = new();
    public List<GroupStoryDto> TopStories { get; set; } = new();
}
public class SubcategoryDto
{
    public string Id { get; set; } = "";
    public string Name { get; set; } = "";
    public string Summary { get; set; } = "";
    public int StoryCount { get; set; }
    public List<GroupStoryDto> Stories { get; set; } = new();
}
public class GroupStoryDto
{
    public string Id { get; set; } = "";
    public string Headline { get; set; } = "";
    public string Summary { get; set; } = "";
    public string Analysis { get; set; } = "";
    public string SourceHandle { get; set; } = "";
    public List<TweetDto> Tweets { get; set; } = new();
    public long Engagement { get; set; }
    public string CreatedAt { get; set; } = "";
}

// ─── Map / Events / Accounts / History ──────────────────────────────────────

public class MapDto { public List<MapMarker> Markers { get; set; } = new(); }
public class MapMarker
{
    public double Lat { get; set; }
    public double Lng { get; set; }
    public string City { get; set; } = "";
    public string Country { get; set; } = "";
    public string GroupId { get; set; } = "";
    public string GroupName { get; set; } = "";
    public string GroupColor { get; set; } = "";
    public List<MapStoryRef> Stories { get; set; } = new();
}
public class MapStoryRef { public string Headline { get; set; } = ""; public string SourceHandle { get; set; } = ""; public string TweetUrl { get; set; } = ""; }

public class EventsDto { public List<EventDto> Events { get; set; } = new(); }
public class EventDto
{
    public string Id { get; set; } = "";
    public string Name { get; set; } = "";
    public string Date { get; set; } = "";
    public string Location { get; set; } = "";
    public string Category { get; set; } = "";
    public string Description { get; set; } = "";
    public EventSource? Source { get; set; }
}
public class EventSource { public string Headline { get; set; } = ""; public string AuthorUsername { get; set; } = ""; public string TweetUrl { get; set; } = ""; }

public class AccountsDto { public List<AccountDto> Accounts { get; set; } = new(); }
public class AccountDto
{
    public string Id { get; set; } = "";
    public string Username { get; set; } = "";
    public string Name { get; set; } = "";
    public string ProfileImage { get; set; } = "";
    public long Followers { get; set; }
    public string Description { get; set; } = "";
    public string AddedAt { get; set; } = "";
}

public class HistoryEntryDto
{
    public string Id { get; set; } = "";
    public string Type { get; set; } = "";
    public string Query { get; set; } = "";
    public int Confidence { get; set; }
    public int InsightCount { get; set; }
    public string CompletedAt { get; set; } = "";
}

// ─── Feedback ───────────────────────────────────────────────────────────────

public class FeedbackRequest
{
    public string StoryId { get; set; } = "";
    public string Category { get; set; } = "";
    public string Vote { get; set; } = "up";
    public FeedbackRequest() { }
    public FeedbackRequest(string storyId, string category, string vote)
    { StoryId = storyId; Category = category; Vote = vote; }
}
public class FeedbackPostDto { public FeedbackEcho Feedback { get; set; } = new(); }
public class FeedbackEcho
{
    public string StoryId { get; set; } = "";
    public string Headline { get; set; } = "";
    public string Category { get; set; } = "";
    public string Sentiment { get; set; } = "";
    public string Timestamp { get; set; } = "";
}
public class FeedbackStatsDto
{
    public int Total { get; set; }
    public int ThumbsUp { get; set; }
    public int ThumbsDown { get; set; }
    public Dictionary<string, FeedbackCount> ByCategory { get; set; } = new();
}
public class FeedbackCount
{
    public int Up { get; set; }
    public int Down { get; set; }
}

// ─── Summarize ──────────────────────────────────────────────────────────────

public class TweetInput
{
    public string Text { get; set; } = "";
    public string AuthorUsername { get; set; } = "";
    public TweetInput() { }
    public TweetInput(string text, string authorUsername) { Text = text; AuthorUsername = authorUsername; }
}
public class SummarizeRequest
{
    public List<TweetInput> Tweets { get; set; } = new();
    public SummarizeRequest() { }
    public SummarizeRequest(List<TweetInput> tweets) { Tweets = tweets; }
}
public class SummarizeDto { public string Overview { get; set; } = ""; public List<string> Individual { get; set; } = new(); }

// ─── Chat (SSE) ─────────────────────────────────────────────────────────────

public class ChatRequest
{
    public string Message { get; set; } = "";
    public string? StoryId { get; set; }
    public List<ChatTurn> History { get; set; } = new();
    public ChatRequest() { }
    public ChatRequest(string message, string? storyId, List<ChatTurn>? history)
    { Message = message; StoryId = storyId; History = history ?? new(); }
}
public class ChatTurn
{
    public string Role { get; set; } = "user";
    public string Content { get; set; } = "";
    public ChatTurn() { }
    public ChatTurn(string role, string content) { Role = role; Content = content; }
}

public abstract record ChatEvent
{
    public sealed record Token(string Text) : ChatEvent;
    public sealed record Done() : ChatEvent;
}

// ─── Research ───────────────────────────────────────────────────────────────

public class ResearchStartRequest
{
    public string Query { get; set; } = "";
    public ResearchStartRequest() { }
    public ResearchStartRequest(string query) { Query = query; }
}
public class ResearchStartDto
{
    public string Id { get; set; } = "";
    public string Status { get; set; } = "";
}

public class ResearchDto
{
    public string Id { get; set; } = "";
    public string Query { get; set; } = "";
    public string Status { get; set; } = "";
    public List<ResearchStep> Steps { get; set; } = new();
    public int CurrentStep { get; set; }
    public List<Insight> Insights { get; set; } = new();
    public List<string> ActivityLog { get; set; } = new();
    public List<TweetDto> Tweets { get; set; } = new();
    public string? StartedAt { get; set; }
    public string? CompletedAt { get; set; }
    public string? SummaryAnswer { get; set; }
}
public class ResearchStep
{
    public string Name { get; set; } = "";
    public string Status { get; set; } = "";
    public string? Detail { get; set; }
}
public class Insight
{
    public string Title { get; set; } = "";
    public string Summary { get; set; } = "";
    public int Confidence { get; set; }
    public List<string> Sources { get; set; } = new();
}

public enum Vote { Up, Down }
public static class VoteExtensions { public static string Wire(this Vote v) => v == Vote.Up ? "up" : "down"; }
