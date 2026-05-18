using Microsoft.UI;
using Microsoft.UI.Xaml.Media;
using Windows.UI;

namespace Aligned.App.Theming;

/// <summary>
/// Fallback category colors (mirrors design/tokens.json:color.category and the live
/// /api/categories response). UI uses this when the API hasn't returned a per-category
/// color, or when the category isn't present in the live catalogue.
/// </summary>
public static class CategoryPalette
{
    private static readonly Dictionary<string, Color> Map = new(StringComparer.OrdinalIgnoreCase)
    {
        ["ai-headlines"]     = FromHex("#3B82F6"),
        ["ai-companies"]     = FromHex("#6366F1"),
        ["ai-papers"]        = FromHex("#A78BFA"),
        ["ai-models"]        = FromHex("#818CF8"),
        ["ai-events"]        = FromHex("#0EA5E9"),
        ["ai-scoble"]        = FromHex("#F59E0B"),
        ["ai-conversations"] = FromHex("#10B981"),
        ["ai-safety"]        = FromHex("#F87171"),
        ["ai-robotics"]      = FromHex("#FB923C"),
        ["ai-vehicles"]      = FromHex("#16A34A"),
        ["creative-ai"]      = FromHex("#F472B6"),
        ["ai-decentralized"] = FromHex("#EAB308"),
        ["ai-spatial"]       = FromHex("#14B8A6"),
        ["ai-videos"]        = FromHex("#8B5CF6"),
        ["ai-agents"]        = FromHex("#06B6D4"),
        ["ai-entrepreneurs"] = FromHex("#A855F7"),
        ["ai-investors"]     = FromHex("#34D399"),
        ["ai-neuro"]         = FromHex("#EC4899"),
        ["world-models"]     = FromHex("#2DD4BF"),
        ["xai-news"]         = FromHex("#1D4ED8"),
        ["quantum"]          = FromHex("#7C3AED"),
        ["dev-tools"]        = FromHex("#0891B2"),
        ["health-biotech"]   = FromHex("#F43F5E"),
        ["china-global-ai"]  = FromHex("#DC2626"),
        ["ai-tech"]          = FromHex("#3B82F6"),
        ["business"]         = FromHex("#10B981"),
        ["science"]          = FromHex("#8B5CF6"),
        ["world"]            = FromHex("#F59E0B"),
        ["sports"]           = FromHex("#06B6D4"),
        ["funding"]          = FromHex("#34D399"),
        ["research"]         = FromHex("#A78BFA"),
        ["robotics"]         = FromHex("#FB923C"),
    };

    public static Color ColorFor(string? id)
    {
        if (string.IsNullOrEmpty(id)) return FromHex("#0A84FF");
        return Map.TryGetValue(id, out var c) ? c : FromHex("#0A84FF");
    }

    public static Brush BrushFor(string? id) => new SolidColorBrush(ColorFor(id));

    public static Color FromHex(string hex)
    {
        var v = hex.TrimStart('#');
        if (v.Length == 6) v = "FF" + v;
        if (v.Length != 8) return Colors.Black;
        byte a = Convert.ToByte(v[..2], 16);
        byte r = Convert.ToByte(v[2..4], 16);
        byte g = Convert.ToByte(v[4..6], 16);
        byte b = Convert.ToByte(v[6..8], 16);
        return Color.FromArgb(a, r, g, b);
    }

    public static string Pretty(string? id)
    {
        if (string.IsNullOrEmpty(id)) return "";
        var parts = id.Split('-');
        return string.Join(" ", parts.Select(p => p.Length > 0 ? char.ToUpper(p[0]) + p[1..] : p));
    }
}
