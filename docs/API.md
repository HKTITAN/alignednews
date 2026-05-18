# alignednews.ai — Reverse-Engineered API Reference

Reconstructed from black-box probing of `https://alignednews.ai/api` and the underlying Railway origin, plus the Next.js shell HTML. There is no public API key; everything below is reachable anonymously with `curl`.

## 0. Topology

The site is a **Next.js 15 / Turbopack** app. Two hostnames serve the *same* backend (the API portion at least):

- `https://alignednews.ai` — public domain
- `https://x-news-stream-robert-production.up.railway.app` — Railway origin (`Server: railway-edge`, region `asia-southeast1-eqsg3a`)

Both respond identically to `/api/*`. The Open Graph image in the page meta points directly at the Railway host, confirming the origin.

> Curious quirk: every HTML route (`/`, `/feed`, `/news`, anything other than `/api/*`) returns the Next.js **404 "_not-found"** page to a plain `curl`. The browser app must be a separate deployment or gated by something we don't see. The `/api/*` surface is fully functional regardless.

Stack signals in the shell HTML:

- Geist font, Tailwind, service-worker registered at `/sw.js`
- `meta name="robots" content="noindex"`
- Twitter creator: `@Scobleizer` (Robert Scoble) — site framing says "63 curated X/Twitter lists, 100K+ accounts"
- Services advertised by `/api/health`: **OpenAI, Tavily, Cerebras, X (Twitter)** — and the `/api/settings` model catalog lists **Anthropic** Claude as the default synthesis & chat model.

## 1. Endpoint map

All paths are GET unless noted, return JSON unless noted, no auth required.

| Method | Path | Purpose |
|---|---|---|
| GET  | `/api/health` | Liveness + service-up flags + last ingest timestamp |
| GET  | `/api/news` | Story feed (clusters of tweets per headline). Default ~200 stories. |
| GET  | `/api/news/{id}` | Single story by id |
| GET  | `/api/search?q=…` | Full-text search across stories |
| GET  | `/api/lists` | Big roll-up: editorial overview + per-group breakdowns + history dates |
| GET  | `/api/lists?group={groupId}` | Single group slice of the above |
| GET  | `/api/lists?date=YYYY-MM-DD` | Lists payload for a past day (limited window) |
| GET  | `/api/categories` | Category catalog (id, label, color, section) + custom list + flat `all` |
| GET  | `/api/map` | Geo-tagged story markers for the interactive map |
| GET  | `/api/accounts` | Tracked X accounts metadata (partial — only "added accounts") |
| GET  | `/api/history` | Recent saved research / chat sessions (titles + ids) |
| GET  | `/api/events` | Calendar/conference events extracted from stories |
| GET  | `/api/feedback` | Aggregate thumbs-up/down counts per category |
| POST | `/api/feedback` | Cast a vote on a story. Body `{storyId, category, vote: "up"\|"down"}` |
| POST | `/api/summarize` | LLM summary of an arbitrary tweet array. Body `{tweets: [{text, authorUsername}] }` |
| GET  | `/api/digest` | **Leaks newsletter subscriber list** (id, email, frequency, lang, subscribedAt) |
| GET  | `/api/settings` | Current model / language / algorithm + available model & language catalogs |
| POST | `/api/settings` | Update settings (body `{language, synthesisModel, chatModel, algorithm, customXApiKey}`) |
| GET  | `/api/feed` | RSS 2.0 (default) |
| GET  | `/api/feed?format=json` | JSON Feed 1.1 |
| GET  | `/api/feed?format=atom` / `?format=rss` | RSS/Atom |
| GET  | `/api/og` | Open Graph PNG (1200×630). Optional `?title=`, `?subtitle=`, `?storyId=` |
| GET  | `/api/infographic` | SVG 600×170 infographic. Accepts `?id=`, `?storyId=`, `?topic=` |
| POST | `/api/chat` | Streaming chatbot (SSE). Body `{message, storyId?, history?}` |
| POST | `/api/research` | Kick off a deep-research session. Body `{query, sessionId?}` → `{id, status}` |
| GET  | `/api/research?id=…` | Poll a research session's progress + insights |
| POST | `/api/refresh` | Trigger an out-of-band ingest cycle (returns `{status:"processing", timestamp}`) |

