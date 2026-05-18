using Microsoft.UI.Xaml;
using Microsoft.UI.Xaml.Controls;
using Aligned.App.ViewModels;

namespace Aligned.App.Views;

public sealed partial class FeedPage : Page
{
    public FeedViewModel ViewModel { get; }

    public FeedPage()
    {
        InitializeComponent();
        ViewModel = (FeedViewModel)DataContext;
    }

    private void OnStoryClick(object sender, RoutedEventArgs e)
    {
        if (sender is Button btn && btn.Tag is string id && App.Current is App app
            && app.Window is MainWindow win)
        {
            win.NavigateToStory(id);
        }
    }
}
