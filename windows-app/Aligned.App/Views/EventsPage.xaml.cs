using Microsoft.UI.Xaml.Controls;
using Aligned.App.ViewModels;

namespace Aligned.App.Views;

public sealed partial class EventsPage : Page
{
    public EventsViewModel ViewModel { get; }
    public EventsPage()
    {
        InitializeComponent();
        ViewModel = (EventsViewModel)DataContext;
    }
}
