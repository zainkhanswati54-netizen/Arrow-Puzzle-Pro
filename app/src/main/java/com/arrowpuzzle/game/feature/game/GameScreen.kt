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
    mode: String,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
    levelId: Int = 1,
    onNextLevel: ((Int) -> Unit)? = null
) {
    val context = LocalContext.current
    val vm: GameViewModel = viewModel(
        key = "game_$levelId",
        factory = GameViewModel.factory(context, levelId)
    )
    val uiState by vm.state.collectAsState()
    val puzzle = uiState.puzzle ?: return
    val palette = AppTheme.palette
    val haptics = LocalHapticFeedback.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(palette.canvas)
    ) {
        ArrowBackdrop(tint = Color(0xFFE8EDF4))

        Column(Modifier.fillMaxSize()) {
            AppTopBar(
                title = "Level ${puzzle.level.id}",
                onBack = onExit,
                trailing = { LivesIndicator(lives = puzzle.lives) }
            )

            // Arrows remaining + difficulty
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Flag icon + remaining count
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "🏁", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "${puzzle.remaining.size}",
                        style = MaterialTheme.typography.titleMedium,
                        color = palette.ink
                    )
                }
                Text(
                    text = puzzle.level.difficulty.name,
                    style = MaterialTheme.typography.labelMedium,
                    color = palette.inkMuted
                )
            }

            Spacer(Modifier.height(8.dp))

            // The maze board
            MazeBoard(
                puzzle = puzzle,
                escapable = uiState.escapable,
                hintCell = uiState.hintCell,
                onCellTap = { row, col ->
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    vm.onCellTap(row, col)
                },
                modifier = Modifier
                    .enterFromBelow(delayMillis = Motion.stagger(1), travel = 20f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )

            Spacer(Modifier.height(24.dp))

            // Tool buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .enterFromBelow(delayMillis = Motion.stagger(2))
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
            ) {
                ToolButton(
                    label = "Hint",
                    badgeCount = puzzle.hintsRemaining,
                    icon = Icons.Rounded.Lightbulb,
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        vm.onHint()
                    },
                    modifier = Modifier.weight(1f)
                )
                ToolButton(
                    label = "Retry",
                    icon = Icons.Rounded.Refresh,
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        SoundEngine.playButton()
                        vm.retry()
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.weight(1f))
        }

        // Tutorial overlay
        if (puzzle.level.isTutorial && uiState.tutorialStep == 0) {
            TutorialOverlay(onDismiss = { vm.dismissTutorial() })
        }

        // Win
        if (uiState.showWinCelebration) {
            WinDialog(
                levelId = puzzle.level.id,
                moves = puzzle.moveCount,
                onNext = {
                    vm.dismissWin()
                    onNextLevel?.invoke(puzzle.level.id + 1) ?: onExit()
                },
                onExit = { vm.dismissWin(); onExit() }
            )
        }

        // Game Over
        if (uiState.showGameOver) {
            GameOverDialog(
                onRetry = { vm.retry() },
                onExit = onExit
            )
        }
    }
}

// ── Maze Board ───────────────────────────────────────────────────────────────

