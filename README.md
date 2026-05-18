# ALIGNED

Native Android and Windows clients for [alignednews.ai](https://alignednews.ai), plus a full reverse-engineered reference for its API surface.

The companion docs in [`docs/`](./docs) reverse-engineer the public API, prompt engineering, and data stores of alignednews.ai — useful if you want to build your own client, integrate the feed, or just understand how the service is wired.

## What's here

```
.
├── README.md, PLAN.md, STATUS.md   — what's done, what's planned
├── docs/
│   ├── API.md                       — every documented endpoint
│   ├── PROMPTS_AND_STORES.md        — system prompts + data-store fingerprints
│   ├── morphing-icons-reference.md  — design DNA from benji.org
│   └── samples/                     — captured sample payloads
├── design/
│   ├── tokens.json                  — single source of truth for color/type/motion
│   ├── ALIGNED_DESIGN.md            — design system writeup
│   └── icons/                       — 26 source SVGs (3-line morphing system)
├── shared-kmp/                      — Kotlin Multiplatform module (Android + future iOS)
│   └── src/commonMain/kotlin/ai/aligned/
│       ├── net/AlignedApi.kt        — every endpoint, typed
│       ├── net/dto/Dtos.kt
│       └── sync/SyncEngine.kt
├── android-app/                     — Jetpack Compose Android app
├── windows-app/                     — WinUI 3 / .NET 10 Windows app
│   ├── Aligned.sln
│   ├── Aligned.Core/                — netstandard API client (mirror of :shared-kmp)
│   ├── Aligned.App/                 — XAML shell
│   ├── Aligned.Background/          — background sync exe
│   └── Aligned.Core.SmokeTest/      — runnable live-API integration test
└── .github/workflows/release.yml    — CI builds .exe + .apk on tag push
```

## Quick start

### Windows

Requires .NET 10 SDK + Visual Studio 2022 (or `dotnet` CLI with WinUI workload).

```powershell
cd windows-app/Aligned.App
dotnet build -c Release
dotnet publish -c Release -r win-x64 -p:WindowsAppSDKSelfContained=true
```

The smoke test against the live API needs only `dotnet`:

```powershell
cd windows-app/Aligned.Core.SmokeTest
dotnet run -c Release
```

### Android

Requires JDK 21, Android SDK 35, Gradle 8.10+.

```powershell
cd .
gradle wrapper --gradle-version 8.10.2     # first time
./gradlew assembleDebug
./gradlew installDebug                     # to a connected device or emulator
```

## Architecture

- **No global mutation.** The backend's `POST /api/settings` is unauthenticated and globally persistent — writing it changes every visitor's experience. Both clients treat `/api/settings` as read-only. (`docs/API.md §5`.)
- **All notifications are client-synthesized.** The backend has no push channel. Notifications come from diffing successive sync results.
- **One source of truth per concern.** Design tokens in `design/tokens.json`. The API DTO schema is duplicated between Kotlin (shared-kmp) and C# (Aligned.Core) — keep them in sync manually.
- **Morphing icons.** Every glyph is exactly 3 SVG lines in a 14×14 viewBox. Icons in a rotation group animate via rotation; cross-group transitions interpolate line endpoints. Implementation in `MorphingIcon.kt` (Compose) and `MorphingIcon.cs` (WinUI Composition API).

See [`PLAN.md`](./PLAN.md) for the milestone roadmap M0 → M11, and [`STATUS.md`](./STATUS.md) for what's complete now.

## License

MIT — see [LICENSE](./LICENSE).

This is an independent client + reference. Not affiliated with alignednews.ai.
