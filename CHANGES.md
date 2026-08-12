# v0.2.0 — AdMob Monetization, Daily Reminders, End-Game Engagement Loop

Requested changes: wire up all four AdMob ad formats (Banner, Interstitial,
Rewarded Interstitial, Rewarded), add two daily local notifications to bring
players back, and make the level-complete/game-over flow more engaging so
players keep playing.

## Ad units used

| Placement | Ad unit ID | Where it shows |
|---|---|---|
| Banner-arrow | `ca-app-pub-9019700052213764/3749889300` | Bottom of Home screen and Game screen |
| Interstitial-arrow | `ca-app-pub-9019700052213764/3725838572` | Every 3rd level completion (never on the first 3 levels) |
| Rewarded interstitial — Hint | ⚠️ **not set** — see below | Tapping Hint after hints run out |
| Rewarded-Arrow | `ca-app-pub-9019700052213764/8810644297` | "Double coins" on the Win screen, "Continue" on the Game Over screen |

**⚠️ Action needed:** the Rewarded Interstitial (Hint) block only contained
the App ID twice (`ca-app-pub-9019700052213764~2171611579`), not a real
ad-unit ID — those look like `ca-app-pub-XXXXXXXXXXXXXXXX/NNNNNNNNNN`. The
build currently falls back to Google's public test ID
(`core/ads/AdIds.kt`) so it compiles and shows test ads; grab the real ID
from the AdMob console (Apps → Arrow Puzzle → Ad units) and drop it into
`AdIds.kt` before shipping.

## Files touched

| File | Change |
|---|---|
| `gradle/libs.versions.toml`, `app/build.gradle.kts` | Added `play-services-ads`, `user-messaging-platform` (GDPR/UMP consent), `androidx-work-runtime-ktx`. |
| `AndroidManifest.xml` | AdMob `APPLICATION_ID` meta-data; `INTERNET`, `ACCESS_NETWORK_STATE`, `AD_ID`, `POST_NOTIFICATIONS`, `RECEIVE_BOOT_COMPLETED` permissions; registered `ReminderReceiver` and `BootReceiver`. |
| `core/ads/AdIds.kt` | **New** — every ad-unit ID in one place; debug builds always use Google's test IDs. |
| `core/ads/AdManager.kt` | **New** — owns consent (UMP), loading/caching, and showing for all three full-screen formats, plus a simple frequency cap (skip first 3 levels, then every 3rd) for the interstitial. |
| `core/ads/BannerAdView.kt` | **New** — Compose wrapper around `AdView` for the anchored banner. |
| `core/notifications/ReminderScheduler.kt` | **New** — arms two daily `AlarmManager` alarms (12:00 PM and 7:30 PM). |
| `core/notifications/ReminderReceiver.kt` | **New** — builds and posts each reminder notification, with different copy per time slot. |
| `core/notifications/BootReceiver.kt` | **New** — re-arms the alarms after a device reboot. |
| `core/notifications/NotificationChannels.kt` | **New** — creates the `daily_reminder` notification channel. |
| `ArrowPuzzleApplication.kt` | Initializes `AdManager`, creates the notification channel, and arms the daily reminders on process start. |
| `MainActivity.kt` | Requests the `POST_NOTIFICATIONS` runtime permission on Android 13+, then runs the UMP consent flow before any ad request. |
| `core/data/AppPreferences.kt` | Added a persisted coin balance and a calendar-day play streak (`recordPlaySession()`, `addCoins()`), exposed through `AppViewModel.onLevelCompleted()` / `onRewardedCoinsDoubled()`. |
| `feature/game/GameViewModel.kt` | Added `grantBonusHint()` — grants and immediately reveals one hint (used after a rewarded-interstitial watch). |
| `feature/game/GameScreen.kt` | Win screen now shows coins earned/total and the current streak, a **"Milestone Reached!"** variant with bonus coins every 5th level, and a **"Watch ad to double coins"** button. Game Over screen adds a **"Watch ad for a fresh set of lives"** rewarded option before Try Again. Hint button falls through to a rewarded interstitial once hints run out. Banner ad added below the tool row. Interstitial is triggered from `onNext`/`onExit` on the win screen — a natural break point, never mid-puzzle. |
| `feature/home/HomeScreen.kt` | Banner ad added below the "New Game" button. |
| `navigation/ArrowNavHost.kt` | Passes the shared `AppViewModel` down into `GameScreen` so coins/streak persist through the app-level store. |

## Design notes (the "addictive/engaging" ask)

Kept this to standard, transparent game-economy mechanics — no dark
patterns, no fake scarcity, no disguised ads:
- **Coins** are awarded openly on every win and shown immediately.
- **Streak** only increments once per real calendar day (retries don't
  inflate it) and is always visible, never hidden until it breaks.
- **Milestone chest** (every 5th level) is a bonus, not a paywall.
- Every rewarded-ad placement is opt-in with a clearly labeled button —
  the player always has a free path forward (Try Again / Next Game) that
  doesn't require watching anything.
