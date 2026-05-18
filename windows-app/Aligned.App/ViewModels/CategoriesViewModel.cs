using System.Collections.ObjectModel;
using CommunityToolkit.Mvvm.ComponentModel;
using CommunityToolkit.Mvvm.Input;
using Microsoft.UI;
using Microsoft.UI.Xaml;
using Microsoft.UI.Xaml.Media;
using Windows.UI;
using Aligned.App.Theming;
using Aligned.Core.Net;
using Aligned.Core.Net.Dto;

namespace Aligned.App.ViewModels;

public partial class CategoryRowVm : ObservableObject
{
    public string Id { get; set; } = "";
    public string Label { get; set; } = "";
    public Color DotColor { get; set; }
    [ObservableProperty] private bool _isPinned;
    public IRelayCommand TogglePinCommand { get; }
    public Brush PinBrush =>
        IsPinned
            ? (Brush)Application.Current.Resources["AlignedAccent"]
            : (Brush)Application.Current.Resources["AlignedTextTertiary"];
    partial void OnIsPinnedChanged(bool value) => OnPropertyChanged(nameof(PinBrush));
    private readonly Action<CategoryRowVm> _onToggle;
    public CategoryRowVm(Action<CategoryRowVm> onToggle)
    {
        _onToggle = onToggle;
        TogglePinCommand = new RelayCommand(() => _onToggle(this));
    }
}

public partial class CategoriesViewModel : ObservableObject
{
    private readonly AlignedApi _api = new();

    public ObservableCollection<CategoryRowVm> Items { get; } = new();

    [ObservableProperty] private bool _isLoading;
    [ObservableProperty] private string? _errorMessage;

    public Visibility LoadingVisibility => IsLoading && Items.Count == 0 ? Visibility.Visible : Visibility.Collapsed;
    public Visibility ErrorVisibility => !IsLoading && ErrorMessage != null && Items.Count == 0 ? Visibility.Visible : Visibility.Collapsed;
    public Visibility ContentVisibility => Items.Count > 0 ? Visibility.Visible : Visibility.Collapsed;
    public string Subtitle =>
        Items.Count == 0 ? "Loading…"
        : $"{Items.Count(i => i.IsPinned)} pinned · stories in these get pushed to you";

    public CategoriesViewModel() => _ = LoadAsync();

    partial void OnIsLoadingChanged(bool value) => Notify();
    private void Notify()
    {
        OnPropertyChanged(nameof(LoadingVisibility));
        OnPropertyChanged(nameof(ErrorVisibility));
        OnPropertyChanged(nameof(ContentVisibility));
        OnPropertyChanged(nameof(Subtitle));
    }

    [RelayCommand]
    public async Task LoadAsync()
    {
        IsLoading = true;
        ErrorMessage = null;
        try
        {
            var dto = await WarmCache.GetOrFetch("categories", WarmCache.ColdTtl, () => _api.Categories());
            var pinned = TopicPinStore.Load();
            Items.Clear();
            var list = (dto?.All.Count > 0 ? dto.All : dto?.Categories) ?? new List<CategoryDto>();
            foreach (var c in list)
            {
                var row = new CategoryRowVm(TogglePin)
                {
                    Id = c.Id,
                    Label = c.Label,
                    DotColor = ParseHex(c.Color) ?? CategoryPalette.ColorFor(c.Id),
                    IsPinned = pinned.Contains(c.Id)
                };
                Items.Add(row);
            }
        }
        catch (Exception ex) { ErrorMessage = ex.Message; }
        finally { IsLoading = false; Notify(); }
    }

    private void TogglePin(CategoryRowVm row)
    {
        row.IsPinned = TopicPinStore.Toggle(row.Id);
        OnPropertyChanged(nameof(Subtitle));
    }

    private static Color? ParseHex(string? hex)
    {
        if (string.IsNullOrEmpty(hex)) return null;
        try { return CategoryPalette.FromHex(hex); } catch { return null; }
    }
}