### 404s worth noting (probed and absent)

`/api/auth`, `/api/me`, `/api/voice/*`, `/api/realtime/*`, `/api/tts`, `/api/stt`, `/api/groups`, `/api/topics`, `/api/sources`, `/api/subscribe`, `/api/fact-check`. The site advertises an "interactive voice chatbot" but the realtime/voice endpoints are not exposed under predictable names — likely a third-party widget (OpenAI Realtime / ElevenLabs) called directly from the browser with a token minted server-side via a route we haven't found.

---

## 2. Endpoint details

### 2.1 `/api/health`

```json
{
  "status": "ok",
  "version": "1.0.0",
  "storyCount": 200,
  "lastUpdated": "2026-05-18T00:49:55.525Z",
  "services": { "twitter": true, "openai": true, "tavily": true, "cerebras": true }
}
```

### 2.2 `/api/news`

Query params (all optional):

- `category` — one of the ids from `/api/categories` (e.g. `ai-companies`, `ai-safety`, `creative-ai`, `xai-news`, …). Filtering is observed working.
- `limit` — caps `stories[]` length.
- `groupId`, `date`, `since`, `after`, `id`, `subcategory` — **accepted but appear to be ignored** in current build (response size unchanged from default).

Response:

```jsonc
{
  "stories": [
    {
      "id": "ai-safety-1779061164284-0",      // {category}-{epochMs}-{index}
      "headline": "...",
      "summary": "...",
      "category": "ai-safety",
      "tweets": [ Tweet ],
      "citations": [],
      "createdAt": "2026-05-17T23:39:24.284Z",
      "updatedAt": "2026-05-17T23:39:24.284Z",
      "tweetCount": 2,
      "totalEngagement": 2,
      "signals": { "topScore": 1, "categories": [], "flags": {} }
    }
  ]
}
```

### 2.3 `/api/news/{id}` — single story

Same `Story` shape as above, top-level (no wrapping array).

### 2.4 `Tweet` shape (shared across `/api/news`, `/api/search`, `/api/lists`)

```jsonc
{
  "id":                "2056156514872959301",        // X status id
  "text":              "...",
  "authorName":        "...",
  "authorUsername":    "...",
  "authorProfileImage":"https://pbs.twimg.com/...",
  "authorFollowers":   8,
  "authorLocation":    "...",                        // present in /api/lists tweets
  "createdAt":         "2026-05-17T23:34:49.000Z",
  "likes":   1, "retweets": 1, "replies": 0,
  "quotes":  0, "bookmarks": 0, "views": 10,
  "url":               "https://x.com/{user}/status/{id}",
  "media": [
    { "type": "photo", "url": "https://pbs.twimg.com/media/...", "width": 2044, "height": 2048 }
  ]
}
```

### 2.5 `/api/search?q={query}&limit={n}`

- `q` is the only working param (`query=` returns empty).
- Empty `q` returns `{ "stories": [], "query": "" }`.
- Returns `{ stories: Story[], query }` with the same `Story` shape as the feed.

### 2.6 `/api/lists` — editorial roll-up (~390 KB)

This is the heaviest payload, and the "AI editor's brief" of the day. Shape:

