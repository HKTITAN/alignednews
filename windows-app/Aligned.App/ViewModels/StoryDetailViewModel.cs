using System.Collections.ObjectModel;
using CommunityToolkit.Mvvm.ComponentModel;
using CommunityToolkit.Mvvm.Input;
using Microsoft.UI.Xaml;
using Microsoft.UI.Xaml.Media;
using Windows.UI;
using Aligned.App.Theming;
using Aligned.Core.Net;
using Aligned.Core.Net.Dto;

namespace Aligned.App.ViewModels;

public partial class StoryDetailViewModel : ObservableObject
{
    private readonly AlignedApi _api = new();

    [ObservableProperty] private string _storyId = "";
    [ObservableProperty] private StoryDto? _story;
    [ObservableProperty] private bool _isLoading;
    [ObservableProperty] private string? _errorMessage;
    [ObservableProperty] private string _lastVote = "";

    public ObservableCollection<TweetDto> Tweets { get; } = new();

    public Visibility IsLoadingVisibility =>
        IsLoading ? Visibility.Visible : Visibility.Collapsed;
    public Visibility ErrorVisibility =>
        !IsLoading && ErrorMessage != null ? Visibility.Visible : Visibility.Collapsed;
    public Visibility ReadyVisibility =>
        !IsLoading && Story != null ? Visibility.Visible : Visibility.Collapsed;

    public Color CategoryDot => CategoryPalette.ColorFor(Story?.Category ?? "");
    public string CategoryLabel => CategoryPalette.Pretty(Story?.Category ?? "").ToUpperInvariant();

    partial void OnStoryChanged(StoryDto? value)
    {
        OnPropertyChanged(nameof(CategoryDot));
        OnPropertyChanged(nameof(CategoryLabel));
        OnPropertyChanged(nameof(ReadyVisibility));
    }
    partial void OnIsLoadingChanged(bool value)
    {
        OnPropertyChanged(nameof(IsLoadingVisibility));
        OnPropertyChanged(nameof(ErrorVisibility));
        OnPropertyChanged(nameof(ReadyVisibility));
    }
    partial void OnErrorMessageChanged(string? value)
    {
        OnPropertyChanged(nameof(ErrorVisibility));
    }

    public async Task LoadAsync(string id)
    {
        StoryId = id;
        IsLoading = true;
        ErrorMessage = null;
        IsBookmarked = BookmarkStore.IsBookmarked(id);
        try
        {
            // Stories: 7-day cache, like Android.
            Story = await WarmCache.GetOrFetch($"story_{id}", WarmCache.StoryTtl, () => _api.Story(id));
            Tweets.Clear();
            if (Story?.Tweets != null) foreach (var t in Story.Tweets) Tweets.Add(t);
        }
        catch (Exception ex) { ErrorMessage = ex.Message; }
        finally { IsLoading = false; }
    }

    [ObservableProperty] private bool _isBookmarked;

    [RelayCommand]
    public void ToggleBookmark()
    {
        if (string.IsNullOrEmpty(StoryId)) return;
        IsBookmarked = BookmarkStore.Toggle(StoryId);
    }

    partial void OnIsBookmarkedChanged(bool value)
    {
        OnPropertyChanged(nameof(BookmarkLabel));
        OnPropertyChanged(nameof(BookmarkBrush));
    }
    public string BookmarkLabel => IsBookmarked ? "Saved" : "";
    public Brush BookmarkBrush =>
        IsBookmarked
            ? (Brush)Application.Current.Resources["AlignedAccent"]
            : (Brush)Application.Current.Resources["AlignedText"];

    [RelayCommand] private Task VoteUpAsync()   => SendVote(Vote.Up);
    [RelayCommand] private Task VoteDownAsync() => SendVote(Vote.Down);

    private async Task SendVote(Vote v)
    {
        if (Story is null) return;
        try
        {
            await _api.Feedback(Story.Id, Story.Category, v);
            LastVote = v == Vote.Up ? "Marked useful" : "Marked not useful";
        }
        catch (Exception ex) { ErrorMessage = ex.Message; }
    }

    public string InfographicUrl() =>
        string.IsNullOrEmpty(StoryId) ? "" : _api.InfographicUrl(StoryId);

    public static string FormatNum(long v) => v switch
    {
        < 1_000 => v.ToString(),
        < 1_000_000 => $"{v / 1_000}K",
        _ => $"{v / 100_000 / 10.0:0.0}M"
    };
}
