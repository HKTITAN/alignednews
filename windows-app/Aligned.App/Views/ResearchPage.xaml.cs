using Microsoft.UI.Xaml.Controls;
using Aligned.App.ViewModels;

namespace Aligned.App.Views;

public sealed partial class ResearchPage : Page
{
    public ResearchViewModel ViewModel { get; }
    public ResearchPage()
    {
        InitializeComponent();
        ViewModel = (ResearchViewModel)DataContext;
    }
}