```jsonc
{
  "overview": {
    "timestamp": "2026-05-18T04:32:33.780Z",
    "date":      "2026-05-17",
    "execSummary": "...",                    // ~1 paragraph
    "execBriefing": {
      "leadHeadline": "...",
      "leadGroupId":  "ai-leaders",
      "leadBody":     "...",                 // multi-paragraph
      "sections": [
        { "subhead": "...", "groupId": "ai-community", "body": "..." }
      ]
    },
    "groupSummaries": [
      { "groupId": "ai-community", "groupName": "AI Community",
        "color": "#3B82F6", "summary": "...", "storyCount": 10 }
    ],
    "topStories": [
      { "headline": "...", "groupId": "creative-ai",
        "sourceHandle": "@LuisBetx9", "engagement": 11274,
        "tweetUrl": "https://x.com/LuisBetx9/status/..." }
    ]
  },

  "groups": [
    {
      "groupId":   "ai-community",
      "groupName": "AI Community",
      "timestamp": "...",
      "execSummary": "...",
      "storyCount": 10,
      "subcategories": [
        {
          "id":         "ai-community-sub-0",
          "name":       "AI Research & Technical Advances",
          "summary":    "...",
          "storyCount": 3,
          "stories": [
            {
              "id":       "ai-community-0-0-1779065433708",
              "headline": "...",
              "summary":  "...",
              "analysis": "...",                  // editor's take
              "sourceHandle": "@MuzafferKal_",
              "tweets":      [ Tweet ],
              "engagement":  ...,
              "createdAt":   "..."
            }
          ]
        }
      ],
      "topStories": [ /* same shape as subcategories[].stories[] */ ]
    }
  ],

  "historyDates": [ "2026-05-17", "2026-05-16", ... ]   // ~30 days
}
```

Known `groupId` values (18):

```
ai-community, ai-leaders, creative-ai, investors, founders,
ai-companies, ai-orgs, robots, spatial, climate, crypto,
developers, brain-neuro, news, quantum, security, evs-tesla, xai
```

`?group={groupId}` returns just the matching `groups[]` element (no `overview`, no `historyDates`).
`?date=YYYY-MM-DD` returns the same top-level shape for an archived day (only dates listed in `historyDates` are valid).

### 2.7 `/api/categories`

```jsonc
{
  "categories": [ { "id": "ai-companies", "label": "AI Company News",
                    "color": "#6366F1", "bgColor": "bg-indigo-500/20",
                    "section": "ai-news" }, ... ],
  "custom":     [ { "id": "bible", "label": "Bible", "color": "#F97316",
                    "bgColor": "bg-white/10" } ],
  "all":        [ /* categories + custom flattened, with `all` row prepended */ ]
}
```

Note these are **content categories** (per-story tagging) and are a *different* taxonomy from the **groups** (`/api/lists`). Categories are flat; groups have subcategories + editorial summaries.

### 2.8 `/api/map`

```jsonc
{
  "markers": [
    {
      "lat": 37.7749, "lng": -122.4194,
      "city": "San Francisco", "country": "US",
      "groupId": "ai-leaders", "groupName": "AI Leaders", "groupColor": "#6366F1",
      "stories": [
        { "headline": "...", "sourceHandle": "@sama",
          "tweetUrl": "https://x.com/sama/status/..." }
      ]
    }
  ]
}
```

Geo-tagged per story → city granularity. Multiple markers per story is possible (the same headline appears across many cities tied to author/quoter geos).

### 2.9 `/api/accounts`

```jsonc
{
  "accounts": [
    {
      "id":          "13348",
      "username":    "Scobleizer",
      "name":        "Robert Scoble",
      "profileImage":"https://pbs.twimg.com/...",
      "followers":   576554,
      "description": "...",
      "addedAt":     "2026-04-23T11:46:39.105Z"
    }
  ]
}
```

The response is small (~2 accounts) — clearly *not* the full 100K-account list. Best read as "accounts manually pinned by the operator", not the entire monitored universe.

### 2.10 `/api/history`

Flat array of recent saved sessions (research + chat):

```jsonc
[
  {
    "id":           "research-research-1778546086499",
    "type":         "research",            // or "chat"
    "query":        "Fact check: ...",
    "confidence":   83,
    "insightCount": 6,
    "completedAt":  "2026-05-12T00:36:30.443Z"
  }
]
```

### 2.11 `/api/digest`  ⚠

**Leaks newsletter subscribers.** No auth.

```jsonc
{
  "subscribers": [
    { "id": "sub-1776397105973", "email": "user@example.com",
      "frequency": "daily", "categories": [], "language": "en",
      "subscribedAt": "2026-04-17T03:38:25.973Z" }
  ],
  "count": 2
}
```

This should probably be admin-only. Worth flagging to the operator (`@Scobleizer`).

### 2.12 `/api/settings`

