using CommunityToolkit.Mvvm.ComponentModel;
using Microsoft.UI.Xaml;
using Microsoft.UI.Xaml.Media;
using Windows.Storage;
using Aligned.App.Theming;
using Aligned.Core.Net;

namespace Aligned.App.ViewModels;

public partial class SettingsViewModel : ObservableObject
{
    private readonly AlignedApi _api = new();
    private readonly ApplicationDataContainer? _local = TryGetLocalSettings();

    [ObservableProperty] private string _health = "—";
    [ObservableProperty] private int _storyCount;
    [ObservableProperty] private string _lastUpdated = "—";
    [ObservableProperty] private string? _errorMessage;

    [ObservableProperty] private int _themeIndex;
    [ObservableProperty] private bool _breakingEnabled;
    [ObservableProperty] private bool _topicsEnabled = true;
    [ObservableProperty] private bool _briefEnabled = true;
    [ObservableProperty] private bool _researchEnabled = true;

    public Brush HealthBrush =>
        Health == "ok"
            ? new SolidColorBrush(CategoryPalette.FromHex("#34C759"))
            : new SolidColorBrush(CategoryPalette.FromHex("#86868B"));

    public Visibility ErrorVisibility => ErrorMessage != null ? Visibility.Visible : Visibility.Collapsed;

    public SettingsViewModel()
    {
        LoadPrefs();
        _ = RefreshHealth();
    }

    private static ApplicationDataContainer? TryGetLocalSettings()
    {
        try { return ApplicationData.Current?.LocalSettings; } catch { return null; }
    }

    private void LoadPrefs()
    {
        if (_local is null) return;
        ThemeIndex       = (int)(_local.Values["themeIndex"]   ?? 0);
        BreakingEnabled  = (bool)(_local.Values["nBreaking"]   ?? false);
        TopicsEnabled    = (bool)(_local.Values["nTopics"]     ?? true);
        BriefEnabled     = (bool)(_local.Values["nBrief"]      ?? true);
        ResearchEnabled  = (bool)(_local.Values["nResearch"]   ?? true);
    }

    private void Save(string key, object value)
    {
        if (_local is null) return;
        _local.Values[key] = value;
    }

    partial void OnThemeIndexChanged(int value)
    {
        Save("themeIndex", value);
        ApplyTheme(value);
    }
    partial void OnBreakingEnabledChanged(bool value) => Save("nBreaking", value);
    partial void OnTopicsEnabledChanged(bool value)   => Save("nTopics",   value);
    partial void OnBriefEnabledChanged(bool value)    => Save("nBrief",    value);
    partial void OnResearchEnabledChanged(bool value) => Save("nResearch", value);

    partial void OnHealthChanged(string value) => OnPropertyChanged(nameof(HealthBrush));
    partial void OnErrorMessageChanged(string? value) => OnPropertyChanged(nameof(ErrorVisibility));

    private async Task RefreshHealth()
    {
        try
        {
            var h = await _api.Health();
            if (h is null) return;
            Health = h.Status;
            StoryCount = h.StoryCount;
            LastUpdated = h.LastUpdated;
        }
        catch (Exception ex) { ErrorMessage = ex.Message; }
    }

    private static void ApplyTheme(int index)
    {
        // 0 = follow system, 1 = light, 2 = dark
        if (App.Current is not App app || app.Window is not Window w) return;
        if (w.Content is FrameworkElement root)
        {
            root.RequestedTheme = index switch
            {
                1 => ElementTheme.Light,
                2 => ElementTheme.Dark,
                _ => ElementTheme.Default
            };
        }
    }
}
