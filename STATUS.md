# STATUS

What's actually written vs. stubbed vs. pending.

Legend: ✅ done · 🟡 partial · ⬜ not written

## Build verification

- `dotnet 10.0.300`: **Aligned.Core, Aligned.App, Aligned.Background, Aligned.Core.SmokeTest** all build with 0 errors; `dotnet publish -c Release -r win-x64` produces `Aligned.App.exe`.
- Android: built on CI (`.github/workflows/release.yml`), producing both `ALIGNED-android-debug.apk` and `ALIGNED-android-release.apk`.

## Caching

- **Stories: 7-day TTL** in Room. `StoryDao.prune(cutoff)` runs each hot-sync cycle.
- **Story detail: 7-day TTL.** Falls back to stale cache on network failure.
- **Warm tier (lists, map, events): 30-min TTL** via `blob_cache` table (Android) / `LocalFolder/cache/*.json` (Windows).
- **Cold tier (categories, history): 6-hour TTL**.
- Repository always returns stale cache as fallback when the network call fails.

## Feature inventory

| Surface | Android | Windows | Endpoints |
|---|---|---|---|
| Feed (live dot, categories, hero images, pull-to-refresh) | ✅ | ✅ | `/api/news`, `/api/categories` |
| Story detail (vote, share, bookmark, author avatars, tweet media) | ✅ | ✅ | `/api/news/{id}`, `/api/feedback` |
| Today's Brief (exec summary + group cards) | ✅ | ✅ | `/api/lists` |
| Map (color-coded markers, list view) | ✅ | ✅ | `/api/map` |
| Search (debounced 280ms) | ✅ | ✅ | `/api/search` |
| Ask / Chat (SSE streaming, bubbles, stop button, suggestions) | ✅ | ✅ | `/api/chat` |
| Research (10-step progress, insights, exec answer) | ✅ | ✅ | `/api/research` |
| Events (live conferences, color-coded by category) | ✅ | ✅ | `/api/events` |
| History (past chat/research) | ✅ | ✅ | `/api/history` |
| Bookmarks (local-only) | ✅ | ✅ | — |
| Settings (theme, notif toggles, sync log, backend health) | ✅ | ✅ | `/api/health`, `/api/settings` (read) |

## Background sync

- Android `HotSyncWorker` — every 15 min (OS floor). Refreshes /api/news, emits diff to NotificationCenter.
- Android `WarmSyncWorker` — every 30 min. Refreshes /api/lists, /api/map, /api/events.
- Android `DailyBriefWorker` — fires daily ≈08:00 local, refreshes /api/lists, dispatches "today's brief" notification.
- Android `ResearchPollService` — foreground service polls /api/research?id=… and fires "Research ready" notification on completion.
- Windows `Aligned.Background` — headless console exe; refreshes news + lists, logs new brief. (Plug into Windows Task Scheduler.)

## Notifications

- 6 channels: breaking, topics, brief, research, chat, system.
- **Topic-pin store** drives per-category filtering: `db.topic_pins`.
- Daily-brief and Research-ready notifications wired and routed to their respective channels.
- Notification → app deeplink (`aligned://story/{id}`) registered.

## Icon catalogue (35 morphing icons)

Original 26 + redesigned `bookmark`, `moon`, `mic`, `flame`, `search`, `globe` for better recognizability.
Added: `heart`, `retweet`, `reply`, `eye`, `calendar`, `clock`, `pin`, `bell`, `history`.
Each is exactly 3 lines in a 14×14 viewBox; rotation-group fast-path preserved (arrow, chevron, cross).

## i18n

- `values/strings.xml` (English) — all UI strings.
- `values-fa/strings.xml` (Farsi) — added as RTL test case.
- AndroidManifest already declares `android:supportsRtl="true"`.
- Adding `values-{lang}/strings.xml` for zh / es / ar is a translation drop-in.

## Hard guards still in place

- No `POST /api/settings` method exists in either client.
- No `POST /api/refresh` method exists in either client.

## Known caveats

- **MVVMTK0045 warnings** on every `[ObservableProperty]` field — code compiles & runs; partial-property migration is a follow-up.
- **Aligned.Background** is a plain console exe — toast is logged to stdout. The main app handles real notifications; the background poller exists so a Task Scheduler entry can keep the cache warm.
- **Map** renders a card list (not tile-based). Real tile rendering deferred — neither platform has a chosen tile lib yet.
- **Topic pinning** — data layer + notification respect are live; a "+pin" toggle UI per category chip is not yet exposed (you'd write pins via `repo.togglePinnedTopic("ai-safety")` from a debug menu today).
