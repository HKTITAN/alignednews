using System.Collections.ObjectModel;
using CommunityToolkit.Mvvm.ComponentModel;
using CommunityToolkit.Mvvm.Input;
using Microsoft.UI.Xaml;
using Aligned.App.Theming;
using Aligned.Core.Net;
using Aligned.Core.Net.Dto;

namespace Aligned.App.ViewModels;

/// <summary>
/// Paste an X/Twitter URL → resolve the related-tweet cluster → POST /api/summarize.
/// </summary>
public partial class SummarizeViewModel : ObservableObject
{
    private readonly AlignedApi _api = new();

    [ObservableProperty] private string _input = "";
    [ObservableProperty] private bool _isLoading;
    [ObservableProperty] private string? _errorMessage;
    [ObservableProperty] private string _source = "";
    [ObservableProperty] private string _overview = "";
    [ObservableProperty] private StoryDto? _resolvedStory;

    public ObservableCollection<string> Bullets { get; } = new();
    public ObservableCollection<TweetDto> Tweets { get; } = new();

    public string Subtitle =>
        IsLoading ? "Resolving thread…"
        : !string.IsNullOrEmpty(Overview) ? $"{Tweets.Count} tweets summarized"
        : "Paste any X / Twitter URL";

    public Visibility HintVisibility =>
        !IsLoading && string.IsNullOrEmpty(Overview) && ErrorMessage == null
            ? Visibility.Visible : Visibility.Collapsed;
    public Visibility ResultVisibility =>
        !string.IsNullOrEmpty(Overview) ? Visibility.Visible : Visibility.Collapsed;
    public Visibility ErrorVisibility =>
        !string.IsNullOrEmpty(ErrorMessage) ? Visibility.Visible : Visibility.Collapsed;
    public Visibility ClearVisibility =>
        !string.IsNullOrEmpty(Overview) || !string.IsNullOrEmpty(ErrorMessage)
            ? Visibility.Visible : Visibility.Collapsed;
    public Visibility ResolvedStoryVisibility =>
        ResolvedStory != null ? Visibility.Visible : Visibility.Collapsed;

    partial void OnIsLoadingChanged(bool value) => Notify();
    partial void OnOverviewChanged(string value) => Notify();
    partial void OnErrorMessageChanged(string? value) => Notify();
    partial void OnResolvedStoryChanged(StoryDto? value) => OnPropertyChanged(nameof(ResolvedStoryVisibility));
    partial void OnInputChanged(string value) => SubmitCommand.NotifyCanExecuteChanged();

    private void Notify()
    {
        OnPropertyChanged(nameof(Subtitle));
        OnPropertyChanged(nameof(HintVisibility));
        OnPropertyChanged(nameof(ResultVisibility));
        OnPropertyChanged(nameof(ErrorVisibility));
        OnPropertyChanged(nameof(ClearVisibility));
    }

    public bool CanSubmit => !IsLoading && !string.IsNullOrWhiteSpace(Input);

    [RelayCommand(CanExecute = nameof(CanSubmit))]
    public async Task SubmitAsync()
    {
        if (!CanSubmit) return;
        var url = Input.Trim();
        var (handle, id) = TweetLink.Parse(url);

        IsLoading = true;
        ErrorMessage = null;
        Overview = "";
        Bullets.Clear();
        Tweets.Clear();
        ResolvedStory = null;

        try
        {
            var cluster = await ResolveClusterAsync(id, handle, url);
            if (cluster.Tweets.Count == 0)
                throw new InvalidOperationException("Couldn't find any tweets for this URL.");

            ResolvedStory = cluster.Story;
            Source = cluster.Source;
            foreach (var t in cluster.Tweets) Tweets.Add(t);

            var inputs = cluster.Tweets
                .Select(t => new TweetInput(t.Text, t.AuthorUsername))
                .ToList();
            var resp = await _api.Summarize(inputs);

            Overview = resp?.Overview ?? "";
            foreach (var b in resp?.Individual ?? new()) Bullets.Add(b);
        }
        catch (Exception ex) { ErrorMessage = ex.Message; }
        finally { IsLoading = false; }
    }

    [RelayCommand]
    public void Clear()
    {
        Input = "";
        Overview = "";
        ErrorMessage = null;
        Source = "";
        Bullets.Clear();
        Tweets.Clear();
        ResolvedStory = null;
        Notify();
    }

    private async Task<Cluster> ResolveClusterAsync(string? id, string? handle, string originalUrl)
    {
        // 1. Look at the cached news payload for any tweet matching the ID.
        var news = await WarmCache.ReadStale<NewsDto>("news");
        if (id != null && news?.Stories != null)
        {
            var token = $"/status/{id}";
            foreach (var s in news.Stories)
            {
                if (s.Tweets.Any(t => t.Url.Contains(token, StringComparison.OrdinalIgnoreCase) || t.Id == id))
                    return new Cluster("Found in cached feed cluster", s, s.Tweets);
            }
        }

        // 2. Search by author handle.
        if (!string.IsNullOrEmpty(handle))
        {
            try
            {
                var searched = await _api.Search(handle, limit: 20);
                if (searched?.Stories != null)
                {
                    if (id != null)
                    {
                        var token = $"/status/{id}";
                        foreach (var s in searched.Stories)
                        {
                            if (s.Tweets.Any(t => t.Url.Contains(token, StringComparison.OrdinalIgnoreCase) || t.Id == id))
                                return new Cluster("Resolved via /api/search", s, s.Tweets);
                        }
                    }
                    var first = searched.Stories.FirstOrDefault();
                    if (first != null)
                        return new Cluster("Closest match by author", first, first.Tweets);
                }
            }
            catch { /* fall through */ }
        }

        // 3. Direct fallback: ship a single placeholder so the model can still try.
        var placeholder = new TweetDto
        {
            Id = id ?? "0",
            Text = $"Tweet at {originalUrl}",
            AuthorName = handle ?? "",
            AuthorUsername = handle ?? "",
            Url = originalUrl
        };
        return new Cluster("Direct (tweet not in feed)", null, new List<TweetDto> { placeholder });
    }

    private record Cluster(string Source, StoryDto? Story, IReadOnlyList<TweetDto> Tweets);
}
