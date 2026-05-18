using Microsoft.UI.Xaml.Controls;
using Aligned.App.ViewModels;

namespace Aligned.App.Views;

public sealed partial class SettingsPage : Page
{
    public SettingsViewModel ViewModel { get; }
    public SettingsPage()
    {
        InitializeComponent();
        ViewModel = (SettingsViewModel)DataContext;
    }
}