```jsonc
{
  "settings": {
    "synthesisModel": "claude-sonnet-4-6",
    "chatModel":      "claude-haiku-4-5-20251001",
    "language":       "en",
    "customXApiKey":  "",
    "algorithm":      "realign"
  },
  "availableModels": [
    { "id": "claude-sonnet-4-6",          "label": "Advanced (Recommended)" },
    { "id": "claude-haiku-4-5-20251001",  "label": "Fast" },
    { "id": "gpt-4o-mini",                "label": "Lightweight" }
  ],
  "availableLanguages": [
    { "id": "en", "label": "English",          "dir": "ltr" },
    { "id": "fa", "label": "فارسی (Farsi)",   "dir": "rtl" },
    { "id": "zh", "label": "中文 (Chinese)",   "dir": "ltr" },
    { "id": "es", "label": "Español (Spanish)","dir": "ltr" },
    { "id": "ar", "label": "العربية (Arabic)","dir": "rtl" }
  ]
}
```

**POST** with any subset of `{language, synthesisModel, chatModel, algorithm, customXApiKey}` returns `{settings: {…updated}}`. No auth — these settings are **global / server-side**, not per-user. Writing to them mutates everyone's experience.

### 2.13 `/api/feed`

| Query | Content-Type |
|---|---|
| (none) / `?format=rss` / `?format=atom` | `application/rss+xml` |
| `?format=json` | `application/feed+json` (JSON Feed 1.1) |

The JSON Feed payload carries a custom `_x_news_stream` extension per item with `{category, engagement, sourceHandle, tweetUrl}`.

### 2.14 `/api/og`

Returns a 1200×630 PNG. Params observed accepted:

- `?title=...` and `?subtitle=...` — render custom text
- `?storyId=...` — accepted (returns 200) but the rendered image doesn't visibly change in the absence of `title`

### 2.15 `/api/infographic`

Returns a 600×170 SVG card. Accepts `?id=`, `?storyId=`, `?topic=`. In current state every variant returns the same generic template — the dynamic data must be wired through a different route or the feature is stubbed.

### 2.16 `/api/chat`  (POST, SSE)

```http
POST /api/chat
Content-Type: application/json

{ "message": "summarize today's xAI news",
  "storyId": "ai-safety-1779061164284-0",   // optional, scopes context to a story
  "history": [ { "role": "user", "content": "..." },
               { "role": "assistant", "content": "..." } ]  // optional
}
```

Required: `message` (else `400 {"error":"Message is required"}`).

Response is `text/event-stream`. Event payload shape:

```
data: {"type":"token","text":"..."}
data: {"type":"token","text":"..."}
...
data: {"type":"done"}
```

Identity: "AI News Assistant for alignednews.ai — access to real-time news and insights from Robert Scoble's 63 curated X/Twitter lists covering 100,000+ accounts."

### 2.17 `/api/research`  (POST / GET)

**Start a session:**

```http
POST /api/research
Content-Type: application/json

{ "query": "openai news" }   // sessionId is accepted but ignored — server mints `id`
```

Required: `query` (else `400 {"error":"Query is required"}`).

```json
{ "id": "research-1779079112855", "status": "running" }
```

**Poll progress:**

```http
GET /api/research?id=research-1779079112855
```

(Note: the error message says "Session ID required" but the working query parameter is **`id`**, not `sessionId`.)

```jsonc
{
  "id": "research-1779079112855",
  "query": "openai news",
  "status": "running" | "done" | "failed",
  "steps": [
    { "name": "Parsing query & identifying entities", "status": "done",
      "detail": "Found 5 entities, 4 search variants" },
    { "name": "Searching X for relevant tweets",       "status": "done",  "detail": "..." },
    { "name": "Cross-referencing with web sources",    "status": "done",  "detail": "..." },
    { "name": "Synthesizing findings",                 "status": "pending" },
    { "name": "Analyzing sentiment & narrative trends","status": "done",  "detail": "..." },
    { "name": "Identifying key influencers & amplifiers","status": "done","detail": "..." },
    { "name": "Detecting counter-narratives & opposing views","status": "running" },
    { "name": "Mapping temporal event timeline",       "status": "pending" },
    { "name": "Cross-referencing source credibility",  "status": "pending" },
    { "name": "Generating insight cards",              "status": "pending" }
  ],
  "currentStep":   6,
  "insights":      [ /* InsightCard[] — empty until `done` */ ],
  "sourceNetwork": [ /* graph nodes/edges */ ],
  "activityLog":   [ "Research started for: \"openai news\" (10 steps)", "[9:38:32 PM] ..." ]
}
```

