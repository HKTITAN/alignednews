using Microsoft.UI.Xaml;

namespace Aligned.App;

public partial class App : Application
{
    public Window? Window { get; private set; }

    public App() => InitializeComponent();

    public new static App Current => (App)Application.Current;

    protected override void OnLaunched(LaunchActivatedEventArgs args)
    {
        Window = new MainWindow();
        Window.Activate();
    }
}
