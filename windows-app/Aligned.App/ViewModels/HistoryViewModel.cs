using System.Collections.ObjectModel;
using System.Text.Json;
using CommunityToolkit.Mvvm.ComponentModel;
using CommunityToolkit.Mvvm.Input;
using Microsoft.UI.Xaml;
using Aligned.App.Controls;
using Aligned.App.Theming;
using Aligned.Core.Net;
using Aligned.Core.Net.Dto;

namespace Aligned.App.ViewModels;

public class HistoryEntryVm
{
    public string Id { get; set; } = "";
    public string Type { get; set; } = "";
    public string Query { get; set; } = "";
    public int Confidence { get; set; }
    public int InsightCount { get; set; }
    public string WhenLabel { get; set; } = "";
    public string TypeLabel => Type.ToUpperInvariant();
    public IconSpec GlyphSpec => Type == "research" ? MorphingIcons.Sparkle : MorphingIcons.Mic;
    public string ConfidenceLine =>
        (InsightCount > 0 ? $"{InsightCount} insights" : "")
        + (InsightCount > 0 && Confidence > 0 ? " · " : "")
        + (Confidence > 0 ? $"{Confidence}% confidence" : "");
}

public partial class HistoryViewModel : ObservableObject
{
    private readonly AlignedApi _api = new();

    [ObservableProperty] private bool _isLoading;
    [ObservableProperty] private string? _errorMessage;

    public ObservableCollection<HistoryEntryVm> Entries { get; } = new();

    public string Subtitle =>
        IsLoading ? "Loading…"
        : Entries.Count > 0 ? $"{Entries.Count} past sessions"
        : "Past research and chat";

    public Visibility LoadingVisibility => IsLoading && Entries.Count == 0 ? Visibility.Visible : Visibility.Collapsed;
    public Visibility ErrorVisibility => !IsLoading && ErrorMessage != null && Entries.Count == 0 ? Visibility.Visible : Visibility.Collapsed;
    public Visibility ContentVisibility => Entries.Count > 0 ? Visibility.Visible : Visibility.Collapsed;

    public HistoryViewModel() => _ = LoadAsync();

    partial void OnIsLoadingChanged(bool value) => Notify();
    private void Notify()
    {
        OnPropertyChanged(nameof(Subtitle));
        OnPropertyChanged(nameof(LoadingVisibility));
        OnPropertyChanged(nameof(ErrorVisibility));
        OnPropertyChanged(nameof(ContentVisibility));
    }

    [RelayCommand]
    public async Task LoadAsync()
    {
        IsLoading = true;
        ErrorMessage = null;
        try
        {
            var dto = await WarmCache.GetOrFetch<List<HistoryEntryDto>>(
                "history", WarmCache.ColdTtl, async () => await _api.History());
            Entries.Clear();
            foreach (var e in dto ?? new())
            {
                Entries.Add(new HistoryEntryVm
                {
                    Id = e.Id, Type = e.Type, Query = e.Query,
                    Confidence = e.Confidence, InsightCount = e.InsightCount,
                    WhenLabel = string.IsNullOrEmpty(e.CompletedAt) ? "" : "· " + e.CompletedAt
                });
            }
        }
        catch (Exception ex) { ErrorMessage = ex.Message; }
        finally { IsLoading = false; Notify(); }
    }
}
