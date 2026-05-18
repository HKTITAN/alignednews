using Microsoft.UI.Xaml.Controls;
using Microsoft.UI.Xaml.Navigation;

namespace Aligned.App.Views;

public sealed partial class PlaceholderPage : Page
{
    public PlaceholderPage() => InitializeComponent();

    protected override void OnNavigatedTo(NavigationEventArgs e)
    {
        var tag = e.Parameter as string ?? "this section";
        MessageText.Text = tag switch
        {
            "brief"    => "Today's Brief — wired in M4",
            "map"      => "Map — wired in M7",
            "chat"     => "Chat — wired in M8",
            "research" => "Research — wired in M9",
            "settings" => "Settings — wired in M10",
            _          => $"{tag} — coming soon"
        };
    }
}
