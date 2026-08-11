package com.arrowpuzzle.game.feature.game

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arrowpuzzle.game.core.audio.SoundEngine
import com.arrowpuzzle.game.core.design.AppTheme
import com.arrowpuzzle.game.core.design.Blue500
import com.arrowpuzzle.game.core.design.Ink
import com.arrowpuzzle.game.core.design.Red500
import com.arrowpuzzle.game.core.game.CellKey
import com.arrowpuzzle.game.core.game.Direction
import com.arrowpuzzle.game.core.game.PuzzleState
import com.arrowpuzzle.game.core.motion.Motion
import com.arrowpuzzle.game.core.motion.enterFromBelow
import com.arrowpuzzle.game.core.motion.pressScale
import com.arrowpuzzle.game.core.motion.pulse
import com.arrowpuzzle.game.core.ui.AppTopBar
import com.arrowpuzzle.game.core.ui.ArrowBackdrop
import com.arrowpuzzle.game.core.ui.PrimaryPillButton

@Composable
fun GameScreen(
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
    levelId: Int = 1
) {
    val context = LocalContext.current
    val vm: GameViewModel = viewModel(
        key = "game_$levelId",
        factory = GameViewModel.factory(context, levelId)
    )
    val ui by vm.state.collectAsState()
    val puzzle = ui.puzzle
    val palette = AppTheme.palette
    val haptics = LocalHapticFeedback.current

    Box(modifier.fillMaxSize().background(palette.canvas)) {
        ArrowBackdrop(tint = Color(0xFFE8EDF4))

        if (ui.loading || puzzle == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Blue500)
            }
        } else {
            Column(Modifier.fillMaxSize()) {
                AppTopBar(
                    title = "Level ${puzzle.level.id}",
                    onBack = onExit,
                    trailing = { LivesRow(puzzle.lives) }
                )

                // Arrow count + difficulty
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🏁", style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.width(6.dp))
                        Text("${puzzle.remaining.size}", style = MaterialTheme.typography.titleMedium, color = palette.ink)
                    }
                    Text(puzzle.level.difficulty.name, style = MaterialTheme.typography.labelMedium, color = palette.inkMuted)
                }

                Spacer(Modifier.weight(0.05f))

                // ── MAZE ──
                MazeBoard(
                    puzzle = puzzle,
                    hintCell = ui.hintCell,
                    onCellTap = { r, c -> haptics.performHapticFeedback(HapticFeedbackType.LongPress); vm.onCellTap(r, c) },
                    modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 12.dp)
                )

                Spacer(Modifier.weight(0.05f))

                // Buttons
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 48.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally)
                ) {
                    ToolBtn(Icons.Rounded.Lightbulb, "Hint", puzzle.hintsRemaining) {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress); vm.onHint()
                    }
                    ToolBtn(Icons.Rounded.Refresh, "Retry") {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress); SoundEngine.playButton(); vm.retry()
                    }
                }

                Spacer(Modifier.height(16.dp))
            }
        }

        // Tutorial
        if (puzzle?.level?.isTutorial == true && ui.tutorialStep == 0) TutOverlay { vm.dismissTutorial() }
        // Win
        if (ui.showWinCelebration && puzzle != null) WinDlg(puzzle.level.id, puzzle.moveCount, { vm.nextLevel() }, onExit)
        // Game over
        if (ui.showGameOver) GameOverDlg({ vm.retry() }, onExit)
    }
}

// ── MAZE BOARD ───────────────────────────────────────────────────────────────
//
// Rendering model: the board is drawn as a continuous "pipe" network rather than
// one detached glyph per cell. Track follows the FLOW of the arrows — a cell
// always lays track the way its arrow points, and picks up track from any
// neighbour aiming into it. Consecutive arrows merge into one unbroken run and
// direction changes become rounded elbows, which is what produces the labyrinth
// look. (Connecting every adjacent pair instead would just draw a solid grid
// lattice once the board is densely filled.)
//
// Two layers are drawn:
//   1. Ghost layer — the FULL original board at low opacity. This is what keeps
//      the screen from looking empty/broken once most arrows are cleared: the
//      maze silhouette stays, and cleared cells read as empty track.
//   2. Active layer — only the arrows still on the board, in full ink.

