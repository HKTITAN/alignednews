using Microsoft.UI.Xaml.Controls;
using Aligned.App.ViewModels;

namespace Aligned.App.Views;

public sealed partial class BriefPage : Page
{
    public BriefViewModel ViewModel { get; }
    public BriefPage()
    {
        InitializeComponent();
        ViewModel = (BriefViewModel)DataContext;
    }
}
