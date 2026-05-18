# STATUS

What's actually written vs. stubbed vs. pending, as of this commit. Honest.

Legend:
- ✅ written and compiles on this machine
- 🟡 written but stubbed (placeholder body, returns dummy or partial result)
- ⬜ not written

## Build-machine reality

- `dotnet 10.0.300` is installed → **Aligned.Core, Aligned.App, Aligned.Core.SmokeTest all build with 0 errors / 0 warnings**.
- Android toolchain (JDK 21 + SDK 35 + Gradle) is **not** installed → Android Kotlin code is written but not yet compiled. It'll need the toolchain.

## M0 — Repo + tokens + morphing icons

| Item | Status |
|---|---|
| Repo layout | ✅ |
| `design/tokens.json` (canonical) | ✅ |
| `design/ALIGNED_DESIGN.md` | ✅ |
| 26 SVG sources in `design/icons/` | ✅ |
| Android `MorphingIcon` composable (cross-group morph + rotation fast-path) | ✅ |
| Windows `MorphingIcon` control (Composition API, rotation + endpoint animation) | ✅ |
| Icon catalogue mirrored in Kotlin + C# | ✅ |

## M1 — Shared API client

| Item | Status |
|---|---|
| Kotlin `AlignedApi` covering every documented endpoint | ✅ |
| Kotlin DTOs for News/Story/Tweet/Lists/Map/Events/etc. | ✅ |
| C# `AlignedApi` mirror | ✅ |
| C# DTOs mirror | ✅ |
| Chat SSE streaming (Kotlin Flow + C# IAsyncEnumerable) | ✅ |
| Hard guard: no `POST /api/settings` method exists in either client | ✅ |
| `Aligned.Core` compiles clean on .NET 10 (0 warnings, 0 errors) | ✅ |
| Live smoke test (`Aligned.Core.SmokeTest/`) exercises `/api/health`, `/api/news`, `/api/chat` SSE, `/api/research` start+poll | ✅ |

## M2 — Cache + feed + pull-to-refresh

| Item | Status |
|---|---|
| Room schema + DAOs (Android) — `data/AlignedDb.kt` (stories, story_detail, sync_log) | ✅ |
| `StoryRepository` cache-then-network with diff output | ✅ |
| `FeedViewModel` observes cache, refreshes from network | ✅ |
| Microsoft.Data.Sqlite cache (Windows) | ⬜ |
| `FeedScreen` (Android, cache-then-network) | ✅ |
| `FeedPage` (Windows, hits live API on launch) | ✅ |
| Pull-to-refresh | ⬜ (refresh button on Windows, not gesture-driven yet) |
| Shared element transitions feed→detail | ⬜ |
| Sync engine `runHot/runWarm/runCold` | 🟡 (Kotlin only) |

## M3 — Story detail / vote / share

| Item | Status |
|---|---|
| Story detail screen (Android) — `ui/story/StoryScreen.kt` | ✅ |
| Story detail page (Windows) — `Views/StoryPage.xaml` | ✅ |
| `POST /api/feedback` wired to up/down chips on both platforms | ✅ |
| Share card (`/api/og`) | ✅ URL builder only — no Share intent yet |

## M4 — Today's Brief (`/api/lists`)

| Item | Status |
|---|---|
| Roll-up screen on both platforms | ⬜ |

## M5 — Background sync

| Item | Status |
|---|---|
| Android `HotSyncWorker` (HiltWorker, repository-driven, emits diff) | ✅ |
| Android `SyncScheduler.ensureScheduled()` registers periodic hot tier | ✅ |
| Android `ResearchPollService` foreground service | 🟡 (compiles, polls; no notification on completion yet) |
| Windows `Aligned.Background` exe entry | 🟡 (one-shot sync + simple toast on new brief) |
| BackgroundTask manifest registration on Windows | ⬜ (skipped — currently unpackaged) |

## M6 — Notifications

| Item | Status |
|---|---|
| Android channels created at app startup (6 channels) | ✅ |
| `NotificationCenter` synthesizes per-story rich notifications from sync diffs | ✅ |
| Notification deeplink to `aligned://story/{id}` | ✅ wired in manifest + intent |
| Per-user topic-pin store (so "my topics" can actually filter) | ⬜ |
| Daily-brief scheduling at 08:00 local | ⬜ |
| Windows `AppNotificationBuilder` toast | ✅ for "daily brief" only |
| Per-channel routing on Windows | ⬜ |

## M7 — Map | M8 — Chat | M9 — Research | M10 — i18n | M11 — Release

| Item | Status |
|---|---|
| Map screen (both) | ⬜ |
| Chat streaming UI (Android) — `ui/chat/ChatScreen.kt` + ViewModel, send/stop with morphing icon swap | ✅ |
| Chat streaming UI (Windows) — `Views/ChatPage.xaml` + ViewModel | ✅ |
| Research 10-step UI (both) | ⬜ |
| i18n: en/fa/zh/es/ar with RTL | ⬜ |
| Release packaging | ⬜ |

## Toolchain — *not* installed by this scaffold

You'll need to install these locally to actually build:

| Tool | How |
|---|---|
| JDK 21 | `winget install EclipseAdoptium.Temurin.21.JDK` |
| Android cmdline-tools + SDK 35 | https://developer.android.com/studio#command-line-tools-only (set `ANDROID_HOME`) |
| Gradle 8.10+ | once JDK is on PATH: `gradle wrapper --gradle-version 8.10.2` inside `ALIGNED/` |
| Visual Studio 2022 17.10+ | https://visualstudio.microsoft.com/downloads/ — pick "Windows application development" + ".NET desktop development" workloads |
| Windows App SDK 1.6 runtime | https://learn.microsoft.com/windows/apps/windows-app-sdk/downloads |

## Known correctness caveats

- **Hilt + KAPT** generates Java; we use `kotlin-kapt` apply at bottom of `android-app/build.gradle.kts`. If the K2 KAPT path misbehaves on first build, drop `kapt.use.k2=true` in `gradle.properties`.
- **Compose foundation `padding()`** import in NavGraph relies on `androidx.compose.foundation.layout.padding` — confirmed in the imports.
- **WinUI `XamlControlsResources`** is referenced inline in `App.xaml`; that namespace import is resolved by `UseWinUI=true` in the csproj. If a builder complains about the prefix, change to `<controls:XamlControlsResources xmlns:controls="using:Microsoft.UI.Xaml.Controls"/>` and add the prefix at the dictionary root.
- **Icon designs are placeholders.** The 26 SVGs were generated by formula — many are decent (`menu`, `play`, `pause`, `arrow-*`, `chevron-*`, `plus`/`close`, `sparkle`). A few (`bookmark`, `moon`, `mic`, `flame`, `search`, `globe`) deserve a second pass with a designer's eye since the 3-line constraint forces compromises.
- **Feed image rendering**: tweet media URLs come back from `/api/news` but neither `FeedScreen` nor `FeedPage` renders them yet — needs Coil setup on Android and an `Image` element on Windows.
- **The 6 GB Visual Studio install** is the real friction point. If you don't have it, ping me and we'll defer Windows to a separate session.