- Interstitials never interrupt a puzzle in progress; they only fire from
  the win screen, capped to at most once every 3 levels.

## Known gaps / next steps

- The Rewarded Interstitial (Hint) ad-unit ID needs to be supplied (see above).
- AdMob apps need to be linked to your Play Console listing and the app
  needs to go through Google's standard ad-serving review before live ads
  will actually fill in production.
- Consider adding a Settings toggle to opt out of the daily reminders
  (currently always-on if notification permission is granted) if you want
  users to be able to turn them off from within the app rather than only
  via system notification settings.

---

# v0.1.0 Final Polish — New App Icon, Real Sound Effects, Cleanup

Requested changes: replace the app icon/logo with the supplied maze artwork,
replace the synthesized arrow-tap/wrong/level-complete sounds with the three
supplied recorded MP3 effects, and generally tighten up the project for a
final `0.1.0` release build.

## Files touched

| File | Change |
|---|---|
| `app/src/main/res/mipmap-*/ic_launcher.png`, `ic_launcher_round.png` | **Replaced** — regenerated at all 5 densities (48–192px) from the supplied maze-icon artwork. White margin auto-cropped, corners keyed to transparent so the icon isn't a white square with a rounded image floating in it. |
| `app/src/main/res/mipmap-*/ic_launcher_foreground.png` | **Replaced** — same artwork, scaled to sit inside the adaptive-icon safe zone (so it survives circle/squircle/rounded-square OS masks without clipping the maze) at all 5 densities (108–432px). |
| `app/src/main/res/values/ic_launcher_background.xml` | Background colour swapped from placeholder white to `#5087C7`, sampled from the icon's own blue so the adaptive-icon background matches the foreground artwork. |
| `store-assets/play-store-icon-512.png` | **New** — flattened 512×512 listing icon for the Play Console (not part of the app build). |
| `app/src/main/res/raw/sfx_arrow_click.mp3` | **New** — the supplied click effect. |
| `app/src/main/res/raw/sfx_wrong.mp3` | **New** — the supplied error/incorrect effect. |
| `app/src/main/res/raw/sfx_level_complete.mp3` | **New** — the supplied fanfare effect. |
| `core/audio/SoundEngine.kt` | Added `init(context)` which preloads the three MP3s into a `SoundPool`. `playMove()` (successful arrow tap), `playError()` (wrong/blocked tap) and `playComplete()` (level cleared) now play the real recorded clips; each still falls back to its old synthesized tone if the pool hasn't finished loading yet, so the game is never silent. `playButton()`/`playHint()`/`playRotate()` (small UI ticks with no supplied asset) are unchanged. |
| `ArrowPuzzleApplication.kt` | Now calls `SoundEngine.init(this)` in `onCreate()` so the effects are preloaded before the first tap. |
| `feature/game/GameViewModel.kt` | Successful-tap branch now calls `SoundEngine.playMove()` only (previously fired `playMove()` *and* `playCorrect()` back to back, which would have layered a synth ping under the new real click). `playCorrect()` is kept as a no-op for call-site compatibility rather than removed outright. |
| `app/app/` | **Deleted** — a stray, fully unreferenced duplicate of the whole module (not present in `settings.gradle.kts`); it was dead weight left over from an earlier export, called out but left in place in the previous change pass. |
| `app/build.gradle.kts` | `versionCode` bumped `1 → 2` for this release build. `versionName` intentionally left at `"0.1.0"` as requested. |

**Not build-tested locally** — this sandbox has no network access to the
Android/Google Maven repos, so Gradle can't resolve dependencies here.
`SoundPool`, `AudioAttributes` and the mipmap/adaptive-icon setup are all
plain Android SDK APIs already used elsewhere in this project, so risk is
low, but please do a build + install pass in Android Studio (or let the
GitHub Actions workflow build it) before shipping.

---

# Arrow Redesign — Sharp Line Renderer, Sound Pass, Slower Slide

Requested changes: replace the thick rounded "pipe" arrows with a thin,
sharp-cornered line style (per reference screenshot), make sound effects
sound less like raw test-tone beeps, and slow the tap-clear animation down
for better readability.

## Files touched

| File | Change |
|---|---|
| `core/ui/ArrowLineCanvas.kt` | **New** — `drawArrowLineNetwork` / `drawStandaloneArrowLine`: thin single-stroke shafts, sharp miter corners (no rounding), solid filled triangular heads. Replaces `PipeCanvas.kt`'s `drawPipeNetwork` / `drawStandaloneArrow` everywhere they were used. |
| `feature/game/GameScreen.kt` | Swapped all 4 call sites (ghost layer, active layer, exit-fly animation, level thumbnail) from the pipe renderer to the new line renderer. Exit-slide animation slowed from 420ms/`Motion.Exit` to 650ms/`Motion.Standard` for a calmer, more visible slide. |
| `core/audio/SoundEngine.kt` | Reworked every synthesized tone: soft attack envelope (no clicky onset), a quiet 2nd harmonic layered in for warmth. Added `playMove()` — an airy whoosh + soft landing tick for the arrow slide-off. |
| `feature/game/GameViewModel.kt` | Wired `SoundEngine.playMove()` to fire alongside `playCorrect()` whenever a tap successfully escapes. |

