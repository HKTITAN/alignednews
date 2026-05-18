using Microsoft.UI.Xaml.Controls;
using Aligned.App.ViewModels;

namespace Aligned.App.Views;

public sealed partial class CategoriesPage : Page
{
    public CategoriesViewModel ViewModel { get; }
    public CategoriesPage()
    {
        InitializeComponent();
        ViewModel = (CategoriesViewModel)DataContext;
    }
}
