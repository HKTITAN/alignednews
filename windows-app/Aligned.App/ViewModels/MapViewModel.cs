using System.Collections.ObjectModel;
using CommunityToolkit.Mvvm.ComponentModel;
using CommunityToolkit.Mvvm.Input;
using Microsoft.UI.Xaml;
using Windows.UI;
using Aligned.App.Theming;
using Aligned.Core.Net;

namespace Aligned.App.ViewModels;

public class MapMarkerVm
{
    public string Where { get; set; } = "";
    public string GroupName { get; set; } = "";
    public string GroupId { get; set; } = "";
    public Color DotColor { get; set; }
    public List<string> Headlines { get; set; } = new();
}

public partial class MapViewModel : ObservableObject
{
    private readonly AlignedApi _api = new();

    [ObservableProperty] private bool _isLoading;
    [ObservableProperty] private string? _errorMessage;

    public ObservableCollection<MapMarkerVm> Markers { get; } = new();

    public string Subtitle =>
        IsLoading ? "Loading…"
        : Markers.Count > 0 ? $"{Markers.Count} locations today"
        : "Where today's news is happening";

    public Visibility LoadingVisibility => IsLoading && Markers.Count == 0 ? Visibility.Visible : Visibility.Collapsed;
    public Visibility ErrorVisibility => !IsLoading && ErrorMessage != null && Markers.Count == 0 ? Visibility.Visible : Visibility.Collapsed;
    public Visibility ContentVisibility => Markers.Count > 0 ? Visibility.Visible : Visibility.Collapsed;

    public MapViewModel() => _ = LoadAsync();

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
            var dto = await WarmCache.GetOrFetch("map", WarmCache.WarmTtl, () => _api.Map());
            Markers.Clear();
            foreach (var m in dto?.Markers ?? new())
            {
                Markers.Add(new MapMarkerVm
                {
                    Where = string.IsNullOrEmpty(m.Country) ? m.City : $"{m.City}, {m.Country}",
                    GroupName = m.GroupName.ToUpperInvariant(),
                    GroupId = m.GroupId,
                    DotColor = ParseHex(m.GroupColor) ?? CategoryPalette.ColorFor(m.GroupId),
                    Headlines = m.Stories.Take(2).Select(s => "• " + s.Headline).ToList()
                });
            }
        }
        catch (Exception ex) { ErrorMessage = ex.Message; }
        finally { IsLoading = false; Notify(); }
    }

    private static Color? ParseHex(string hex)
    {
        if (string.IsNullOrEmpty(hex)) return null;
        try { return CategoryPalette.FromHex(hex); }
        catch { return null; }
    }
}
