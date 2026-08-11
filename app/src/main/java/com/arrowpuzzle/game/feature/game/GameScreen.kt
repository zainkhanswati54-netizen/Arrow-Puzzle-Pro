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
import androidx.compose.ui.geometry.Offset
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
import com.arrowpuzzle.game.core.game.PuzzleEngine
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
    levelId: Int = 1,
    isDaily: Boolean = false,
    onDailyComplete: () -> Unit = {}
) {
    val context = LocalContext.current
    val vm: GameViewModel = viewModel(
        key = "game_$levelId",
        factory = GameViewModel.factory(context, levelId, isDaily)
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
                    title = if (isDaily) "Daily Challenge" else "Level ${puzzle.level.id}",
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
        if (ui.showWinCelebration && puzzle != null) {
            WinDlg(
                moves = puzzle.moveCount,
                isDaily = isDaily,
                onNext = { vm.nextLevel() },
                onClaim = { onDailyComplete() },
                onExit = onExit
            )
        }
        // Game over
        if (ui.showGameOver) GameOverDlg({ vm.retry() }, onExit)
    }
}

// ── MAZE BOARD ───────────────────────────────────────────────────────────────

@Composable
private fun MazeBoard(puzzle: PuzzleState, hintCell: CellKey?, onCellTap: (Int, Int) -> Unit, modifier: Modifier) {
    val level = puzzle.level
    val allCells = remember(level) { level.arrows.map { CellKey(it.row, it.col) }.toSet() }
    val adjPairs = remember(level) { PuzzleEngine.adjacencyPairs(level) }

    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        val cellW = maxWidth / level.gridCols
        val cellH = maxHeight / level.gridRows
        val cellSize = min(cellW, cellH).coerceAtMost(64.dp)
        val bw = cellSize * level.gridCols
        val bh = cellSize * level.gridRows
        val lineW = cellSize * 0.45f

        Box(Modifier.width(bw).height(bh)) {
            // Draw maze structure
            Box(Modifier.fillMaxSize().drawWithCache {
                val cs = cellSize.toPx()
                val lw = lineW.toPx()
                onDrawBehind {
                    // Connecting lines
                    for ((a, b) in adjPairs) {
                        val ax = a.col * cs + cs / 2; val ay = a.row * cs + cs / 2
                        val bx = b.col * cs + cs / 2; val by = b.row * cs + cs / 2
                        val aR = a in puzzle.remaining; val bR = b in puzzle.remaining
                        val col = when { aR && bR -> Ink; aR || bR -> Ink.copy(0.3f); else -> Blue500.copy(0.15f) }
                        drawLine(col, Offset(ax, ay), Offset(bx, by), lw, StrokeCap.Round)
                    }
                    // Cell nodes
                    for (c in allCells) {
                        val cx = c.col * cs + cs / 2; val cy = c.row * cs + cs / 2
                        val inR = c in puzzle.remaining
                        drawCircle(if (inR) Ink else Blue500.copy(0.12f), lw / 2, Offset(cx, cy))
                    }
                }
            })

            // Arrow glyphs
            puzzle.remaining.forEach { (cell, dir) ->
                key(cell) {
                    ArrowTile(dir, cell == hintCell, cellSize, { onCellTap(cell.row, cell.col) },
                        Modifier.offset(cellSize * cell.col, cellSize * cell.row).size(cellSize))
                }
            }
        }
    }
}

@Composable
private fun ArrowTile(dir: Direction, isHint: Boolean, cellSize: Dp, onTap: () -> Unit, modifier: Modifier) {
    val src = remember { MutableInteractionSource() }
    Box(modifier.clickable(src, null, role = Role.Button, onClick = onTap)
        .pressScale(src, 0.85f).padding(cellSize * 0.15f), Alignment.Center) {
        val color = if (isHint) Blue500 else Color.White
        Box(Modifier.fillMaxSize().graphicsLayer { rotationZ = dir.degrees }
            .drawWithCache {
                val w = size.width; val h = size.height; val sw = w * 0.19f
                val tipX = w * 0.86f; val midY = h * 0.5f; val hs = h * 0.26f
                val shaft = Path().apply { moveTo(w * 0.14f, midY); lineTo(tipX - sw * 0.4f, midY) }
                val head = Path().apply { moveTo(tipX - hs, midY - hs); lineTo(tipX, midY); lineTo(tipX - hs, midY + hs) }
                val stroke = Stroke(sw, cap = StrokeCap.Round, join = StrokeJoin.Round)
                onDrawBehind { drawPath(shaft, color, style = stroke); drawPath(head, color, style = stroke) }
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
private fun WinDlg(
    moves: Int,
    isDaily: Boolean,
    onNext: () -> Unit,
    onClaim: () -> Unit,
    onExit: () -> Unit
) {
    androidx.compose.ui.window.Dialog(onExit, androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().background(Color.Black.copy(0.45f)).padding(28.dp), Alignment.Center) {
            val s = remember { MutableTransitionState(false) }; s.targetState = true
            AnimatedVisibility(s, enter = fadeIn(tween(300)) + scaleIn(initialScale = 0.85f, animationSpec = Motion.playful()), exit = fadeOut(tween(200)) + scaleOut(targetScale = 0.95f)) {
                Surface(shape = RoundedCornerShape(26.dp), color = AppTheme.palette.surface, shadowElevation = 24.dp) {
                    Column(Modifier.padding(24.dp, 32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(if (isDaily) "⭐" else "🎉", style = MaterialTheme.typography.displayLarge, modifier = Modifier.pulse(min = 0.92f, max = 1.1f, periodMillis = 1800))
                        Spacer(Modifier.height(16.dp))
                        Text(
                            if (isDaily) "Daily Star Earned!" else "Level Complete!",
                            style = MaterialTheme.typography.headlineMedium,
                            color = AppTheme.palette.ink
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            if (isDaily) "Cleared in $moves moves — come back in 24 hours for the next one."
                            else "Cleared in $moves moves",
                            style = MaterialTheme.typography.bodyLarge,
                            color = AppTheme.palette.inkMuted,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(28.dp))
                        if (isDaily) {
                            PrimaryPillButton("Claim Star", onClaim, Modifier.fillMaxWidth())
                        } else {
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
