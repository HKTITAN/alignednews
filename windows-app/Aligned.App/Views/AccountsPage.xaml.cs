using Microsoft.UI.Xaml.Controls;
using Aligned.App.ViewModels;

namespace Aligned.App.Views;

public sealed partial class AccountsPage : Page
{
    public AccountsViewModel ViewModel { get; }
    public AccountsPage()
    {
        InitializeComponent();
        ViewModel = (AccountsViewModel)DataContext;
    }
}