@Composable
private fun MazeBoard(puzzle: PuzzleState, hintCell: CellKey?, onCellTap: (Int, Int) -> Unit, modifier: Modifier) {
    val level = puzzle.level
    val allArrows = remember(level) { level.arrows.associate { CellKey(it.row, it.col) to it.direction } }

    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        val cellW = maxWidth / level.gridCols
        val cellH = maxHeight / level.gridRows
        val cellSize = min(cellW, cellH).coerceAtMost(64.dp)
        val bw = cellSize * level.gridCols
        val bh = cellSize * level.gridRows
        val pipeW = cellSize * 0.58f

        Box(Modifier.width(bw).height(bh)) {
            Box(Modifier.fillMaxSize().drawWithCache {
                val cs = cellSize.toPx()
                val lw = pipeW.toPx()
                val ghost = buildPipePath(allArrows, cs)
                val active = buildPipePath(puzzle.remaining, cs)
                val stroke = Stroke(lw, cap = StrokeCap.Round, join = StrokeJoin.Round)
                onDrawBehind {
                    drawPath(ghost, Blue500.copy(alpha = 0.13f), style = stroke)
                    drawPath(active, Ink, style = stroke)
                    if (hintCell != null && hintCell in puzzle.remaining) {
                        drawRoundRect(
                            color = Blue500,
                            topLeft = Offset(hintCell.col * cs, hintCell.row * cs),
                            size = Size(cs, cs),
                            cornerRadius = CornerRadius(cs * 0.32f)
                        )
                    }
                }
            })

            // Arrow heads sit on top of the pipe. One per cell, because one cell
            // is one independently tappable arrow — merging a run into a single
            // arrowhead would hide how many taps that run actually needs.
            puzzle.remaining.forEach { (cell, dir) ->
                key(cell) {
                    ArrowTile(dir, cellSize, { onCellTap(cell.row, cell.col) },
                        Modifier.offset(cellSize * cell.col, cellSize * cell.row).size(cellSize))
                }
            }
        }
    }
}

/**
 * Builds the pipe geometry for the arrows currently on the board.
 *
 * A cell lays track in its own direction (so you can see where the arrow is
 * headed) and picks up track from any neighbour pointing into it. Per cell:
 * opposite arms become one straight run, a single perpendicular pair becomes a
 * rounded elbow (quarter circle drawn as a cubic), and extra arms on a junction
 * become stubs into the centre.
 */
