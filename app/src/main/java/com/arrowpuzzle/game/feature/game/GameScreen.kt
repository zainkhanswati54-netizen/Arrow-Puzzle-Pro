package com.arrowpuzzle.game.feature.game

import androidx.compose.animation.AnimatedVisibility
<<<<<<< HEAD
=======
import androidx.compose.animation.core.Animatable
>>>>>>> e2e958806e5734d2b079726c6ebba9ed15f7b04c
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
<<<<<<< HEAD
=======
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
>>>>>>> e2e958806e5734d2b079726c6ebba9ed15f7b04c
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
<<<<<<< HEAD
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
=======
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
>>>>>>> e2e958806e5734d2b079726c6ebba9ed15f7b04c
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
<<<<<<< HEAD
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
=======
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
>>>>>>> e2e958806e5734d2b079726c6ebba9ed15f7b04c
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
<<<<<<< HEAD
=======

    val title = "Level ${puzzle.level.id}"
>>>>>>> e2e958806e5734d2b079726c6ebba9ed15f7b04c

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

<<<<<<< HEAD
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
=======
            // Progress bar
            ProgressIndicator(
                correct = uiState.correctCount,
                total = uiState.totalCount,
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .enterFromBelow(delayMillis = Motion.stagger(0))
            )

            Spacer(Modifier.height(6.dp))

            // Move counter
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Moves: ${puzzle.moveCount}",
                    style = MaterialTheme.typography.labelMedium,
                    color = palette.inkMuted
                )
                Text(
                    text = "${puzzle.level.difficulty.name}",
>>>>>>> e2e958806e5734d2b079726c6ebba9ed15f7b04c
                    style = MaterialTheme.typography.labelMedium,
                    color = palette.inkMuted
                )
            }

            Spacer(Modifier.height(8.dp))

<<<<<<< HEAD
            // The maze board
            MazeBoard(
                puzzle = puzzle,
                escapable = uiState.escapable,
                hintCell = uiState.hintCell,
=======
            Board(
                puzzle = puzzle,
>>>>>>> e2e958806e5734d2b079726c6ebba9ed15f7b04c
                onCellTap = { row, col ->
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    vm.onCellTap(row, col)
                },
                modifier = Modifier
                    .enterFromBelow(delayMillis = Motion.stagger(1), travel = 20f)
<<<<<<< HEAD
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
=======
                    .padding(horizontal = 20.dp)
>>>>>>> e2e958806e5734d2b079726c6ebba9ed15f7b04c
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
<<<<<<< HEAD
                    label = "Retry",
                    icon = Icons.Rounded.Refresh,
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        SoundEngine.playButton()
                        vm.retry()
=======
                    label = "Undo",
                    icon = Icons.Rounded.Undo,
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        vm.onUndo()
                    },
                    modifier = Modifier.weight(1f)
                )
                ToolButton(
                    label = "Shuffle",
                    icon = Icons.Rounded.Shuffle,
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        vm.onShuffle()
>>>>>>> e2e958806e5734d2b079726c6ebba9ed15f7b04c
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.weight(1f))
        }

<<<<<<< HEAD
        // Tutorial overlay
=======
        // Tutorial overlay for Level 1
>>>>>>> e2e958806e5734d2b079726c6ebba9ed15f7b04c
        if (puzzle.level.isTutorial && uiState.tutorialStep == 0) {
            TutorialOverlay(onDismiss = { vm.dismissTutorial() })
        }

<<<<<<< HEAD
        // Win
=======
        // Win celebration
>>>>>>> e2e958806e5734d2b079726c6ebba9ed15f7b04c
        if (uiState.showWinCelebration) {
            WinDialog(
                levelId = puzzle.level.id,
                moves = puzzle.moveCount,
                onNext = {
                    vm.dismissWin()
<<<<<<< HEAD
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
=======
                    val nextId = puzzle.level.id + 1
                    if (onNextLevel != null) {
                        onNextLevel(nextId)
                    } else {
                        onExit()
                    }
                },
                onExit = {
                    vm.dismissWin()
                    onExit()
                }
>>>>>>> e2e958806e5734d2b079726c6ebba9ed15f7b04c
            )
        }
    }
}

