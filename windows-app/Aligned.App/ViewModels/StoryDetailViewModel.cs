using System.Collections.ObjectModel;
using CommunityToolkit.Mvvm.ComponentModel;
using CommunityToolkit.Mvvm.Input;
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

    public async Task LoadAsync(string id)
    {
        StoryId = id;
        IsLoading = true;
        ErrorMessage = null;
        try
        {
            Story = await _api.Story(id);
            Tweets.Clear();
            if (Story?.Tweets != null) foreach (var t in Story.Tweets) Tweets.Add(t);
        }
        catch (Exception ex) { ErrorMessage = ex.Message; }
        finally { IsLoading = false; }
    }

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
}