private fun buildPipePath(arrows: Map<CellKey, Direction>, cs: Float): Path {
    val path = Path()
    val k = 0.5523f // cubic control ratio for a quarter circle
    for ((c, dir) in arrows) {
        val cx = c.col * cs + cs / 2f
        val cy = c.row * cs + cs / 2f
        val lx = cx - cs / 2f; val rx = cx + cs / 2f
        val ty = cy - cs / 2f; val by = cy + cs / 2f

        var left = dir == Direction.Left
        var right = dir == Direction.Right
        var up = dir == Direction.Up
        var down = dir == Direction.Down
        if (arrows[CellKey(c.row, c.col - 1)] == Direction.Right) left = true
        if (arrows[CellKey(c.row, c.col + 1)] == Direction.Left) right = true
        if (arrows[CellKey(c.row - 1, c.col)] == Direction.Down) up = true
        if (arrows[CellKey(c.row + 1, c.col)] == Direction.Up) down = true

        val runH = left && right
        val runV = up && down

        if (runH) { path.moveTo(lx, cy); path.lineTo(rx, cy) }
        if (runV) { path.moveTo(cx, ty); path.lineTo(cx, by) }

        if (runH && !runV) {
            if (up) { path.moveTo(cx, ty); path.lineTo(cx, cy) }
            if (down) { path.moveTo(cx, by); path.lineTo(cx, cy) }
        } else if (runV && !runH) {
            if (left) { path.moveTo(lx, cy); path.lineTo(cx, cy) }
            if (right) { path.moveTo(rx, cy); path.lineTo(cx, cy) }
        } else if (!runH && !runV) {
            val hasH = left || right
            val hasV = up || down
            val hx = if (left) lx else rx
            val vy = if (up) ty else by
            when {
                hasH && hasV -> {
                    path.moveTo(hx, cy)
                    path.cubicTo(hx + (cx - hx) * k, cy, cx, vy + (cy - vy) * k, cx, vy)
                }
                hasH -> { path.moveTo(hx, cy); path.lineTo(cx, cy) }
                hasV -> { path.moveTo(cx, vy); path.lineTo(cx, cy) }
                // Unreachable while every cell lays its own direction, but a
                // round cap on a zero-length segment keeps a lone cell visible.
                else -> { path.moveTo(cx, cy); path.lineTo(cx, cy) }
            }
        }
    }
    return path
}

@Composable
private fun ArrowTile(dir: Direction, cellSize: Dp, onTap: () -> Unit, modifier: Modifier) {
    val src = remember { MutableInteractionSource() }
    Box(modifier.clickable(src, null, role = Role.Button, onClick = onTap)
        .pressScale(src, 0.85f).padding(cellSize * 0.21f), Alignment.Center) {
        Box(Modifier.fillMaxSize().graphicsLayer { rotationZ = dir.degrees }
            .drawWithCache {
                val w = size.width; val h = size.height; val sw = w * 0.19f
                val tipX = w * 0.86f; val midY = h * 0.5f; val hs = h * 0.26f
                val shaft = Path().apply { moveTo(w * 0.14f, midY); lineTo(tipX - sw * 0.4f, midY) }
                val head = Path().apply { moveTo(tipX - hs, midY - hs); lineTo(tipX, midY); lineTo(tipX - hs, midY + hs) }
                val stroke = Stroke(sw, cap = StrokeCap.Round, join = StrokeJoin.Round)
                onDrawBehind {
                    drawPath(shaft, Color.White, style = stroke)
                    drawPath(head, Color.White, style = stroke)
                }
            })
    }
}

// ── UI COMPONENTS ────────────────────────────────────────────────────────────

@Composable
private fun LivesRow(lives: Int) = Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
    repeat(3) { Icon(Icons.Rounded.Favorite, null, tint = if (it < lives) Red500 else Red500.copy(0.22f), modifier = Modifier.size(18.dp)) }
}

