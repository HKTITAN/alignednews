using Microsoft.UI.Xaml.Controls;
using Aligned.App.ViewModels;

namespace Aligned.App.Views;

public sealed partial class ChatPage : Page
{
    public ChatViewModel ViewModel { get; }
    public ChatPage()
    {
        InitializeComponent();
        ViewModel = (ChatViewModel)DataContext;
    }
}
