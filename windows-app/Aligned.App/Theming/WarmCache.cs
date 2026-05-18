using System.Text.Json;
using Windows.Storage;

namespace Aligned.App.Theming;

/// <summary>
/// On-disk JSON cache under LocalFolder/cache/. Mirrors the Android Room "blob_cache"
/// table: one file per kind ("news", "lists", "map", "events", "categories", "history").
/// </summary>
public static class WarmCache
{
    public static readonly TimeSpan StoryTtl = TimeSpan.FromDays(7);
    public static readonly TimeSpan WarmTtl = TimeSpan.FromMinutes(30);
    public static readonly TimeSpan ColdTtl = TimeSpan.FromHours(6);

    private static readonly JsonSerializerOptions Json = new(JsonSerializerDefaults.Web);

    private static StorageFolder Root() =>
        ApplicationData.Current.LocalFolder.CreateFolderAsync("cache", CreationCollisionOption.OpenIfExists)
            .AsTask().GetAwaiter().GetResult();

    private static string PathFor(string kind) =>
        System.IO.Path.Combine(Root().Path, kind + ".json");

    public static async Task<T?> ReadFresh<T>(string kind, TimeSpan ttl) where T : class
    {
        var path = PathFor(kind);
        if (!File.Exists(path)) return null;
        var age = DateTime.UtcNow - File.GetLastWriteTimeUtc(path);
        if (age > ttl) return null;
        try
        {
            await using var s = File.OpenRead(path);
            return await JsonSerializer.DeserializeAsync<T>(s, Json);
        }
        catch { return null; }
    }

    /// <summary>Reads any cached entry regardless of age (used as offline fallback).</summary>
    public static async Task<T?> ReadStale<T>(string kind) where T : class
    {
        var path = PathFor(kind);
        if (!File.Exists(path)) return null;
        try
        {
            await using var s = File.OpenRead(path);
            return await JsonSerializer.DeserializeAsync<T>(s, Json);
        }
        catch { return null; }
    }

    public static async Task Write<T>(string kind, T value)
    {
        var path = PathFor(kind);
        try
        {
            await using var s = File.Create(path);
            await JsonSerializer.SerializeAsync(s, value, Json);
        }
        catch { /* best-effort */ }
    }

    /// <summary>Get-or-fetch pattern: cached if fresh, else network → cache → return; on net failure, fall back to stale.</summary>
    public static async Task<T?> GetOrFetch<T>(string kind, TimeSpan ttl, Func<Task<T?>> fetch) where T : class
    {
        var cached = await ReadFresh<T>(kind, ttl);
        if (cached != null) return cached;
        try
        {
            var fresh = await fetch();
            if (fresh != null) await Write(kind, fresh);
            return fresh ?? await ReadStale<T>(kind);
        }
        catch
        {
            return await ReadStale<T>(kind);
        }
    }
}
