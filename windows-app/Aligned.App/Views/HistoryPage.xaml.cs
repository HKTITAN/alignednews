using Microsoft.UI.Xaml.Controls;
using Aligned.App.ViewModels;

namespace Aligned.App.Views;

public sealed partial class HistoryPage : Page
{
    public HistoryViewModel ViewModel { get; }
    public HistoryPage()
    {
        InitializeComponent();
        ViewModel = (HistoryViewModel)DataContext;
    }
}
