using Aligned.Core.Net;
using Aligned.Core.Net.Dto;

using var api = new AlignedApi();

Console.WriteLine("=== /api/health ===");
var h = await api.Health();
Console.WriteLine($"  status={h?.Status}  storyCount={h?.StoryCount}  lastUpdated={h?.LastUpdated}");

Console.WriteLine("\n=== /api/news (top 3) ===");
var n = await api.News(limit: 3);
foreach (var s in (n?.Stories ?? new()).Take(3))
    Console.WriteLine($"  [{s.Category}] {s.Headline}");

Console.WriteLine("\n=== /api/chat (SSE stream) ===");
Console.Write("  Q: 'one sentence: what is alignednews.ai?'\n  A: ");
await foreach (var evt in api.Chat("In one sentence, what is alignednews.ai?"))
{
    if (evt is ChatEvent.Token t) Console.Write(t.Text);
}
Console.WriteLine();

Console.WriteLine("\n=== /api/research (start + poll) ===");
var start = await api.StartResearch("xAI Grok recent news");
Console.WriteLine($"  started id={start?.Id} status={start?.Status}");
for (int i = 0; i < 6; i++)
{
    await Task.Delay(5_000);
    var r = await api.Research(start!.Id);
    Console.WriteLine($"  poll {i}: status={r?.Status} step={r?.CurrentStep}/{r?.Steps?.Count} insights={r?.Insights?.Count}");
    if (r?.Status == "complete") break;
}

Console.WriteLine("\n=== /api/og url builder ===");
Console.WriteLine($"  {api.OgUrl(title: "Hello", subtitle: "world")}");

Console.WriteLine("\nOK.");