The pipeline is fixed at 10 named steps — useful as a UI scaffold even without finished output.

### 2.18 `/api/events`

```jsonc
{
  "events": [
    {
      "id":          "ai-papers-1779061126450-0",
      "name":        "Hugging Face publishes 53 AI papers for May 17…",
      "date":        "2026-05-17",                // or "TBD"
      "location":    "TBD",
      "category":    "conference",
      "description": "...",
      "source": {
        "headline":       "...",
        "authorUsername": "LianwenJ",
        "tweetUrl":       "https://x.com/LianwenJ/status/2056156906801213850"
      }
    }
  ]
}
```

Events are LLM-extracted from the same story pool — no separate curation pipeline visible.

### 2.19 `/api/feedback`

**GET** — aggregate vote counters (global):

```json
{
  "total": 12,
  "thumbsUp": 9,
  "thumbsDown": 3,
  "byCategory": {
    "ai-safety": { "up": 2, "down": 0 },
    "ai-tech":   { "up": 3, "down": 1 }
  }
}
```

**POST** — cast a vote. Required: `storyId`, `category`. Vote field is `vote` (the response renames it to `sentiment`).

```json
{ "storyId": "ai-safety-1779061164284-0",
  "category": "ai-safety",
  "vote": "up" }
```

Response echoes `{feedback: {storyId, headline, category, sentiment, timestamp}}`. The vote appears to **replace** the previous vote on the same `storyId` (totals stayed flat across `up` → `down` toggling) — so this is more like a state set than an additive counter, but there's no auth or rate limit, so anyone can flip any story's vote.

### 2.20 `/api/summarize`  (POST)

```http
POST /api/summarize
Content-Type: application/json

{ "tweets": [
    { "text": "OpenAI launches GPT-5…", "authorUsername": "sama" },
    { "text": "Benchmark shows 40% gain", "authorUsername": "openai" }
  ]
}
```

Returns:

```jsonc
{
  "overview":   "Both tweets focus on the launch of OpenAI's new GPT-5…",
  "individual": [ "Tweet-1 summary…", "Tweet-2 summary…" ]
}
```

A general-purpose LLM endpoint accepting **arbitrary** tweet text — no validation that the tweets came from the site's own corpus. This is an open LLM proxy. Cost burned per request lands on the operator's OpenAI / Anthropic bill.

### 2.21 `/api/refresh`  (POST)

```json
{ "status": "processing", "timestamp": "2026-05-18T04:39:21.500Z" }
```

Fires an out-of-band ingest cycle. No auth. Cheap to call but probably rate-limited at the application layer.

---

## 3. Auth, headers, anti-abuse

Standard hardening headers from Railway edge:

```
X-Content-Type-Options: nosniff
X-Frame-Options: DENY
Referrer-Policy: strict-origin-when-cross-origin
Permissions-Policy: camera=(), microphone=(self), geolocation=()
Vary: rsc, next-router-state-tree, next-router-prefetch, next-router-segment-prefetch
```

- No `Authorization`, no API key, no cookie required for any endpoint listed above.
- No CORS preflight observed (calls work cross-origin from `curl`).
- No rate-limit headers surfaced. Expect Railway-edge or Cloudflare-style throttling silently.
- `Permissions-Policy: microphone=(self)` confirms the voice feature is planned/wired even though the realtime endpoint name isn't discoverable.

**Security observations worth flagging to the operator** (each confirmed by live probing, not just inferred from response codes):

