using Microsoft.UI.Xaml;
using Microsoft.UI.Xaml.Controls;
using Aligned.App.ViewModels;

namespace Aligned.App.Views;

public sealed partial class SearchPage : Page
{
    public SearchViewModel ViewModel { get; }
    public SearchPage()
    {
        InitializeComponent();
        ViewModel = (SearchViewModel)DataContext;
    }

    private void OnResultClick(object sender, RoutedEventArgs e)
    {
        if (sender is Button btn && btn.Tag is string id && App.Current is App app
            && app.Window is MainWindow win)
        {
            win.NavigateToStory(id);
        }
    }
}
