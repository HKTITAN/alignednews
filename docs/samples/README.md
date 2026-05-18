# Sample payloads

Real responses captured from `https://alignednews.ai/api/*` on **2026-05-18**, used as test fixtures and to ground the reverse-engineering in `docs/API.md`.

| File | Endpoint | Notes |
|---|---|---|
| `aligned-health.json` | `GET /api/health` | Liveness + service-up flags |
| `aligned-categories.json` | `GET /api/categories` | The full category taxonomy (43 + 1 custom) |
| `aligned-accounts.json` | `GET /api/accounts` | Operator-pinned X accounts |
| `aligned-news-ai-safety-1779061164284-0.json` | `GET /api/news/{id}` | A single story object — full shape |

Excluded on purpose: `/api/digest` (real subscriber emails), large `/api/news` and `/api/lists` dumps (300+ KB each, not needed as fixtures). To capture fresh samples, see the smoke-test program at `windows-app/Aligned.Core.SmokeTest/Program.cs`.
