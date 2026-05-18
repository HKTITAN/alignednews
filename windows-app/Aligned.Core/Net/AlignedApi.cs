using System.Net.Http.Headers;
using System.Net.Http.Json;
using System.Runtime.CompilerServices;
using System.Text;
using System.Text.Json;
using Aligned.Core.Net.Dto;

namespace Aligned.Core.Net;

/// <summary>
/// Complete client for https://alignednews.ai/api. Mirror of
/// shared-kmp/src/commonMain/kotlin/ai/aligned/net/AlignedApi.kt.
///
/// Hard rules (enforced here):
///   1. We never call POST /api/settings — backend is global + unauth'd.
///   2. We never call POST /api/refresh — operator-only.
/// </summary>
public sealed class AlignedApi : IDisposable
{
    public const string DefaultBaseUrl   = "https://alignednews.ai";
    public const string RailwayOrigin    = "https://x-news-stream-robert-production.up.railway.app";

    private readonly string _baseUrl;
    private readonly HttpClient _http;
    private static readonly JsonSerializerOptions Json = new(JsonSerializerDefaults.Web)
    {
        PropertyNameCaseInsensitive = true,
        DefaultIgnoreCondition = System.Text.Json.Serialization.JsonIgnoreCondition.WhenWritingNull
    };

    public AlignedApi(string? baseUrl = null, HttpClient? httpClient = null)
    {
        _baseUrl = baseUrl ?? DefaultBaseUrl;
        _http = httpClient ?? new HttpClient();
        _http.DefaultRequestHeaders.UserAgent.ParseAdd("ALIGNED/0.1 (+https://alignednews.ai)");
    }

    public Task<HealthDto?>      Health()     => _http.GetFromJsonAsync<HealthDto>($"{_baseUrl}/api/health", Json);
    public Task<CategoriesDto?>  Categories() => _http.GetFromJsonAsync<CategoriesDto>($"{_baseUrl}/api/categories", Json);
    public Task<AccountsDto?>    Accounts()   => _http.GetFromJsonAsync<AccountsDto>($"{_baseUrl}/api/accounts", Json);
    public Task<SettingsResponseDto?> Settings() => _http.GetFromJsonAsync<SettingsResponseDto>($"{_baseUrl}/api/settings", Json);

    public Task<NewsDto?> News(string? category = null, int? limit = null)
    {
        var qs = BuildQuery(("category", category), ("limit", limit?.ToString()));
        return _http.GetFromJsonAsync<NewsDto>($"{_baseUrl}/api/news{qs}", Json);
    }

    public Task<StoryDto?> Story(string id) =>
        _http.GetFromJsonAsync<StoryDto>($"{_baseUrl}/api/news/{Uri.EscapeDataString(id)}", Json);

    public Task<SearchDto?> Search(string query, int limit = 12)
    {
        var qs = BuildQuery(("q", query), ("limit", limit.ToString()));
        return _http.GetFromJsonAsync<SearchDto>($"{_baseUrl}/api/search{qs}", Json);
    }

    public Task<ListsDto?> Lists(string? group = null, string? date = null)
    {
        var qs = BuildQuery(("group", group), ("date", date));
        return _http.GetFromJsonAsync<ListsDto>($"{_baseUrl}/api/lists{qs}", Json);
    }

    public Task<MapDto?>             Map()        => _http.GetFromJsonAsync<MapDto>($"{_baseUrl}/api/map", Json);
    public Task<EventsDto?>          Events()     => _http.GetFromJsonAsync<EventsDto>($"{_baseUrl}/api/events", Json);
    public Task<List<HistoryEntryDto>?> History() => _http.GetFromJsonAsync<List<HistoryEntryDto>>($"{_baseUrl}/api/history", Json);
    public Task<FeedbackStatsDto?>   FeedbackStats() => _http.GetFromJsonAsync<FeedbackStatsDto>($"{_baseUrl}/api/feedback", Json);

