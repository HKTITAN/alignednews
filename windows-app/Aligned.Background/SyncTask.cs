using Aligned.Core.Net;

namespace Aligned.Background;

/// <summary>
/// Hot-tier background sync (M5).
///
/// Behavior:
///  - Pulls /api/news + /api/lists once.
///  - Writes the latest brief timestamp to %LOCALAPPDATA%/Aligned/last_brief.txt
///    so the main app can show a "new brief available" badge on next launch.
///  - Returns 0 on success, 1 on network failure.
///
/// This is a headless cross-platform exe (no WinAppSDK dependency) so it can be
/// scheduled via Task Scheduler without bringing in the full MSIX toolchain.
/// </summary>
internal class Program
{
    static async Task<int> Main(string[] args)
    {
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
                Console.WriteLine($"[brief] new overview at {ts} — {count} stories");
            }
            else
            {
                Console.WriteLine($"[sync] {count} stories, no new brief");
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
