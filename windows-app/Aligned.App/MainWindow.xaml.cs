using Microsoft.UI.Xaml;
using Microsoft.UI.Xaml.Controls;
using Microsoft.UI.Xaml.Media.Animation;
using Microsoft.UI.Xaml.Navigation;
using Aligned.App.Views;

namespace Aligned.App;

public sealed partial class MainWindow : Window
{
    public MainWindow()
    {
        InitializeComponent();
        ContentFrame.Navigate(typeof(FeedPage));
    }

    private void OnNavSelected(NavigationView sender, NavigationViewSelectionChangedEventArgs e)
    {
        if (e.SelectedItemContainer is not NavigationViewItem item) return;
        var tag = item.Tag as string;
        var (page, parameter) = tag switch
        {
            "feed"     => (typeof(FeedPage),        (object?)null),
            "chat"     => (typeof(ChatPage),        null),
            "brief"    => (typeof(PlaceholderPage), "brief"),
            "map"      => (typeof(PlaceholderPage), "map"),
            "research" => (typeof(PlaceholderPage), "research"),
            "settings" => (typeof(PlaceholderPage), "settings"),
            _          => (typeof(FeedPage), null)
        };
        ContentFrame.Navigate(page, parameter, new SuppressNavigationTransitionInfo());
    }

    public void NavigateToStory(string id) =>
        ContentFrame.Navigate(typeof(StoryPage), id);
}
