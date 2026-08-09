# Arrow Puzzle — Kotlin + Jetpack Compose

Scaffold build. Navigation, screen shells, design system and motion are complete;
board logic is deliberately not implemented yet. Anything unbuilt routes to a
designed **Coming Soon** state rather than a blank or broken screen.

---

## Opening the project

1. Android Studio → **Open** → select this folder.
2. There is no `gradle-wrapper.jar` in the archive (it is a binary Gradle ships).
   Android Studio will offer to generate the wrapper on first sync — accept it.
   From a terminal instead: `gradle wrapper --gradle-version 8.9`
3. Sync, then run on a device or emulator with **API 26+**.

`minSdk` is 26 so `java.time` works without core-library desugaring. If you need
24, set `isCoreLibraryDesugaringEnabled = true` in `app/build.gradle.kts` and add
`coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.3")`.

Versions are pinned in `gradle/libs.versions.toml` — AGP 8.7.3, Kotlin 2.0.21,
Compose BOM 2024.12.01. Bump them together, not individually.

---

## Structure

```
core/
  design/    Color, Type, Theme       — palette, type scale, spacing, reduced-motion
  motion/    Motion, Modifiers        — one animation vocabulary for the whole app
  ui/        Components, ArrowBackdrop, ComingSoon
  data/      AppPreferences           — DataStore-backed settings + AppViewModel
navigation/  Destinations, ArrowNavHost, BottomBar
feature/     splash, consent, home, daily, me, settings, game
```

### Design system

Every colour is sampled from the reference screens and lives in `Palette`,
provided through `LocalPalette`. No screen hard-codes a hex value, so a dark
theme is one swap rather than a find-and-replace. Same for `Spacing` — every gap
in the app is a token, never a loose number.

The type scale uses the platform face so the project builds with zero asset
downloads. Drop a variable font into `res/font/` and change `AppFontFamily` in
`Type.kt` to switch the whole app over in one line.

### Motion

`Motion` holds named easings, four durations and three spring flavours
(`snappy` for taps, `bouncy` for cards, `playful` for celebrations). Screens pick
an intent — they never write a raw `tween(300)`. Curves are asymmetric on
purpose: things enter with slight overshoot and leave on a fast ease-in, which is
what reads as responsive rather than merely animated.

`Motion.respecting()` collapses any spec to a single frame when the OS animator
scale is zero, and `LocalReducedMotion` is honoured by every animated component.

### Performance

Every animated value is read inside a `graphicsLayer { }` or `drawWithCache { }`
lambda. Reading animation state in those blocks skips recomposition and layout
entirely and only re-runs the draw pass — that is the difference between "has
animations" and "holds 120 Hz while four things move at once."

Other choices in the same spirit: the arrow backdrop builds its paths once per
size change and drives all four shapes from a single infinite transition; list
entrances animate a layer rather than wrapping items in `AnimatedVisibility`, so
each item is measured once; Compose compiler metrics are enabled in
`app/build.gradle.kts` and land in `app/build/compose_reports` so unstable
classes get caught before they cost frames.

---

## What each screen does

| Screen | State |
| --- | --- |
| Splash | Complete — mark springs in over the drifting arrow field, crossfades out |
| Consent | Complete — three annotated links, one action |
| Home | Complete shell — daily + tournament cards, wordmark, New Game |
| Daily | Complete shell — hero header, month grid, two-page intro dialog |
| Me | Complete — all rows navigate |
| Settings | **Functional** — sound/music/haptics persist via DataStore |
| Game | Board renders; taps nudge and give feedback. No rules engine |
| Awards, Achievements, Help, About, Privacy ×2, Remove Ads, Tournament | Coming Soon |

---

## Research notes that shaped this

The mechanic is consistent across every version on the Play Store: each arrow has
a fixed direction, tapping one works only when its path to the board edge is
clear, and a wrong tap costs a life. Clear the board to finish the level.

The differentiation opportunity is in the complaints, not the mechanics. The
recurring one-star themes are **mis-taps on crowded boards**, **ad frequency**,
**repeated puzzles**, and **difficulty swings**. Two of those are already
addressed structurally here:

- Every board cell keeps a 48 dp touch target even where the drawn glyph is
  smaller (`ArrowTile` pads inward rather than shrinking the hit area).
- Taps produce motion on the same frame instead of waiting on a rules check, so
  input never feels dropped. When the engine lands, the nudge becomes either a
  slide-off or a refusal — the feedback loop does not change.

The other two are content problems: they need a generator with a guaranteed
unique-solution check and a difficulty curve driven by measured solve time.

---

## Next steps, in the order I'd take them

1. **Rules engine** — `Board`, `Move`, `canExit(piece)`, immutable state + undo
   stack. Pure Kotlin, unit-testable, no Compose dependency.
2. **Level generator** — generate by playing a solved board backwards so every
   level is solvable by construction; verify uniqueness; bucket by solve depth.
3. **Slide-off animation** — the arrow travels its full lane to the edge and
   fades; failures shake in place and drop a heart.
4. **Progress persistence** — its own DataStore, separate from settings.
5. **Sound and haptics** — wire the toggles that already persist to a real
   `SoundPool`.
6. **Baseline Profile** — measurable cold-start and scroll-jank win, worth doing
   before the first release rather than after.