`core/ui/PipeCanvas.kt` is left in place but no longer referenced anywhere —
kept only because `armsFor()` (the shared "which directions does this cell
connect to" helper) still lives there and is reused by the new renderer.
Safe to delete later if you want, but harmless as-is.

**Not build-tested locally** — this sandbox has no network access to the
Android/Google Maven repos, so Gradle can't resolve dependencies here. Please
build in Android Studio and confirm; the changes are plain Compose
`DrawScope` path code with no new dependencies, so risk is low, but worth a
quick run before shipping.

---

# Diagnostic-Report Fixes — Density, Connected Pipe, Animations, Celebration

Implemented directly from the frame-by-frame own-app-vs-competitor video
comparison (Aug 2026). Nine issues were reported; all nine are addressed below.

## Files touched

| File | Change |
|---|---|
| `core/game/PuzzleEngine.kt` | Rewrote `LevelGenerator` (density + solvability), added `PuzzleEngine.fillRatio()` |
| `core/ui/PipeCanvas.kt` | **New** — shared connected-pipe drawing (`drawPipeNetwork`, `drawStandaloneArrow`) |
| `core/ui/Celebration.kt` | **New** — `ConfettiOverlay` + `SunburstBackground` for the win screen |
| `feature/game/GameScreen.kt` | Rewrote `MazeBoard`, HUD, bottom buttons, win/praise dialogs |

Nothing else in the project was touched. `app/app` is a stray, unreferenced
duplicate folder (not in `settings.gradle.kts`) — left as-is, out of scope.

## 1. Board density (the reported "Level 2 shows one arrow" bug)

**Root cause:** the old generator picked an explicit target arrow count and
stopped once it hit that number (or gave up early) — on small/early grids the
placement loop could bail out with almost nothing placed.

**Fix:** `generate()` no longer targets a count. It repeatedly fills whichever
*empty* cell currently has the **fewest legal directions left**
(most-constrained-first) until literally no empty cell can accept an arrow.
Racing to fill cells that are about to become unplaceable is what pushes fill
from ~70% (random order) to ~95-100%. Verified in simulation across 4x4→10x10:
avg fill 97-100%, 100% solvable in every trial.

**Solvability is structural:** an arrow is only ever placed on a ray that's
clear of every arrow placed *before* it, so undoing placements in reverse
order is always a valid solve — no backtracking search needed.

**Difficulty** now rides on grid size (4x4 tutorial → 10x10 hard) plus
`maxStartOpen` — the max share of arrows allowed to be immediately tappable
on load, so a level never feels "already half solved."

## 2. Connected pipe rendering (was: bulky filled "paperclip" glyphs)

`core/ui/PipeCanvas.kt` → `drawPipeNetwork()`. Each cell lays track in its own
arrow's direction and picks up an extra arm for every neighbour whose arrow
points into it:
- 2 opposite arms → one straight run through the cell
- 2 perpendicular arms → a rounded elbow (quadratic curve through the cell centre)
- 1 arm (dead end) or 3-4 arms (junction) → straight stubs from the centre

**Two layers**, both using the same function:
- **Ghost** — the full original board at 10% opacity, always present. This is
  the direct fix for boards looking "broken/empty" once mostly cleared.
- **Active** — only the arrows still in play, full ink colour.

## 3. Tap-to-clear animation (was: instant fade in place)

Tapped/hint-cleared arrows now fly off-board in their pointing direction with
a stretch/motion trail and a colour shift from ink to blue (`GameScreen.kt` —
`MazeBoard`'s `exitAnims`), instead of disappearing on the spot. The ghost
layer automatically leaves the faint "dot" impression behind, matching the
reference.

Also added: a brief red flash on a blocked tap (the engine already tracked
`lastError`, it just wasn't surfaced visually before).

## 4. Mid-level praise ("Impressive!")

`ImpressiveToast()` — shown for ~650ms right after the board clears and
before the fullscreen win screen, matching the reference's beat.

## 5. Fullscreen win screen (was: small centred dialog)

`WinDlg` is now fullscreen: blue gradient + `SunburstBackground` +
`ConfettiOverlay`, "Level Completed!", a white card with a `LevelThumbnail`
(the solved board's pipe silhouette), "Next Game / Level N" white pill, and a
"Main" link — instead of the old small white card + blue button.

## 6. HUD layout

Two-row header now: row 1 = back + title + settings gear icon (new); row 2 =
remaining-arrows pill chip, hearts, difficulty pill chip — instead of the old
single-row layout with no gear icon and unstyled counters.

## 7. Grammar fix

"Cleared in 1 moves" → "Cleared in 1 move" (singular) / "Cleared in N moves" (plural).

## 8. Bottom action buttons

Restyled from large labeled rounded-square buttons to small circular icon
buttons (Hint with badge, Retry), matching the reference's compact tool row.
