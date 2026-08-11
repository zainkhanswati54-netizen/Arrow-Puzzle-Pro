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
