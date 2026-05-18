using Microsoft.UI.Xaml;
using Microsoft.UI.Xaml.Controls;
using Microsoft.UI.Xaml.Media.Animation;
using Aligned.App.Views;

namespace Aligned.App;

public sealed partial class MainWindow : Window
{
    public MainWindow()
    {
        InitializeComponent();
        ExtendsContentIntoTitleBar = true;
        ContentFrame.Navigate(typeof(FeedPage));
    }

    private void OnNavSelected(NavigationView sender, NavigationViewSelectionChangedEventArgs e)
    {
        if (e.SelectedItemContainer is not NavigationViewItem item) return;
        var tag = item.Tag as string;
        var page = tag switch
        {
            "feed"       => typeof(FeedPage),
            "brief"      => typeof(BriefPage),
            "map"        => typeof(MapPage),
            "events"     => typeof(EventsPage),
            "search"     => typeof(SearchPage),
            "chat"       => typeof(ChatPage),
            "summarize"  => typeof(SummarizePage),
            "research"   => typeof(ResearchPage),
            "bookmarks"  => typeof(BookmarksPage),
            "history"    => typeof(HistoryPage),
            "categories" => typeof(CategoriesPage),
            "accounts"   => typeof(AccountsPage),
            "settings"   => typeof(SettingsPage),
            _            => typeof(FeedPage)
        };
        ContentFrame.Navigate(page, null, new EntranceNavigationTransitionInfo());
    }

    public void NavigateToStory(string id) =>
        ContentFrame.Navigate(typeof(StoryPage), id, new DrillInNavigationTransitionInfo());
}
