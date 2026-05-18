# ALIGNED — Build Plan

Native apps for `alignednews.ai`. Android (Kotlin Multiplatform, Compose) and Windows (WinUI 3 / Windows App SDK, C# .NET 9). Full feature parity with the web product. Always-on background sync, push notifications, Apple-style polish, benji.org-style morphing icons.

This document is the blueprint we lock in *before* writing any code.

---

## 1. Scope (full parity)

Surfaces the apps must cover, mapped to the backend API we already documented:

| Surface | Endpoints | Notes |
|---|---|---|
| Feed | `GET /api/news` (+ `?category`, `?limit`) | Primary entry point |
| Story detail | `GET /api/news/{id}`, `POST /api/feedback`, `GET /api/infographic`, `GET /api/og` | Vote, share card |
| Editorial roll-up | `GET /api/lists` (+ `?group`, `?date`) | "Today's brief" + per-group |
| Categories / groups | `GET /api/categories` | Filter chips |
| Map | `GET /api/map` | Pinch-zoom world map of story markers |
| Events | `GET /api/events` | Conference / launch calendar |
| Search | `GET /api/search?q=` | Debounced live search |
| Chat | `POST /api/chat` (SSE) | Streaming token UI |
| Research | `POST /api/research` then `GET /api/research?id=` | 10-step progress UI, insight cards, exec summary |
| Settings | `GET /api/settings` (read only — never POST to global) | Mirror the global config locally |
| Subscribe / digest | `POST` to subscribe (route TBD) | Not yet found; keep stub |
| History | `GET /api/history` | Recent research + chat sessions list |
| RSS / share | `GET /api/feed?format=json` | Used internally for share intents |
| Languages | `en, fa, zh, es, ar` | RTL for fa & ar |

**Explicit non-goals for v1:** account/login (the backend has no auth), inline tweet composition, paid features, offline-first writes.

**Hard rule:** we never `POST /api/settings` from the apps. Settings are global and unauthenticated; writing them would change every user's site experience. We mirror the global config read-only and store user preferences locally.

---

## 2. Top-level architecture

```
ALIGNED/
├── shared-kmp/                      # Kotlin Multiplatform — for Android (+ iOS later)
│   └── src/
│       ├── commonMain/              # API client, models, sync engine, business logic
│       ├── androidMain/             # Android-specific (WorkManager, NotificationManager hooks)
│       └── iosMain/                 # (stubbed for future iOS port)
├── android-app/                     # Kotlin + Jetpack Compose, depends on :shared-kmp
│   └── src/main/...
├── windows-app/                     # C# .NET 9, WinUI 3 / Windows App SDK
│   └── Aligned.Windows/
│       ├── Aligned.Core/            # API client + models + sync (mirror of :shared-kmp)
│       ├── Aligned.App/             # XAML views, ViewModels, navigation
│       └── Aligned.Background/      # BackgroundTask out-of-proc COM server
├── design/
│   ├── tokens.json                  # Shared color / spacing / motion tokens (consumed by both)
│   ├── icons/                       # Source SVGs for the 3-line morphing icon system
│   └── ALIGNED_DESIGN.md
├── PLAN.md                          # This file
└── docs/                            # Generated API + architecture docs
```

The two apps **do not share UI code** — Android uses Compose, Windows uses XAML. They share *concepts* (the data model, the icon system, the design tokens) which are duplicated in each codebase from `design/tokens.json`. KMP is used for Android-iOS sharing, not Android-Windows.

---

## 3. Backend — what we depend on, and gotchas

From the reverse-engineered API doc:

- **Base URL**: `https://alignednews.ai` (production). Both apps allow override (`https://x-news-stream-robert-production.up.railway.app`, or a local proxy).
- **No auth.** Calls are anonymous. No token storage on the client.
- **Cache aggressively.** `/api/news` returns byte-identical bytes on repeat calls; do conditional fetch with our own hash. The server doesn't set `ETag`.
- **Heavy payloads.** `/api/lists` is ~400 KB. Stream-parse it; don't load the whole tree into ViewModel.
- **`?date=` is broken on `/api/news`** — only `/api/lists?date=` is functional. Surface this honestly in the UI ("Past days" is read-only and only available under Today's Brief).
- **Research is slow** (~80 s end-to-end). The app must keep polling in the background even if the user navigates away.
- **Chat is SSE.** Each token arrives as `data: {"type":"token","text":"…"}` and the stream ends with `{"type":"done"}`. We render incrementally.

---

## 4. Background sync

Each platform uses its native job scheduler. Same logic, two implementations.

### 4.1 Sync cadence

| Tier | Cadence | Trigger | Endpoints fetched |
|---|---|---|---|
| **Hot** | every 5 min | foreground or user-pinned topic active | `/api/news`, `/api/health` |
| **Warm** | every 30 min | always | `/api/lists` (overview only), `/api/events`, `/api/map` |
| **Cold** | every 6 h | always | `/api/categories`, `/api/accounts`, `/api/settings`, `/api/history` |
| **One-shot** | on demand | story open | `/api/news/{id}`, `/api/infographic?id=…` |

Hot is power-aware: on cell data + battery <20 %, drops to every 15 min. On Wi-Fi + charging, can go to 2 min.

### 4.2 Android — WorkManager

- `PeriodicWorkRequest` with `Constraints(NetworkType.CONNECTED)` for hot, warm, cold tiers.
- A separate `OneTimeWorkRequest` chain for "fetch on app launch."
- Coalesced into a single `SyncWorker` that reads the tier from input data and writes a `SyncResult` row to a local Room DB.
- Foreground service only for the long-running `/api/research` polling (Android 14+ requires `dataSync` foreground service type).

### 4.3 Windows — BackgroundTasks

- Out-of-proc COM server registered via package manifest with `Application > Extensions > BackgroundTasks`.
- `TimeTrigger(15, false)` for hot (15 min is the OS minimum), longer for warm/cold using `ApplicationTrigger` chained from a single ticker.
- Persists last-sync timestamps in `ApplicationData.Current.LocalSettings`.
- For research polling, we use the same out-of-proc task with a `MaintenanceTrigger`, plus an in-app `DispatcherTimer` while the window is open.

### 4.4 Cache layer (both platforms)

- Local SQLite (Room on Android, `Microsoft.Data.Sqlite` on Windows).
- Tables: `stories`, `story_detail`, `lists_snapshot`, `map_markers`, `events`, `categories`, `accounts`, `history`, `research_sessions`, `sync_log`.
- Every row has `fetched_at` and `payload_hash`. Conditional save: skip the write if `payload_hash` is unchanged.
- TTLs match the sync tiers above. Reads always hit the cache first; UI shows a "stale by Nm" chip if the data is past its TTL.

---

## 5. Notifications

This is the highest-stakes UX choice. Notifications can either be loved or hated. We'll be conservative by default and let users opt in to more.

### 5.1 Notification taxonomy

| Channel | Trigger | Default state |
|---|---|---|
| **Breaking** | A story enters the top-3 by `totalEngagement` and didn't exist in the last sync | Off; opt-in only |
| **My topics** | A new story matches one of the user's pinned categories or groups | On |
| **Daily brief** | When `/api/lists.overview.timestamp` advances and the day is new | On (08:00 local) |
| **Research ready** | A user-started research session finishes | On |
| **Chat reply** | (Future) async chat reply | Off |
| **System** | Sync errors, rate limits, backend health changes | Off (visible in app only) |

Per Apple HIG / Material guidance, each channel maps to a separate OS notification category so users can mute one without losing others.

### 5.2 De-duplication

The backend has no notification push system, so we synthesize notifications from sync diffs. Each `SyncWorker` cycle computes the set of new `storyId`s vs. the last cycle and emits at most:

- 1 "daily brief" per local day
- 3 "breaking" per hour
- 5 "my topics" per hour (collapsible group)

All thresholds are user-tunable in Settings.

### 5.3 Android

- `NotificationChannel` per channel above, with `IMPORTANCE_DEFAULT` for "my topics" / "daily brief" and `IMPORTANCE_HIGH` only for "breaking" / "research ready".
- Rich notifications: big-picture (story media), `MessagingStyle` for chat, progress-bar updates for research.
- Tap → `MainActivity` with deeplink intent `aligned://story/{id}` or `aligned://research/{id}`.

### 5.4 Windows

- `AppNotificationManager` (Windows App SDK toast API, not the legacy UWP one).
- Adaptive toast XML with hero image for stories.
- `Aligned.Background` raises notifications; activation passes a launch arg `--story={id}` parsed in `App.OnLaunched`.
- Action Center grouping by channel.

---

## 6. Design system

Three sources combined:

1. **Apple aesthetic** (per the user-provided getdesign.md/apple reference): SF Pro typography, generous white space, cinematic imagery, monochrome luxury, minimal chrome.
2. **benji.org / morphing-icons**: every glyph in the icon set is **three SVG path lines**. Icons that need fewer lines collapse the extras to invisible center points. Transitions either *rotate* (same-shape icons in a rotation group) or *coordinate-morph* (cross-group). Library: Motion on web → Compose `animateFloatAsState` + custom `Path` morpher on Android, `Microsoft.UI.Composition` + `PathKeyFrameAnimation` on Windows.
3. **The alignednews.ai brand**: we keep the existing category colors from `/api/categories` (e.g. `ai-companies #6366F1`, `ai-safety #F87171`, …). Those are the only colors that get to be vivid; everything else is monochrome.

### 6.1 Tokens (in `design/tokens.json`)

```jsonc
{
  "color": {
    "bg":          { "light": "#FFFFFF", "dark": "#000000" },
    "surface":     { "light": "#F5F5F7", "dark": "#1C1C1E" },
    "elev1":       { "light": "#FFFFFF", "dark": "#2C2C2E" },
    "text":        { "light": "#1D1D1F", "dark": "#F5F5F7" },
    "textSecondary":{ "light": "#6E6E73", "dark": "#98989D" },
    "separator":   { "light": "#D2D2D7", "dark": "#38383A" },
    "accent":      "#0A84FF",
    "category":    "<from /api/categories>"
  },
  "type": {
    "displayLg":   { "font": "SF Pro Display",   "size": 34, "weight": 700, "tracking": -0.02 },
    "title":       { "font": "SF Pro Display",   "size": 22, "weight": 600, "tracking": -0.01 },
    "body":        { "font": "SF Pro Text",      "size": 17, "weight": 400 },
    "callout":     { "font": "SF Pro Text",      "size": 16, "weight": 400 },
    "footnote":    { "font": "SF Pro Text",      "size": 13, "weight": 400 },
    "mono":        { "font": "SF Mono",          "size": 13, "weight": 400 }
  },
  "space": [ 0, 4, 8, 12, 16, 20, 24, 32, 40, 48, 64, 80 ],
  "radius": { "card": 16, "chip": 100, "tap": 12 },
  "motion": {
    "spring":   { "stiffness": 380, "damping": 30 },
    "easeOut":  "cubic-bezier(0.2, 0.8, 0.2, 1)",
    "icon":     { "duration": 220, "curve": "spring" }
  }
}
```

SF Pro isn't licensed for Windows, so on Windows we substitute **Segoe UI Variable Display / Text** which is its visual sibling. On Android we ship **Inter** as the SF Pro stand-in (closest free font; tracking/weights tuned per token to match SF's metrics).

### 6.2 Morphing icon set (3-line, per benji)

The 21 icons benji built are a starting library. We need ours to cover: `menu, close, plus, check, arrow-{up,down,left,right}, chevron-{up,down,left,right}, search, settings, share, bookmark, play, pause, sun, moon, refresh, sparkle, mic, send`. That's 24 — we add `flame` (breaking) and `globe` (map) for 26. All drawn as 3 lines in a 14×14 viewBox with `(7, 7)` as the collapse point. Rotation groups: arrows (90° quartet), chevrons (90° quartet), plus/cross (45° pair).

### 6.3 Motion principles

- **No crossfades.** Ever. Either rotate, slide, or coordinate-morph.
- **Springs > tweens** for any user-initiated change. Tweens only for chrome (status bars).
- **Shared element transitions** between feed → story detail (the headline scales and re-anchors). Android: `androidx.compose.animation.SharedTransitionLayout`. Windows: `ConnectedAnimationService`.
- **Story-card tap = haptic light + 96 ms scale-down to 0.97.** Both platforms.

### 6.4 Layout primitives

- **One column** on phone / narrow window.
- **Two column** at ≥720 dp / ≥720 epx (list + detail).
- **Three column** at ≥1280 epx on Windows (groups rail + list + detail).
- Detail pane never collapses below 480 epx; rather than shrink, we drop a column.

---

## 7. Android app — concrete plan

### 7.1 Tech

- Kotlin 2.1.x, JDK 21
- Compose Multiplatform 1.7.x (via the `:shared-kmp` module's UI shouldn't run on Windows; Compose is Android-only in this project despite using CMP gradle infra)
- Ktor 3 (client + OkHttp engine) — the shared HTTP client
- kotlinx.serialization
- AndroidX: WorkManager, DataStore, Lifecycle, Navigation Compose 2.8, Room 2.7
- Coil 2.7 for images
- moko-resources for i18n (handles RTL out of the box)
- Coroutines, Flow

### 7.2 Module layout

```
android-app/src/main/java/ai/aligned/
├── App.kt
├── MainActivity.kt
├── nav/                   # NavHost + deeplink graph
├── ui/
│   ├── feed/              # FeedScreen, FeedViewModel
│   ├── story/             # StoryDetailScreen, ShareCard
│   ├── lists/             # Today's Brief
│   ├── map/               # MapScreen (Maplibre-android or OSM)
│   ├── search/
│   ├── chat/              # SSE streaming UI
│   ├── research/          # 10-step progress, insight cards
│   ├── settings/
│   ├── icons/             # MorphingIcon composable + IconSpec data class
│   └── theme/             # AlignedTheme, Tokens, Typography, Colors
├── work/
│   ├── HotSyncWorker.kt
│   ├── WarmSyncWorker.kt
│   ├── ColdSyncWorker.kt
│   ├── ResearchPollService.kt  # ForegroundService
│   └── NotificationCenter.kt
└── di/                    # Hilt (or Koin)
```

`:shared-kmp` exposes:

```
commonMain/
├── net/
│   ├── AlignedApi.kt            # Ktor client + every endpoint
│   ├── dto/                     # @Serializable data classes
│   └── sse/SseClient.kt
├── domain/
│   ├── Story.kt, Cluster.kt, Tweet.kt, Author.kt, Insight.kt, ...
│   └── repository/              # Cache-then-network strategy
├── sync/
│   └── SyncEngine.kt            # Pure-Kotlin tier scheduler; platform plugs in actual timers
└── settings/SettingsStore.kt    # Read-only mirror of /api/settings + local user prefs
```

### 7.3 Notable Compose details

- `MorphingIcon` composable accepts an `IconSpec` (target icon id) and animates each of the three `Line` primitives. Internal state holds `previousSpec`, transitions using `animateFloatAsState` per coordinate, with rotation-group fast-path.
- `SharedTransitionLayout` for feed→detail headline animation.
- `LazyColumn` virtualization for feed; sticky category chip header.
- Pull-to-refresh = `PullToRefreshContainer` from material3-pull-to-refresh, but the indicator is *our* morphing-icon refresh glyph.

### 7.4 Build / run

```
cd android-app
./gradlew installDebug
```

Local SDK requirements (we'll install these as step 0):

- Android Studio Hedgehog or Iguana (or just `cmdline-tools` + SDK manager)
- Android SDK Platform 35
- JDK 21 (Temurin)
- Kotlin Gradle Plugin 2.1.x

---

## 8. Windows app — concrete plan

### 8.1 Tech

- C# 13, .NET 9
- Windows App SDK 1.6+, WinUI 3
- CommunityToolkit.Mvvm 8.x (RelayCommand, ObservableObject)
- Microsoft.UI.Xaml (XAML), Win2D for the map canvas
- System.Net.Http with `HttpClientFactory`, `System.Text.Json`
- Microsoft.Data.Sqlite for local cache
- Microsoft.Windows.AppNotifications for toasts
- Mapsui or Microsoft.Maps.MapControl.WPF (we'll pick during prototyping)

### 8.2 Solution layout

```
Aligned.sln
├── Aligned.Core/             # netstandard2.1 — API client, DTOs, sync engine
├── Aligned.App/              # WinUI 3 app, depends on Aligned.Core
│   ├── Views/                # XAML pages: FeedPage, StoryPage, ListsPage, MapPage, ChatPage, ResearchPage, SettingsPage
│   ├── ViewModels/
│   ├── Controls/MorphingIconControl.cs   # Custom Control using CompositionAPI
│   ├── Themes/Generic.xaml
│   └── Resources/Tokens.xaml
├── Aligned.Background/       # Out-of-proc background task (separate exe per appx convention)
└── Aligned.Tests/
```

### 8.3 Notable WinUI details

- `MorphingIconControl` is a custom `Control` with three `CompositionLineGeometry` children and a `Vector2KeyFrameAnimation` per endpoint. Rotation groups use a single `RotationAngleInDegrees` animation instead.
- `NavigationView` left rail at ≥1280 epx; tab bar bottom on narrow.
- `MapPage` uses Win2D with a custom marker layer (Mapsui has decent perf but Win2D direct gives us the same painterly look as web).
- Connected animation between feed list item and story detail page.
- Hot reload during dev (`DevHomePage` shows raw API responses for debug — gated behind `#if DEBUG`).

### 8.4 Build / run

```
cd windows-app/Aligned.sln
msbuild /restore /p:Configuration=Debug /p:Platform=x64
# or open in Visual Studio and F5
```

Local toolchain to install (step 0 for Windows):

- Visual Studio 2022 17.10+ with workloads:
  - ".NET Multi-platform App UI development" *or* ".NET desktop development"
  - "Windows application development"
- Windows App SDK 1.6+ runtime
- Windows 11 SDK 10.0.22621
- .NET 9 SDK

---

## 9. Step-0 environment setup (before any code)

These run on your Windows machine in order. None are auto — each is a 1-2 GB download with confirmation prompts, so I'll narrate and you click through.

| Step | Tool | Size | Required for |
|---|---|---|---|
| 1 | JDK 21 (Temurin via winget) | ~180 MB | Android Gradle |
| 2 | Android cmdline-tools + SDK 35 | ~1.5 GB | Android build |
| 3 | Android emulator + system image (optional) | ~1 GB | Running on device-free |
| 4 | Visual Studio 2022 with WinUI workload | ~6 GB | Windows build |
| 5 | Windows App SDK runtime | ~50 MB | Windows app |
| 6 | Git LFS (for icon sources) | ~10 MB | Repo |

We **don't** need to install Android Studio if we're scripting with `gradle` directly. Visual Studio is harder to skip on the Windows side.

---

## 10. Build sequence (proposed milestones)

Each milestone produces a runnable artifact on at least one platform.

| M | Deliverable | Touches |
|---|---|---|
| **M0** | Repo scaffolded, design tokens written, morphing-icon library prototyped on both platforms with the 26-icon set | `design/`, `android-app/ui/icons`, `windows-app/Aligned.App/Controls/MorphingIconControl` |
| **M1** | Shared API client (KMP for Android, mirrored in C# for Windows) — every endpoint typed + tested against live backend | `:shared-kmp/net`, `Aligned.Core` |
| **M2** | Local cache + sync engine (hot tier only). Feed screen on both platforms, pull-to-refresh, shared transitions | `:shared-kmp/sync`, both UI projects |
| **M3** | Story detail, share card (`/api/og`), feedback POST, category chips, search | both UI |
| **M4** | Today's Brief (`/api/lists`), groups, subcategories, per-group color theming | both UI |
| **M5** | Background sync wired to WorkManager + BackgroundTasks. All 4 tiers active. Sync log visible in Settings | `android-app/work`, `Aligned.Background` |
| **M6** | Notification system (channels, dedupe, deep links) — start with "daily brief" + "research ready", then "my topics" | both |
| **M7** | Map screen (story markers, group-color clustering) | both |
| **M8** | Chat with SSE streaming | both |
| **M9** | Research with 10-step progress UI, insight cards, exec summary, foreground service / background task for polling | both |
| **M10** | Settings, i18n (en/fa/zh/es/ar with RTL), accessibility pass, motion polish, dark/light parity | both |
| **M11** | Release: Play internal track build, MSIX sideload package, README, signing | both |

---

## 11. Quality bars

- **Cold launch <500 ms to first paint** on both platforms (mid-range Pixel 7a; Windows on a 2022-era laptop).
- **Sync battery cost <2 %/day** on default cadence (measured via Android `BatteryStatsManager`).
- **APK size <12 MB**, MSIX <30 MB.
- **Every animation 60 fps** on the target devices; jank check via Compose Layout Inspector & WinUI Performance Tools.
- **A11y**: TalkBack and Narrator pass all primary flows; minimum tap target 44 dp.
- **Localization**: every string in `strings.xml` / `Resources.resw`; RTL mirroring verified for fa & ar.

---

## 12. Risks & open questions

1. **Subscribe endpoint not found.** `/api/subscribe` 404'd. Until found, the "subscribe to digest" surface is stubbed.
2. **Voice / realtime endpoints not found.** The web copy mentions an "interactive voice chatbot" but its endpoint is hidden. v1 omits voice.
3. **Backend may rate-limit.** No rate-limit headers were surfaced in probing, but Railway edge will eventually throttle. We need client-side back-off (exponential, jittered) in the sync engine.
4. **Backend mutability is dangerous.** `POST /api/settings` is globally writable. We must guarantee we never call it (linter check on the API client).
5. **Push without server push.** All notifications are synthesized client-side from sync diffs. If the user kills the app, no notifications fire. Acceptable for v1; would need FCM/WNS pipeline for v2.
6. **Map tile source.** Need to pick & attribute (likely OpenStreetMap via MapLibre on Android, Win2D-rendered OSM tiles on Windows).
7. **Visual Studio installer is ~6 GB.** If you don't already have VS, that's a real hurdle. If you'd rather start with just Android (you have JDK already), we can defer the Windows toolchain until M2 ships.

---

## 13. What I'll do next (after you say go)

1. Create the empty repo layout above under `D:\1. Projects\4. aligned news\ALIGNED\`.
2. Write `design/tokens.json` and `design/ALIGNED_DESIGN.md` so both apps build off the same source of truth.
3. Build the 26-icon morphing library on both platforms in isolation (a tiny demo activity / window) before any networking — it's the hardest visual piece and de-risks everything else.
4. Then march through M1 → M11.

I will not start step 1 until you've reviewed this plan and either approved or pushed back on §2 (architecture), §5 (notification defaults), §6 (design tokens), or §10 (milestone ordering).
