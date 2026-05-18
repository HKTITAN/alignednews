using Microsoft.UI.Xaml.Controls;
using Aligned.App.ViewModels;

namespace Aligned.App.Views;

public sealed partial class MapPage : Page
{
    public MapViewModel ViewModel { get; }
    public MapPage()
    {
        InitializeComponent();
        ViewModel = (MapViewModel)DataContext;
    }
}
