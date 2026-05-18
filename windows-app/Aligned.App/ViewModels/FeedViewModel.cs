using System.Collections.ObjectModel;
using CommunityToolkit.Mvvm.ComponentModel;
using CommunityToolkit.Mvvm.Input;
using Microsoft.UI.Xaml;
using Microsoft.UI.Xaml.Media;
using Windows.UI;
using Aligned.App.Theming;
using Aligned.Core.Net;
using Aligned.Core.Net.Dto;
// WarmCache lives in Aligned.App.Theming.

namespace Aligned.App.ViewModels;

public partial class CategoryChip : ObservableObject
{
    public string Id { get; }
    public string Label { get; }
    public Color DotColor { get; }
    public Visibility DotVisibility => Id == "__all__" ? Visibility.Collapsed : Visibility.Visible;

    [ObservableProperty] private bool _isSelected;

    public Brush BackgroundBrush =>
        IsSelected ? (Brush)Application.Current.Resources["AlignedText"]
                   : (Brush)Application.Current.Resources["AlignedElev1"];
    public Brush ForegroundBrush =>
        IsSelected ? (Brush)Application.Current.Resources["AlignedBg"]
                   : (Brush)Application.Current.Resources["AlignedText"];

    private readonly Action<string?> _onSelect;
    public IRelayCommand SelectCommand { get; }

    public CategoryChip(string id, string label, Color dot, Action<string?> onSelect)
    {
        Id = id; Label = label; DotColor = dot; _onSelect = onSelect;
        SelectCommand = new RelayCommand(() => _onSelect(Id == "__all__" ? null : Id));
    }

    partial void OnIsSelectedChanged(bool value)
    {
        OnPropertyChanged(nameof(BackgroundBrush));
        OnPropertyChanged(nameof(ForegroundBrush));
    }
}

public partial class FeedViewModel : ObservableObject
{
    private readonly AlignedApi _api = new();

    public ObservableCollection<StoryDto> Stories { get; } = new();
    public ObservableCollection<StoryDto> FilteredStories { get; } = new();
    public ObservableCollection<CategoryChip> CategoryChips { get; } = new();

    [ObservableProperty] private bool _isLoading;
    [ObservableProperty] private string? _errorMessage;
    [ObservableProperty] private string? _selectedCategory;

    public FeedViewModel() => _ = LoadAsync();

    public string StatusLine =>
        IsLoading ? "Refreshing…"
        : ErrorMessage != null ? "Offline · cached"
        : Stories.Count > 0 ? $"{Stories.Count} stories · live"
        : "Loading";

    public Color LiveDotColor =>
        IsLoading ? CategoryPalette.FromHex("#FF9500")
        : ErrorMessage != null ? CategoryPalette.FromHex("#FF3B30")
        : CategoryPalette.FromHex("#34C759");

    public Visibility IsLoadingVisibility =>
        IsLoading && Stories.Count == 0 ? Visibility.Visible : Visibility.Collapsed;
    public Visibility ErrorVisibility =>
        !IsLoading && ErrorMessage != null && Stories.Count == 0 ? Visibility.Visible : Visibility.Collapsed;
    public Visibility ListVisibility =>
        Stories.Count > 0 ? Visibility.Visible : Visibility.Collapsed;

    [RelayCommand]
    public async Task LoadAsync()
    {
        IsLoading = true;
        ErrorMessage = null;
        Notify();
        try
        {
            // Story list is hot tier — 30 min TTL; falls back to stale cache on net failure.
            var resp = await WarmCache.GetOrFetch("news", WarmCache.WarmTtl, () => _api.News());
            Stories.Clear();
            if (resp?.Stories != null)
                foreach (var s in resp.Stories) Stories.Add(s);
            RebuildChips();
            ApplyFilter();
        }
        catch (Exception ex) { ErrorMessage = ex.Message; }
        finally { IsLoading = false; Notify(); }
    }

    private void RebuildChips()
    {
        CategoryChips.Clear();
        CategoryChips.Add(new CategoryChip("__all__", "All", Microsoft.UI.Colors.Transparent, SelectCategory)
        { IsSelected = SelectedCategory == null });
        var distinct = Stories.Select(s => s.Category).Distinct().OrderBy(c => c).ToList();
        foreach (var cat in distinct)
        {
            CategoryChips.Add(new CategoryChip(
                cat, CategoryPalette.Pretty(cat),
                CategoryPalette.ColorFor(cat),
                SelectCategory
            ) { IsSelected = cat == SelectedCategory });
        }
    }

    private void SelectCategory(string? cat)
    {
        SelectedCategory = cat;
        foreach (var c in CategoryChips)
            c.IsSelected = (cat == null && c.Id == "__all__") || c.Id == cat;
        ApplyFilter();
    }

    private void ApplyFilter()
    {
        FilteredStories.Clear();
        IEnumerable<StoryDto> src = Stories;
        if (SelectedCategory != null) src = src.Where(s => s.Category == SelectedCategory);
        foreach (var s in src) FilteredStories.Add(s);
    }

    private void Notify()
    {
        OnPropertyChanged(nameof(StatusLine));
        OnPropertyChanged(nameof(LiveDotColor));
        OnPropertyChanged(nameof(IsLoadingVisibility));
        OnPropertyChanged(nameof(ErrorVisibility));
        OnPropertyChanged(nameof(ListVisibility));
    }

    // Static helpers for x:Bind in FeedPage.xaml
    public static Color CategoryColor(string id) => CategoryPalette.ColorFor(id);
    public static string PrettyCategory(string id) => CategoryPalette.Pretty(id).ToUpperInvariant();
    public static string MetaLine(int tweetCount, long engagement)
    {
        string e = engagement switch
        {
            < 1_000 => engagement.ToString(),
            < 1_000_000 => $"{engagement / 1_000}K",
            _ => $"{engagement / 100_000 / 10.0:0.0}M"
        };
        return $"{tweetCount} posts · {e}";
    }
    public static Visibility NonEmptyVisibility(string s) =>
        string.IsNullOrEmpty(s) ? Visibility.Collapsed : Visibility.Visible;
}
