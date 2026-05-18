using System.Text.RegularExpressions;

namespace Aligned.App.Theming;

/// <summary>
/// Parses X / Twitter URLs to pull out the tweet ID and author handle.
/// Used by the Summarize-a-link feature to resolve a pasted link to the
/// tweet cluster that alignednews.ai has already grouped.
/// </summary>
public static class TweetLink
{
    private static readonly Regex Re = new(
        @"(?:twitter\.com|x\.com)/(?<handle>[^/?#]+)/status/(?<id>\d+)",
        RegexOptions.IgnoreCase | RegexOptions.Compiled);

    public static (string? Handle, string? Id) Parse(string url)
    {
        if (string.IsNullOrWhiteSpace(url)) return (null, null);
        var m = Re.Match(url);
        if (!m.Success) return (null, null);
        return (m.Groups["handle"].Value, m.Groups["id"].Value);
    }
}
