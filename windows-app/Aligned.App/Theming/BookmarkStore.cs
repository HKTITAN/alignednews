using Windows.Storage;

namespace Aligned.App.Theming;

/// <summary>
/// Local-only bookmark store backed by ApplicationData.LocalSettings.
/// Story IDs are stored as a CSV string under "bookmarks".
/// </summary>
public static class BookmarkStore
{
    private const string Key = "bookmarks";

    private static ApplicationDataContainer? Local()
    {
        try { return ApplicationData.Current?.LocalSettings; } catch { return null; }
    }

    public static HashSet<string> Load()
    {
        var local = Local();
        if (local is null) return new();
        var raw = local.Values[Key] as string ?? "";
        return raw.Split(',', StringSplitOptions.RemoveEmptyEntries).ToHashSet();
    }

    public static void Save(HashSet<string> ids)
    {
        var local = Local();
        if (local is null) return;
        local.Values[Key] = string.Join(',', ids);
    }

    public static bool Toggle(string id)
    {
        var s = Load();
        bool added;
        if (s.Contains(id)) { s.Remove(id); added = false; }
        else { s.Add(id); added = true; }
        Save(s);
        return added;
    }

    public static bool IsBookmarked(string id) => Load().Contains(id);
}

/// <summary>
/// Local-only topic-pin store. Category IDs stored as CSV string under "topic_pins".
/// </summary>
public static class TopicPinStore
{
    private const string Key = "topic_pins";

    private static ApplicationDataContainer? Local()
    {
        try { return ApplicationData.Current?.LocalSettings; } catch { return null; }
    }

    public static HashSet<string> Load()
    {
        var local = Local();
        if (local is null) return new();
        var raw = local.Values[Key] as string ?? "";
        return raw.Split(',', StringSplitOptions.RemoveEmptyEntries).ToHashSet();
    }

    public static void Save(HashSet<string> ids)
    {
        var local = Local();
        if (local is null) return;
        local.Values[Key] = string.Join(',', ids);
    }

    public static bool Toggle(string id)
    {
        var s = Load();
        bool added;
        if (s.Contains(id)) { s.Remove(id); added = false; }
        else { s.Add(id); added = true; }
        Save(s);
        return added;
    }

    public static bool IsPinned(string id) => Load().Contains(id);
}
