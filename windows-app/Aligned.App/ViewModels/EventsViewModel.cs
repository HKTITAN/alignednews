using System.Collections.ObjectModel;
using CommunityToolkit.Mvvm.ComponentModel;
using CommunityToolkit.Mvvm.Input;
using Microsoft.UI.Xaml;
using Windows.UI;
using Aligned.App.Theming;
using Aligned.Core.Net;

namespace Aligned.App.ViewModels;

public class EventVm
{
    public string Id { get; set; } = "";
    public string Name { get; set; } = "";
    public string DateLabel { get; set; } = "";
    public string LocationLabel { get; set; } = "";
    public string Description { get; set; } = "";
    public Color DotColor { get; set; }
}

public partial class EventsViewModel : ObservableObject
{
    private readonly AlignedApi _api = new();

    [ObservableProperty] private bool _isLoading;
    [ObservableProperty] private string? _errorMessage;

    public ObservableCollection<EventVm> Events { get; } = new();

    public string Subtitle =>
        IsLoading ? "Loading…"
        : Events.Count > 0 ? $"{Events.Count} upcoming · live"
        : "Conferences, launches, deadlines";

    public Visibility LoadingVisibility => IsLoading && Events.Count == 0 ? Visibility.Visible : Visibility.Collapsed;
    public Visibility ErrorVisibility => !IsLoading && ErrorMessage != null && Events.Count == 0 ? Visibility.Visible : Visibility.Collapsed;
    public Visibility ContentVisibility => Events.Count > 0 ? Visibility.Visible : Visibility.Collapsed;

    public EventsViewModel() => _ = LoadAsync();

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
            var dto = await WarmCache.GetOrFetch("events", WarmCache.WarmTtl, () => _api.Events());
            Events.Clear();
            foreach (var e in dto?.Events ?? new())
            {
                Events.Add(new EventVm
                {
                    Id = e.Id,
                    Name = e.Name,
                    DateLabel = e.Date,
                    LocationLabel = string.IsNullOrEmpty(e.Location) ? "" : "· " + e.Location,
                    Description = e.Description,
                    DotColor = CategoryPalette.ColorFor(e.Category)
                });
            }
        }
        catch (Exception ex) { ErrorMessage = ex.Message; }
        finally { IsLoading = false; Notify(); }
    }
}