<<<<<<< HEAD
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
=======
// ── Board ────────────────────────────────────────────────────────────────────

@Composable
private fun Board(
    puzzle: PuzzleState,
    onCellTap: (row: Int, col: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = AppTheme.palette
    val gridSize = puzzle.level.gridSize

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .shadow(
                elevation = 16.dp,
                shape = RoundedCornerShape(24.dp),
                ambientColor = Ink.copy(alpha = 0.10f),
                spotColor = Ink.copy(alpha = 0.14f)
            )
            .clip(RoundedCornerShape(24.dp))
            .background(palette.surface)
            .padding(10.dp)
    ) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val cell = maxWidth / gridSize

            // Grid wells
            Column {
                repeat(gridSize) {
                    Row {
                        repeat(gridSize) { _ ->
                            Box(
                                Modifier
                                    .size(cell)
                                    .padding(3.dp)
                                    .clip(RoundedCornerShape(9.dp))
                                    .background(palette.canvasSunken.copy(alpha = 0.55f))
                            )
>>>>>>> e2e958806e5734d2b079726c6ebba9ed15f7b04c
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
<<<<<<< HEAD
=======

            // Arrows
            puzzle.directions.forEach { (key, direction) ->
                val isCorrect = puzzle.isComplete || PuzzleEngine.isCellCorrect(puzzle, key)
                ArrowTile(
                    direction = direction,
                    isCorrect = isCorrect,
                    isComplete = puzzle.isComplete,
                    onTap = { onCellTap(key.row, key.col) },
                    modifier = Modifier
                        .offset(x = cell * key.col, y = cell * key.row)
                        .size(cell)
                )
            }
>>>>>>> e2e958806e5734d2b079726c6ebba9ed15f7b04c
        }
    }
}

@Composable
<<<<<<< HEAD
private fun ArrowOnMaze(
    direction: Direction,
    isHint: Boolean,
    cellSize: Dp,
=======
private fun ArrowTile(
    direction: Direction,
    isCorrect: Boolean,
    isComplete: Boolean,
>>>>>>> e2e958806e5734d2b079726c6ebba9ed15f7b04c
    onTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
<<<<<<< HEAD
=======
    val reduced = AppTheme.reducedMotion
    val rotation = remember { Animatable(direction.degrees) }
    var tapCount by remember { mutableIntStateOf(0) }

    // Animate rotation when direction changes
    LaunchedEffect(direction) {
        val target = direction.degrees
        // Handle wrap-around (e.g., 270 -> 0 should go to 360)
        var current = rotation.value % 360f
        if (current < 0) current += 360f
        var t = target % 360f
        if (t < 0) t += 360f

        // Always rotate clockwise
        if (t <= current) t += 360f

        if (!reduced) {
            rotation.animateTo(t, Motion.snappy())
        } else {
            rotation.snapTo(t)
        }
    }
>>>>>>> e2e958806e5734d2b079726c6ebba9ed15f7b04c

    Box(
        modifier = modifier
            .clickable(
                interactionSource = interactionSource,
                indication = null,
<<<<<<< HEAD
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
=======
                role = Role.Button
            ) {
                tapCount++
                onTap()
            }
            .pressScale(interactionSource, pressedScale = 0.9f)
            .padding(6.dp),
        contentAlignment = Alignment.Center
    ) {
        ArrowGlyph(
            rotationDegrees = rotation.value,
            color = when {
                isComplete -> Blue500
                isCorrect -> Blue500.copy(alpha = 0.7f)
                else -> Ink
            },
>>>>>>> e2e958806e5734d2b079726c6ebba9ed15f7b04c
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun ArrowGlyph(
    rotationDegrees: Float,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .graphicsLayer { rotationZ = rotationDegrees }
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

<<<<<<< HEAD
// ── Lives ────────────────────────────────────────────────────────────────────
=======
// ── Progress bar ─────────────────────────────────────────────────────────────

@Composable
private fun ProgressIndicator(
    correct: Int,
    total: Int,
    modifier: Modifier = Modifier
) {
    val fraction = if (total > 0) correct.toFloat() / total else 0f
    val palette = AppTheme.palette

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(CircleShape)
            .background(palette.canvasSunken)
    ) {
        Box(
            Modifier
                .fillMaxWidth(fraction)
                .height(6.dp)
                .clip(CircleShape)
                .background(Blue500)
        )
    }
}

// ── Tutorial overlay ─────────────────────────────────────────────────────────

@Composable
private fun TutorialOverlay(onDismiss: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                SoundEngine.playButton()
                onDismiss()
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .enterFromBelow(travel = 30f)
                .padding(horizontal = 40.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.AutoAwesome,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier
                    .pulse(min = 0.95f, max = 1.08f, periodMillis = 2200)
                    .size(48.dp)
            )

            Spacer(Modifier.height(20.dp))

            Text(
                text = "Tap arrows to rotate them",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(12.dp))

            Text(
                text = "Make all arrows form a connected path.\nEach arrow should point to the next one in the chain.",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.82f),
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(28.dp))

            Text(
                text = "Tap anywhere to start",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.55f)
            )
        }
    }
}

// ── Win dialog ───────────────────────────────────────────────────────────────

@Composable
private fun WinDialog(
    levelId: Int,
    moves: Int,
    onNext: () -> Unit,
    onExit: () -> Unit
) {
    Dialog(
        onDismissRequest = onExit,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.45f))
                .padding(horizontal = 28.dp),
            contentAlignment = Alignment.Center
        ) {
            val cardState = remember { MutableTransitionState(false) }
            cardState.targetState = true

            AnimatedVisibility(
                visibleState = cardState,
                enter = fadeIn(tween(Motion.Normal)) +
                    scaleIn(initialScale = 0.85f, animationSpec = Motion.playful()),
                exit = fadeOut(tween(Motion.Quick)) + scaleOut(targetScale = 0.95f)
            ) {
                Surface(
                    shape = RoundedCornerShape(26.dp),
                    color = AppTheme.palette.surface,
                    shadowElevation = 24.dp
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "🎉",
                            style = MaterialTheme.typography.displayLarge,
                            modifier = Modifier.pulse(min = 0.92f, max = 1.1f, periodMillis = 1800)
                        )

                        Spacer(Modifier.height(16.dp))

                        Text(
                            text = "Level Complete!",
                            style = MaterialTheme.typography.headlineMedium,
                            color = AppTheme.palette.ink,
                            textAlign = TextAlign.Center
                        )

                        Spacer(Modifier.height(8.dp))

                        Text(
                            text = "Level $levelId solved in $moves moves",
                            style = MaterialTheme.typography.bodyLarge,
                            color = AppTheme.palette.inkMuted,
                            textAlign = TextAlign.Center
                        )

                        Spacer(Modifier.height(28.dp))

                        if (levelId < 6) {
                            PrimaryPillButton(
                                text = "Next Level",
                                onClick = onNext,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(Modifier.height(12.dp))
                        }

                        Text(
                            text = if (levelId < 6) "Back to levels" else "Done",
                            style = MaterialTheme.typography.titleMedium,
                            color = Blue500,
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable { onExit() }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

// ── Supporting composables ───────────────────────────────────────────────────
>>>>>>> e2e958806e5734d2b079726c6ebba9ed15f7b04c

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
<<<<<<< HEAD
            Icon(icon, null, tint = Blue500, modifier = Modifier.size(22.dp))
            if (badgeCount != null && badgeCount > 0) {
                Box(
                    Modifier.align(Alignment.TopEnd).offset(x = 8.dp, y = (-4).dp)
                        .size(16.dp).clip(CircleShape).background(Blue500),
                    contentAlignment = Alignment.Center
                ) { Text("$badgeCount", style = MaterialTheme.typography.labelSmall, color = Color.White) }
=======
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Blue500,
                modifier = Modifier.size(22.dp)
            )
            if (badgeCount != null && badgeCount > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 8.dp, y = (-4).dp)
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(Blue500),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$badgeCount",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White
                    )
                }
>>>>>>> e2e958806e5734d2b079726c6ebba9ed15f7b04c
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
                scaleIn(0.85f, animationSpec = Motion.playful()),
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
                scaleIn(0.85f, animationSpec = Motion.bouncy()),
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