@Composable
private fun MazeBoard(
    puzzle: PuzzleState,
    escapable: Set<CellKey>,
    hintCell: CellKey?,
    onCellTap: (Int, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val level = puzzle.level
    val allCells = level.arrows.map { CellKey(it.row, it.col) }.toSet()
    val adjacency = remember(level) { PuzzleEngine.adjacencyPairs(level) }

    BoxWithConstraints(modifier = modifier) {
        val availableWidth = maxWidth
        // Calculate cell size based on grid dimensions
        val cellSize = (availableWidth / level.gridCols).coerceAtMost(72.dp)
        val boardWidth = cellSize * level.gridCols
        val boardHeight = cellSize * level.gridRows
        val lineThickness = cellSize * 0.35f

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(boardHeight),
            contentAlignment = Alignment.Center
        ) {
            // Draw connecting lines (the maze structure)
            Box(
                modifier = Modifier
                    .width(boardWidth)
                    .height(boardHeight)
                    .drawWithCache {
                        val cSize = cellSize.toPx()
                        val thick = lineThickness.toPx()
                        val radius = thick / 2f

                        onDrawBehind {
                            // Draw filled rounded rects at each original cell position
                            for (cell in allCells) {
                                val cx = cell.col * cSize + cSize / 2f
                                val cy = cell.row * cSize + cSize / 2f
                                val isRemaining = cell in puzzle.remaining
                                val color = if (isRemaining) Ink.copy(alpha = 0.12f)
                                    else Blue500.copy(alpha = 0.08f)

                                drawCircle(
                                    color = color,
                                    radius = radius,
                                    center = Offset(cx, cy)
                                )
                            }

                            // Draw connections between adjacent cells
                            for ((a, b) in adjacency) {
                                val ax = a.col * cSize + cSize / 2f
                                val ay = a.row * cSize + cSize / 2f
                                val bx = b.col * cSize + cSize / 2f
                                val by = b.row * cSize + cSize / 2f

                                val aRemaining = a in puzzle.remaining
                                val bRemaining = b in puzzle.remaining
                                val color = when {
                                    aRemaining && bRemaining -> Ink
                                    aRemaining || bRemaining -> Ink.copy(alpha = 0.45f)
                                    else -> Blue500.copy(alpha = 0.25f)
                                }

                                drawLine(
                                    color = color,
                                    start = Offset(ax, ay),
                                    end = Offset(bx, by),
                                    strokeWidth = thick,
                                    cap = StrokeCap.Round
                                )
                            }

                            // Draw filled circles at remaining cells (nodes of the maze)
                            for (cell in allCells) {
                                if (cell in puzzle.remaining) {
                                    val cx = cell.col * cSize + cSize / 2f
                                    val cy = cell.row * cSize + cSize / 2f
                                    drawCircle(
                                        color = Ink,
                                        radius = radius,
                                        center = Offset(cx, cy)
                                    )
                                }
                            }
                        }
                    }
            )

            // Overlay arrows on remaining cells
            Box(
                modifier = Modifier
                    .width(boardWidth)
                    .height(boardHeight)
            ) {
                puzzle.remaining.forEach { (cell, direction) ->
                    val isHint = cell == hintCell
                    key(cell) {
                        ArrowOnMaze(
                            direction = direction,
                            isHint = isHint,
                            cellSize = cellSize,
                            onTap = { onCellTap(cell.row, cell.col) },
                            modifier = Modifier
                                .offset(x = cellSize * cell.col, y = cellSize * cell.row)
                                .size(cellSize)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ArrowOnMaze(
    direction: Direction,
    isHint: Boolean,
    cellSize: Dp,
    onTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = onTap
            )
            .pressScale(interactionSource, pressedScale = 0.85f)
            .then(
                if (isHint) Modifier.graphicsLayer {
                    // Pulsing glow for hint
                } else Modifier
            )
            .padding(cellSize * 0.18f),
        contentAlignment = Alignment.Center
    ) {
        ArrowGlyph(
            direction = direction,
            color = if (isHint) Blue500 else Color.White,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun ArrowGlyph(
    direction: Direction,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .graphicsLayer { rotationZ = direction.degrees }
            .drawWithCache {
                val w = size.width
                val h = size.height
                val strokeWidth = w * 0.18f
                val tipX = w * 0.88f
                val midY = h * 0.5f
                val headSpan = h * 0.28f

                val shaft = Path().apply {
                    moveTo(w * 0.12f, midY)
                    lineTo(tipX - strokeWidth * 0.4f, midY)
                }
                val head = Path().apply {
                    moveTo(tipX - headSpan, midY - headSpan)
                    lineTo(tipX, midY)
                    lineTo(tipX - headSpan, midY + headSpan)
                }
                val stroke = Stroke(
                    width = strokeWidth,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )

                onDrawBehind {
                    drawPath(shaft, color, style = stroke)
                    drawPath(head, color, style = stroke)
                }
            }
    )
}

// ── Lives ────────────────────────────────────────────────────────────────────

@Composable
private fun LivesIndicator(lives: Int, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        repeat(3) { i ->
            Icon(
                Icons.Rounded.Favorite, null,
                tint = if (i < lives) Red500 else Red500.copy(alpha = 0.22f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

// ── Tool button ──────────────────────────────────────────────────────────────

@Composable
private fun ToolButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    badgeCount: Int? = null
) {
    val palette = AppTheme.palette
    val src = remember { MutableInteractionSource() }
    Column(
        modifier = modifier
            .pressScale(src, pressedScale = 0.94f)
            .shadow(8.dp, RoundedCornerShape(16.dp), ambientColor = Ink.copy(0.08f), spotColor = Ink.copy(0.12f))
            .clip(RoundedCornerShape(16.dp))
            .background(palette.surface)
            .clickable(src, null, role = Role.Button, onClick = onClick)
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box {
            Icon(icon, null, tint = Blue500, modifier = Modifier.size(22.dp))
            if (badgeCount != null && badgeCount > 0) {
                Box(
                    Modifier.align(Alignment.TopEnd).offset(x = 8.dp, y = (-4).dp)
                        .size(16.dp).clip(CircleShape).background(Blue500),
                    contentAlignment = Alignment.Center
                ) { Text("$badgeCount", style = MaterialTheme.typography.labelSmall, color = Color.White) }
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = palette.inkSoft)
    }
}

// ── Tutorial overlay ─────────────────────────────────────────────────────────

@Composable
private fun TutorialOverlay(onDismiss: () -> Unit) {
    Box(
        Modifier.fillMaxSize().background(Color.Black.copy(0.55f))
            .clickable(remember { MutableInteractionSource() }, null) {
                SoundEngine.playButton(); onDismiss()
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.enterFromBelow(travel = 30f).padding(horizontal = 40.dp)
        ) {
            Icon(Icons.Rounded.AutoAwesome, null, tint = Color.White,
                modifier = Modifier.pulse(min = 0.95f, max = 1.08f).size(48.dp))
            Spacer(Modifier.height(20.dp))
            Text("Tap arrows to escape", style = MaterialTheme.typography.headlineMedium,
                color = Color.White, textAlign = TextAlign.Center)
            Spacer(Modifier.height(12.dp))
            Text("Tap an arrow to send it flying in its direction.\nIt can only escape if nothing blocks the path!\nClear all arrows to complete the level.",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(0.82f), textAlign = TextAlign.Center)
            Spacer(Modifier.height(28.dp))
            Text("Tap anywhere to start", style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(0.55f))
        }
    }
}

// ── Win dialog ───────────────────────────────────────────────────────────────

@Composable
private fun WinDialog(levelId: Int, moves: Int, onNext: () -> Unit, onExit: () -> Unit) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onExit,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().background(Color.Black.copy(0.45f)).padding(horizontal = 28.dp),
            contentAlignment = Alignment.Center) {
            val s = remember { MutableTransitionState(false) }; s.targetState = true
            AnimatedVisibility(s, enter = fadeIn(tween(Motion.Normal)) +
                scaleIn(initialScale = 0.85f, animationSpec = Motion.playful()),
                exit = fadeOut(tween(Motion.Quick)) + scaleOut(targetScale = 0.95f)) {
                Surface(shape = RoundedCornerShape(26.dp), color = AppTheme.palette.surface, shadowElevation = 24.dp) {
                    Column(Modifier.padding(24.dp, 32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🎉", style = MaterialTheme.typography.displayLarge,
                            modifier = Modifier.pulse(min = 0.92f, max = 1.1f, periodMillis = 1800))
                        Spacer(Modifier.height(16.dp))
                        Text("Level Complete!", style = MaterialTheme.typography.headlineMedium,
                            color = AppTheme.palette.ink, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(8.dp))
                        Text("Cleared in $moves moves", style = MaterialTheme.typography.bodyLarge,
                            color = AppTheme.palette.inkMuted, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(28.dp))
                        if (levelId < 6) {
                            PrimaryPillButton("Next Level", onClick = onNext, modifier = Modifier.fillMaxWidth())
                            Spacer(Modifier.height(12.dp))
                        }
                        Text(if (levelId < 6) "Back to levels" else "Done",
                            style = MaterialTheme.typography.titleMedium, color = Blue500,
                            modifier = Modifier.clip(CircleShape).clickable { onExit() }.padding(16.dp, 8.dp))
                    }
                }
            }
        }
    }
}

// ── Game over dialog ─────────────────────────────────────────────────────────

@Composable
private fun GameOverDialog(onRetry: () -> Unit, onExit: () -> Unit) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onExit,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().background(Color.Black.copy(0.45f)).padding(horizontal = 28.dp),
            contentAlignment = Alignment.Center) {
            val s = remember { MutableTransitionState(false) }; s.targetState = true
            AnimatedVisibility(s, enter = fadeIn(tween(Motion.Normal)) +
                scaleIn(initialScale = 0.85f, animationSpec = Motion.bouncy()),
                exit = fadeOut(tween(Motion.Quick))) {
                Surface(shape = RoundedCornerShape(26.dp), color = AppTheme.palette.surface, shadowElevation = 24.dp) {
                    Column(Modifier.padding(24.dp, 32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Out of lives!", style = MaterialTheme.typography.headlineMedium,
                            color = AppTheme.palette.ink, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(8.dp))
                        Text("Tap on arrows with a clear path ahead.\nBlocked arrows cost a life!",
                            style = MaterialTheme.typography.bodyLarge,
                            color = AppTheme.palette.inkMuted, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(28.dp))
                        PrimaryPillButton("Try Again", onClick = onRetry, modifier = Modifier.fillMaxWidth())
                        Spacer(Modifier.height(12.dp))
                        Text("Back to levels", style = MaterialTheme.typography.titleMedium,
                            color = Blue500, modifier = Modifier.clip(CircleShape).clickable { onExit() }.padding(16.dp, 8.dp))
                    }
                }
            }
        }
    }
}