1. `/api/digest` returns the full subscriber email list unauthenticated. Two real emails observed in the response.
2. `/api/settings` POST is **globally persistent with no auth — confirmed**. Wrote a canary value `CANARY-PROBE-DO-NOT-USE-12345` to `customXApiKey`, re-GET'd and the canary came back, then cleared it. The fact that `customXApiKey` is a *stored field* implies an attacker can plant a key that the backend will then use for downstream X API calls — i.e. exfiltrate the site's X traffic through their own key, *or* inject a malformed key to break ingestion site-wide. Same with `synthesisModel` / `chatModel` / `algorithm`: anyone can swap them for the whole site.
3. `/api/refresh` POST is unauthenticated and kicks off backend ingest. No rate limit headers surfaced.
4. `/api/feedback` POST has no auth and no per-user identity, so a single actor can drown the global thumbs-up/down counters.
5. `/api/summarize` POST is an **open LLM proxy** that accepts arbitrary tweet-like text and runs it through the operator's paid LLM bill. No size cap observed.
6. `/api/history` lists prior research session ids and queries publicly; combined with `/api/research?id=…`, anyone can read the full content of any prior research session.

Archive depth probe: `/api/lists?date=…` only accepts dates listed in `historyDates` (~30 days; older dates return 404 + `{"error":"..."}`). `/api/news?date=…` accepts any value but **silently ignores it** — the response body is identical (same 310,960 bytes for `2026-05-16`, `2026-04-17`, `2026-01-01`). So there is no working day-by-day news archive endpoint despite the parameter being accepted.

---

## 4. Quick start (curl)

```bash
BASE=https://alignednews.ai     # or https://x-news-stream-robert-production.up.railway.app

# Liveness
curl "$BASE/api/health"

# Today's feed (~200 stories)
curl "$BASE/api/news" | jq '.stories | length'

# Filter by category
curl "$BASE/api/news?category=xai-news&limit=5"

# A single story
curl "$BASE/api/news/ai-safety-1779061164284-0"

# Full-text search
curl "$BASE/api/search?q=openai&limit=5"

# Editorial roll-up (heavy: ~400 KB)
curl "$BASE/api/lists"
curl "$BASE/api/lists?group=xai"
curl "$BASE/api/lists?date=2026-05-16"

# Category & group catalogs
curl "$BASE/api/categories"

# Geo map markers
curl "$BASE/api/map"

# JSON Feed
curl "$BASE/api/feed?format=json"

# Streaming chat (SSE)
curl -N -X POST "$BASE/api/chat" \
  -H "Content-Type: application/json" \
  -d '{"message":"What did xAI announce today?"}'

# Deep research: start, then poll
SID=$(curl -s -X POST "$BASE/api/research" \
  -H "Content-Type: application/json" \
  -d '{"query":"openai news"}' | jq -r .id)
curl "$BASE/api/research?id=$SID"

# Cast a vote
curl -X POST "$BASE/api/feedback" \
  -H "Content-Type: application/json" \
  -d '{"storyId":"ai-safety-1779061164284-0","category":"ai-safety","vote":"up"}'

# Free LLM summary of arbitrary tweets
curl -X POST "$BASE/api/summarize" \
  -H "Content-Type: application/json" \
  -d '{"tweets":[{"text":"any text","authorUsername":"any"}]}'

# Events extracted from stories
curl "$BASE/api/events"

# Open Graph / infographic
curl -o og.png   "$BASE/api/og?title=Hello&subtitle=World"
curl -o card.svg "$BASE/api/infographic?id=ai-safety-1779061164284-0"
```

---

## 5. What's notably missing vs. digg.com

- No author rankings, no per-account profile API, no follower graph — alignednews is **story-centric**, digg is **author-centric**.
- No user accounts, no auth (Clerk on digg, none here).
- No "snapshots" / time-series rank tracking on stories. Stories here have a single engagement total, no `peakRank` / `delta` history.
- No public write endpoints (bookmark/vote/follow) — but settings *are* publicly writable, which digg's are not.
- A fully working **deep-research pipeline** and **streaming chat** endpoint — digg has neither.
- Multilingual UX baked in (`en`, `fa`, `zh`, `es`, `ar`).
