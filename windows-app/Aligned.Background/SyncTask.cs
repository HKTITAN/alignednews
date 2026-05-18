using Aligned.Core.Net;
using Microsoft.Windows.AppNotifications;
using Microsoft.Windows.AppNotifications.Builder;

namespace Aligned.Background;

/// <summary>
/// Hot-tier background sync (M5 will wire this to a Windows BackgroundTask
/// registered via AppNotificationManager.Default and a maintenance trigger).
/// For now: poll-once-and-exit, dispatch a "daily brief" toast if a new
/// /api/lists overview timestamp is observed vs. the last persisted value.
/// </summary>
internal class Program
{
    static async Task<int> Main(string[] args)
    {
        AppNotificationManager.Default.Register();
        try
        {
            using var api = new AlignedApi();
            var news = await api.News();
            var lists = await api.Lists();

            var count = news?.Stories?.Count ?? 0;
            var ts    = lists?.Overview?.Timestamp ?? "";
            var lastKnown = LastBriefStamp.Read();
            if (!string.IsNullOrEmpty(ts) && ts != lastKnown)
            {
                LastBriefStamp.Write(ts);
                var note = new AppNotificationBuilder()
                    .AddText("ALIGNED — today's brief")
                    .AddText($"{count} stories synced · {DateTime.Now:t}")
                    .BuildNotification();
                AppNotificationManager.Default.Show(note);
            }
            return 0;
        }
        catch (Exception ex)
        {
            Console.Error.WriteLine($"sync failed: {ex.Message}");
            return 1;
        }
    }
}

internal static class LastBriefStamp
{
    private static string Path() =>
        System.IO.Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData),
            "Aligned", "last_brief.txt");

    public static string Read()
    {
        try { return File.Exists(Path()) ? File.ReadAllText(Path()).Trim() : ""; }
        catch { return ""; }
    }

    public static void Write(string ts)
    {
        try
        {
            Directory.CreateDirectory(System.IO.Path.GetDirectoryName(Path())!);
            File.WriteAllText(Path(), ts);
        }
        catch { /* best-effort */ }
    }
}
