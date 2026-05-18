using Microsoft.UI.Xaml;
using Microsoft.UI.Xaml.Controls;
using Microsoft.UI.Xaml.Input;
using Windows.System;
using Aligned.App.ViewModels;

namespace Aligned.App.Views;

public sealed partial class ChatPage : Page
{
    public ChatViewModel ViewModel { get; }
    public ChatPage()
    {
        InitializeComponent();
        ViewModel = (ChatViewModel)DataContext;
        ViewModel.Messages.CollectionChanged += (_, __) =>
            DispatcherQueue.TryEnqueue(() => MessagesScroll?.ChangeView(null, double.MaxValue, null, false));
    }

    private void OnSendClick(object sender, RoutedEventArgs e)
    {
        if (ViewModel.IsStreaming) ViewModel.Stop();
        else if (ViewModel.SendCommand.CanExecute(null)) ViewModel.SendCommand.Execute(null);
    }

    private void OnComposerKeyDown(object sender, KeyRoutedEventArgs e)
    {
        if (e.Key == VirtualKey.Enter && ViewModel.SendCommand.CanExecute(null))
        {
            ViewModel.SendCommand.Execute(null);
            e.Handled = true;
        }
    }
}
