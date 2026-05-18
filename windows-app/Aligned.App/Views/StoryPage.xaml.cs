using Microsoft.UI.Xaml;
using Microsoft.UI.Xaml.Controls;
using Microsoft.UI.Xaml.Navigation;
using Windows.ApplicationModel.DataTransfer;
using Aligned.App.ViewModels;

namespace Aligned.App.Views;

public sealed partial class StoryPage : Page
{
    public StoryDetailViewModel ViewModel { get; }
    public StoryPage()
    {
        InitializeComponent();
        ViewModel = (StoryDetailViewModel)DataContext;
    }

    protected override async void OnNavigatedTo(NavigationEventArgs e)
    {
        if (e.Parameter is string id && !string.IsNullOrEmpty(id))
            await ViewModel.LoadAsync(id);
    }

    private void OnBackClick(object sender, RoutedEventArgs e)
    {
        if (Frame.CanGoBack) Frame.GoBack();
    }

    private async void OnInfographicClick(object sender, RoutedEventArgs e)
    {
        if (ViewModel.Story is null) return;
        var url = ViewModel.InfographicUrl();
        if (!string.IsNullOrEmpty(url))
            await Windows.System.Launcher.LaunchUriAsync(new Uri(url));
    }

    private void OnShareClick(object sender, RoutedEventArgs e)
    {
        if (ViewModel.Story is null) return;
        var s = ViewModel.Story;
        var url = $"https://alignednews.ai/story/{s.Id}";
        var pkg = new DataPackage();
        pkg.SetText($"{s.Headline}\n{url}");
        pkg.SetWebLink(new Uri(url));
        Clipboard.SetContent(pkg);
        ViewModel.LastVote = "Copied share link";
    }
}
