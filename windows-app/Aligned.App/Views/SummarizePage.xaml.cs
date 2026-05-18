using Microsoft.UI.Xaml;
using Microsoft.UI.Xaml.Controls;
using Microsoft.UI.Xaml.Input;
using Windows.System;
using Aligned.App.ViewModels;

namespace Aligned.App.Views;

public sealed partial class SummarizePage : Page
{
    public SummarizeViewModel ViewModel { get; }

    public SummarizePage()
    {
        InitializeComponent();
        ViewModel = (SummarizeViewModel)DataContext;
    }

    private void OnInputKeyDown(object sender, KeyRoutedEventArgs e)
    {
        if (e.Key == VirtualKey.Enter && ViewModel.SubmitCommand.CanExecute(null))
        {
            ViewModel.SubmitCommand.Execute(null);
            e.Handled = true;
        }
    }

    private void OnOpenStoryClick(object sender, RoutedEventArgs e)
    {
        if (ViewModel.ResolvedStory is null) return;
        if (App.Current is App app && app.Window is MainWindow win)
            win.NavigateToStory(ViewModel.ResolvedStory.Id);
    }
}
