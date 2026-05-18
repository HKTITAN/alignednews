# ALIGNED Design System

All visual decisions resolve to a token in `tokens.json`. If something in code references a literal color, font, or duration that isn't in tokens.json, it's a bug.

## Sources

1. **Apple aesthetic** — SF Pro, monochrome surfaces, generous whitespace, no gratuitous shadows. Color earns its place by carrying meaning (category, accent, destructive). The rest is grayscale.
2. **benji.org morphing icons** — Every glyph is exactly three SVG lines in a 14×14 viewBox. Unused lines collapse to `(7, 7)` with `opacity: 0`. Icons that are the same shape at different rotations belong to a **rotation group** and animate via rotation, not coordinate morphing.
3. **alignednews.ai brand** — Vivid color is reserved for category chips; the palette comes from the live `/api/categories` response, mirrored in `tokens.color.category`.

## Surfaces

| | Light | Dark |
|---|---|---|
| Background | `#FFFFFF` | `#000000` |
| Surface (chrome, sheets) | `#F5F5F7` | `#1C1C1E` |
| Elev 1 (cards on surface) | `#FFFFFF` | `#2C2C2E` |
| Separator | `#D2D2D7` | `#38383A` |

Cards rest on `surface`. The card itself is `elev1`. We use **hairline 0.5 px separator** for hierarchy, never a drop shadow on cards. Sheets and popovers do get shadow.

## Typography

iOS: SF Pro Display (≥20 pt) + SF Pro Text (<20 pt).
Android substitution: **Inter** at all sizes. Inter's metrics are close enough that the type scale in tokens still hits the same visual rhythm; we tighten tracking by an extra –0.1 to match SF's compactness.
Windows substitution: **Segoe UI Variable Display / Text**. This is the native system font on Windows 11 and is metrically very close to SF Pro.

Never use a system fallback for body text — bundle the chosen font in the app package.

## Motion

Two springs cover 90 % of cases:
- `spring`: stiffness 380, damping 30 — default for icon morphs, sheet enter, card press release.
- `springSnappy`: stiffness 600, damping 36 — for chips selecting, segmented control flip.

Tweens only for chrome (status bar tint, scrim fade). Never crossfade content.

## Layout

| Breakpoint | Columns | Notes |
|---|---|---|
| <600 dp / <600 epx | 1 | Phone, narrow window |
| 600–1280 | 2 | List + detail |
| ≥1280 | 3 | Group rail + list + detail (Windows only typically) |

Minimum tap target 44 dp / 44 epx, always.

## Morphing icon system

Every icon is built from this primitive (pseudocode):

```
type Line = { x1: Float, y1: Float, x2: Float, y2: Float, opacity: 0|1 }
type Icon = { lines: [Line, Line, Line], rotationGroup?: String, rotationDeg?: Int }
```

When morphing icon A → icon B:

1. If both are in the same `rotationGroup`, animate the parent rotation, leave lines static.
2. Otherwise, animate each of the three line endpoints from A's coordinates to B's. Lines with `opacity: 0` interpolate their coordinates *while invisible* so they're in the right place at the destination.

A collapsed line is `{ x1: 7, y1: 7, x2: 7, y2: 7, opacity: 0 }` — a zero-length segment at the icon center.

## Icon catalogue (26)

Source SVGs in `design/icons/*.svg`. Each is a minimal `<svg viewBox="0 0 14 14">` with up to three `<line>` elements. The Kotlin (`MorphingIconLibrary.kt`) and C# (`MorphingIconLibrary.cs`) catalogues are generated from these so both platforms stay in sync.

| Icon | Rotation group |
|---|---|
| `arrow-up`, `arrow-down`, `arrow-left`, `arrow-right` | `arrow` |
| `chevron-up`, `chevron-down`, `chevron-left`, `chevron-right` | `chevron` |
| `plus`, `close` | `cross` (`close` = `plus` rotated 45°) |
| `menu`, `check`, `search`, `settings`, `share`, `bookmark`, `play`, `pause`, `sun`, `moon`, `refresh`, `sparkle`, `mic`, `send`, `flame`, `globe` | — |

## Accessibility

- Dynamic type: respect OS-level text scale. Caps at 200 % for headlines.
- Reduce-motion: substitute `springSoft` for `spring` everywhere, and replace cross-group icon morphs with a 120 ms opacity crossfade. Within rotation groups, keep the rotation but slow it 2×.
- Minimum contrast 4.5:1 for body text against background.
- RTL: full mirror for `fa` and `ar`. Icons in `arrow`/`chevron` rotation groups auto-flip their default rotation by 180°.
