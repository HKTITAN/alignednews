using System.Collections.ObjectModel;
using CommunityToolkit.Mvvm.ComponentModel;
using Microsoft.UI.Dispatching;
using Microsoft.UI.Xaml;
using Aligned.Core.Net;
using Aligned.Core.Net.Dto;

namespace Aligned.App.ViewModels;

public partial class SearchViewModel : ObservableObject
{
    private readonly AlignedApi _api = new();
    private readonly DispatcherQueue _ui = DispatcherQueue.GetForCurrentThread();
    private CancellationTokenSource? _cts;
    private DateTime _lastEdit = DateTime.MinValue;

    [ObservableProperty] private string _query = "";
    [ObservableProperty] private bool _isSearching;
    [ObservableProperty] private string? _errorMessage;
    [ObservableProperty] private string _lastIssued = "";

    public ObservableCollection<StoryDto> Results { get; } = new();

    public Visibility IdleVisibility =>
        string.IsNullOrWhiteSpace(Query) ? Visibility.Visible : Visibility.Collapsed;
    public Visibility SearchingVisibility =>
        IsSearching && Results.Count == 0 ? Visibility.Visible : Visibility.Collapsed;
    public Visibility NoResultsVisibility =>
        !IsSearching && !string.IsNullOrWhiteSpace(Query) && Results.Count == 0 ? Visibility.Visible : Visibility.Collapsed;
    public Visibility ResultsVisibility =>
        Results.Count > 0 ? Visibility.Visible : Visibility.Collapsed;
    public string NoResultsText => $"No matches for \"{LastIssued}\".";

    partial void OnQueryChanged(string value)
    {
        Notify();
        _ = DebouncedSearch(value);
    }
    partial void OnIsSearchingChanged(bool value) => Notify();

    private void Notify()
    {
        OnPropertyChanged(nameof(IdleVisibility));
        OnPropertyChanged(nameof(SearchingVisibility));
        OnPropertyChanged(nameof(NoResultsVisibility));
        OnPropertyChanged(nameof(ResultsVisibility));
        OnPropertyChanged(nameof(NoResultsText));
    }

    private async Task DebouncedSearch(string q)
    {
        _cts?.Cancel();
        _cts = new CancellationTokenSource();
        var ct = _cts.Token;
        var stamp = DateTime.UtcNow;
        _lastEdit = stamp;
        try { await Task.Delay(280, ct); } catch { return; }
        if (_lastEdit != stamp) return;
        if (string.IsNullOrWhiteSpace(q))
        {
            Results.Clear();
            Notify();
            return;
        }
        IsSearching = true;
        Results.Clear();
        Notify();
        try
        {
            var dto = await _api.Search(q);
            if (dto?.Stories != null)
                foreach (var s in dto.Stories) Results.Add(s);
            LastIssued = q;
        }
        catch (OperationCanceledException) { }
        catch (Exception ex) { ErrorMessage = ex.Message; }
        finally { IsSearching = false; Notify(); }
    }
}
