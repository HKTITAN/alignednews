using Microsoft.UI.Xaml.Controls;
using Microsoft.UI.Xaml.Navigation;
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
}