@Composable
private fun ToolBtn(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, badge: Int? = null, onClick: () -> Unit) {
    val pal = AppTheme.palette; val src = remember { MutableInteractionSource() }
    Column(Modifier.pressScale(src, 0.94f).shadow(8.dp, RoundedCornerShape(20.dp)).clip(RoundedCornerShape(20.dp))
        .background(pal.surface).clickable(src, null, role = Role.Button, onClick = onClick)
        .padding(horizontal = 24.dp, vertical = 14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box {
            Icon(icon, null, tint = Blue500, modifier = Modifier.size(24.dp))
            if (badge != null && badge > 0) Box(Modifier.align(Alignment.TopEnd).offset(8.dp, (-4).dp).size(16.dp).clip(CircleShape).background(Blue500), Alignment.Center) {
                Text("$badge", style = MaterialTheme.typography.labelSmall, color = Color.White)
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = pal.inkSoft)
    }
}

@Composable
private fun TutOverlay(onDismiss: () -> Unit) {
    Box(Modifier.fillMaxSize().background(Color.Black.copy(0.55f))
        .clickable(remember { MutableInteractionSource() }, null) { SoundEngine.playButton(); onDismiss() }, Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.enterFromBelow(travel = 30f).padding(40.dp)) {
            Icon(Icons.Rounded.AutoAwesome, null, tint = Color.White, modifier = Modifier.pulse().size(48.dp))
            Spacer(Modifier.height(20.dp))
            Text("Tap arrows to escape!", style = MaterialTheme.typography.headlineMedium, color = Color.White, textAlign = TextAlign.Center)
            Spacer(Modifier.height(12.dp))
            Text("Tap an arrow to send it flying off the board.\nIt only moves if nothing blocks the path!\nClear all arrows to complete the level.", style = MaterialTheme.typography.bodyLarge, color = Color.White.copy(0.82f), textAlign = TextAlign.Center)
            Spacer(Modifier.height(28.dp))
            Text("Tap anywhere to start", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(0.55f))
        }
    }
}

@Composable
private fun WinDlg(levelId: Int, moves: Int, onNext: () -> Unit, onExit: () -> Unit) {
    androidx.compose.ui.window.Dialog(onExit, androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().background(Color.Black.copy(0.45f)).padding(28.dp), Alignment.Center) {
            val s = remember { MutableTransitionState(false) }; s.targetState = true
            AnimatedVisibility(s, enter = fadeIn(tween(300)) + scaleIn(initialScale = 0.85f, animationSpec = Motion.playful()), exit = fadeOut(tween(200)) + scaleOut(targetScale = 0.95f)) {
                Surface(shape = RoundedCornerShape(26.dp), color = AppTheme.palette.surface, shadowElevation = 24.dp) {
                    Column(Modifier.padding(24.dp, 32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🎉", style = MaterialTheme.typography.displayLarge, modifier = Modifier.pulse(min = 0.92f, max = 1.1f, periodMillis = 1800))
                        Spacer(Modifier.height(16.dp))
                        Text("Level Complete!", style = MaterialTheme.typography.headlineMedium, color = AppTheme.palette.ink)
                        Spacer(Modifier.height(8.dp))
                        Text("Cleared in $moves moves", style = MaterialTheme.typography.bodyLarge, color = AppTheme.palette.inkMuted)
                        Spacer(Modifier.height(28.dp))
                        PrimaryPillButton("Next Level", onNext, Modifier.fillMaxWidth())
                        Spacer(Modifier.height(12.dp))
                        Text("Menu", style = MaterialTheme.typography.titleMedium, color = Blue500,
                            modifier = Modifier.clip(CircleShape).clickable { onExit() }.padding(16.dp, 8.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun GameOverDlg(onRetry: () -> Unit, onExit: () -> Unit) {
    androidx.compose.ui.window.Dialog(onExit, androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().background(Color.Black.copy(0.45f)).padding(28.dp), Alignment.Center) {
            val s = remember { MutableTransitionState(false) }; s.targetState = true
            AnimatedVisibility(s, enter = fadeIn(tween(300)) + scaleIn(initialScale = 0.85f, animationSpec = Motion.bouncy()), exit = fadeOut(tween(200))) {
                Surface(shape = RoundedCornerShape(26.dp), color = AppTheme.palette.surface, shadowElevation = 24.dp) {
                    Column(Modifier.padding(24.dp, 32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Out of lives!", style = MaterialTheme.typography.headlineMedium, color = AppTheme.palette.ink)
                        Spacer(Modifier.height(8.dp))
                        Text("Only tap arrows with a clear path ahead!", style = MaterialTheme.typography.bodyLarge, color = AppTheme.palette.inkMuted, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(28.dp))
                        PrimaryPillButton("Try Again", onRetry, Modifier.fillMaxWidth())
                        Spacer(Modifier.height(12.dp))
                        Text("Menu", style = MaterialTheme.typography.titleMedium, color = Blue500,
                            modifier = Modifier.clip(CircleShape).clickable { onExit() }.padding(16.dp, 8.dp))
                    }
                }
            }
        }
    }
}
