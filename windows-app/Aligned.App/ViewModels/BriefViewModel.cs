using System.Collections.ObjectModel;
using CommunityToolkit.Mvvm.ComponentModel;
using CommunityToolkit.Mvvm.Input;
using Microsoft.UI.Xaml;
using Windows.UI;
using Aligned.App.Theming;
using Aligned.Core.Net;

namespace Aligned.App.ViewModels;

public class BriefGroupVm
{
    public string Id { get; set; } = "";
    public string Name { get; set; } = "";
    public string Summary { get; set; } = "";
    public int StoryCount { get; set; }
    public Color DotColor { get; set; }
    public string CountLabel => StoryCount > 0 ? $"· {StoryCount}" : "";
}

public partial class BriefViewModel : ObservableObject
{
    private readonly AlignedApi _api = new();

    [ObservableProperty] private bool _isLoading;
    [ObservableProperty] private string? _errorMessage;
    [ObservableProperty] private string _dateLabel = "Today";
    [ObservableProperty] private string? _execSummary;

    public ObservableCollection<BriefGroupVm> Groups { get; } = new();

    public Visibility LoadingVisibility => IsLoading ? Visibility.Visible : Visibility.Collapsed;
    public Visibility ErrorVisibility => !IsLoading && ErrorMessage != null && Groups.Count == 0 ? Visibility.Visible : Visibility.Collapsed;
    public Visibility ContentVisibility => !IsLoading && Groups.Count > 0 ? Visibility.Visible : Visibility.Collapsed;
    public Visibility SummaryVisibility => !string.IsNullOrWhiteSpace(ExecSummary) ? Visibility.Visible : Visibility.Collapsed;

    public BriefViewModel() => _ = LoadAsync();

    partial void OnIsLoadingChanged(bool value) { Notify(); }
    partial void OnExecSummaryChanged(string? value) => OnPropertyChanged(nameof(SummaryVisibility));

    private void Notify()
    {
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
            var dto = await WarmCache.GetOrFetch("lists", WarmCache.WarmTtl, () => _api.Lists());
            DateLabel = dto?.Overview?.Date ?? "Today";
            ExecSummary = dto?.Overview?.ExecSummary;
            Groups.Clear();
            foreach (var g in dto?.Groups ?? new())
            {
                Groups.Add(new BriefGroupVm
                {
                    Id = g.GroupId,
                    Name = g.GroupName,
                    Summary = g.ExecSummary,
                    StoryCount = g.StoryCount,
                    DotColor = CategoryPalette.ColorFor(g.GroupId)
                });
            }
        }
        catch (Exception ex) { ErrorMessage = ex.Message; }
        finally { IsLoading = false; Notify(); }
    }
}
