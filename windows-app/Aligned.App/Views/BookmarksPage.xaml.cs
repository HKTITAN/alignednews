using Microsoft.UI.Xaml;
using Microsoft.UI.Xaml.Controls;
using Microsoft.UI.Xaml.Navigation;
using Aligned.App.ViewModels;

namespace Aligned.App.Views;

public sealed partial class BookmarksPage : Page
{
    public BookmarksViewModel ViewModel { get; }
    public BookmarksPage()
    {
        InitializeComponent();
        ViewModel = (BookmarksViewModel)DataContext;
    }

    protected override async void OnNavigatedTo(NavigationEventArgs e)
    {
        await ViewModel.LoadAsync();
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