    public async Task<FeedbackPostDto?> Feedback(string storyId, string category, Vote vote)
    {
        var resp = await _http.PostAsJsonAsync($"{_baseUrl}/api/feedback",
            new FeedbackRequest(storyId, category, vote.Wire()), Json);
        resp.EnsureSuccessStatusCode();
        return await resp.Content.ReadFromJsonAsync<FeedbackPostDto>(Json);
    }

    public async Task<SummarizeDto?> Summarize(IEnumerable<TweetInput> tweets)
    {
        var resp = await _http.PostAsJsonAsync($"{_baseUrl}/api/summarize",
            new SummarizeRequest(tweets.ToList()), Json);
        resp.EnsureSuccessStatusCode();
        return await resp.Content.ReadFromJsonAsync<SummarizeDto>(Json);
    }

    /// <summary>
    /// Streams chat tokens. Each yielded item is a <see cref="ChatEvent"/>.
    /// </summary>
    public async IAsyncEnumerable<ChatEvent> Chat(
        string message,
        string? storyId = null,
        List<ChatTurn>? history = null,
        [EnumeratorCancellation] CancellationToken ct = default)
    {
        var body = new ChatRequest(message, storyId, history);
        using var req = new HttpRequestMessage(HttpMethod.Post, $"{_baseUrl}/api/chat")
        {
            Content = new StringContent(JsonSerializer.Serialize(body, Json), Encoding.UTF8, "application/json")
        };
        using var resp = await _http.SendAsync(req, HttpCompletionOption.ResponseHeadersRead, ct);
        resp.EnsureSuccessStatusCode();
        await using var stream = await resp.Content.ReadAsStreamAsync(ct);
        using var reader = new StreamReader(stream);
        while (!ct.IsCancellationRequested)
        {
            var line = await reader.ReadLineAsync(ct);
            if (line is null) break;
            if (!line.StartsWith("data:")) continue;
            var payload = line[5..].Trim();
            if (payload.Length == 0) continue;
            ChatEvent? evt;
            try
            {
                using var doc = JsonDocument.Parse(payload);
                var type = doc.RootElement.GetProperty("type").GetString();
                evt = type switch
                {
                    "token" => new ChatEvent.Token(doc.RootElement.GetProperty("text").GetString() ?? ""),
                    "done"  => new ChatEvent.Done(),
                    _       => null
                };
            }
            catch (JsonException) { evt = null; }
            if (evt is null) continue;
            yield return evt;
            if (evt is ChatEvent.Done) yield break;
        }
    }

    public async Task<ResearchStartDto?> StartResearch(string query)
    {
        var resp = await _http.PostAsJsonAsync($"{_baseUrl}/api/research",
            new ResearchStartRequest(query), Json);
        resp.EnsureSuccessStatusCode();
        return await resp.Content.ReadFromJsonAsync<ResearchStartDto>(Json);
    }

    public Task<ResearchDto?> Research(string id) =>
        _http.GetFromJsonAsync<ResearchDto>($"{_baseUrl}/api/research?id={Uri.EscapeDataString(id)}", Json);

    public string OgUrl(string? title = null, string? subtitle = null, string? storyId = null)
        => $"{_baseUrl}/api/og{BuildQuery(("title", title), ("subtitle", subtitle), ("storyId", storyId))}";

    public string InfographicUrl(string storyId)
        => $"{_baseUrl}/api/infographic?id={Uri.EscapeDataString(storyId)}";

    private static string BuildQuery(params (string Key, string? Value)[] parts)
    {
        var pairs = parts.Where(p => !string.IsNullOrEmpty(p.Value))
                         .Select(p => $"{p.Key}={Uri.EscapeDataString(p.Value!)}");
        return pairs.Any() ? "?" + string.Join("&", pairs) : "";
    }

    public void Dispose() => _http.Dispose();
}
