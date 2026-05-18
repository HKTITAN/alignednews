using System.Collections.ObjectModel;
using System.Text.Json;
using CommunityToolkit.Mvvm.ComponentModel;
using Microsoft.UI.Xaml;
using Aligned.App.Theming;
using Aligned.Core.Net;
using Aligned.Core.Net.Dto;

namespace Aligned.App.ViewModels;

public partial class BookmarksViewModel : ObservableObject
{
    private readonly AlignedApi _api = new();
    private static readonly JsonSerializerOptions Json = new(JsonSerializerDefaults.Web);

    public ObservableCollection<StoryDto> Items { get; } = new();

    public string Subtitle => Items.Count == 0 ? "No bookmarks yet" : $"{Items.Count} saved";
    public Visibility EmptyVisibility => Items.Count == 0 ? Visibility.Visible : Visibility.Collapsed;
    public Visibility ListVisibility => Items.Count > 0 ? Visibility.Visible : Visibility.Collapsed;

    public async Task LoadAsync()
    {
        Items.Clear();
        var ids = BookmarkStore.Load();
        // First resolve from the per-story cache, then network as fallback.
        foreach (var id in ids)
        {
            var cached = await WarmCache.ReadStale<StoryDto>($"story_{id}");
            if (cached != null) { Items.Add(cached); continue; }
            try
            {
                var fresh = await _api.Story(id);
                if (fresh != null) { Items.Add(fresh); await WarmCache.Write($"story_{id}", fresh); }
            }
            catch { /* skip missing */ }
        }
        OnPropertyChanged(nameof(Subtitle));
        OnPropertyChanged(nameof(EmptyVisibility));
        OnPropertyChanged(nameof(ListVisibility));
    }
}
