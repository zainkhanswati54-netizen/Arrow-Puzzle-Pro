package com.arrowpuzzle.game.feature.game

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.Undo
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.arrowpuzzle.game.core.design.AppTheme
import com.arrowpuzzle.game.core.design.Blue500
import com.arrowpuzzle.game.core.design.Ink
import com.arrowpuzzle.game.core.design.Red500
import com.arrowpuzzle.game.core.motion.Motion
import com.arrowpuzzle.game.core.motion.enterFromBelow
import com.arrowpuzzle.game.core.motion.pressScale
import com.arrowpuzzle.game.core.ui.ArrowBackdrop
import com.arrowpuzzle.game.core.ui.AppTopBar
import com.arrowpuzzle.game.core.ui.ComingSoonBadge
import com.arrowpuzzle.game.navigation.GameMode

/**
 * The play surface. Board *logic* is deliberately out of scope for this build, but
 * the board itself is fully rendered — geometry, spacing, tap targets, and the
 * feedback loop are all real, so wiring the solver in later is a drop-in.
 *
 * The two things this screen proves out ahead of logic:
 *  1. Tap targets. The most common complaint about the games on the store is
 *     mis-taps on crowded boards, so every cell keeps a 48dp touch slop even when
 *     the drawn glyph is smaller.
 *  2. Feedback latency. Taps nudge the arrow immediately on the same frame rather
 *     than waiting for a rules check, which is what makes a puzzle game feel tight.
 */

enum class Direction(val dx: Int, val dy: Int, val degrees: Float) {
    Up(0, -1, -90f),
    Right(1, 0, 0f),
    Down(0, 1, 90f),
    Left(-1, 0, 180f)
}

@Immutable
data class ArrowPiece(val row: Int, val column: Int, val direction: Direction)

private const val GridSize = 6

/** A hand-placed sample board — enough to show real density and spacing. */
private val SampleBoard = listOf(
    ArrowPiece(0, 1, Direction.Down),
    ArrowPiece(0, 3, Direction.Right),
    ArrowPiece(0, 4, Direction.Down),
    ArrowPiece(1, 0, Direction.Right),
    ArrowPiece(1, 2, Direction.Up),
    ArrowPiece(1, 5, Direction.Left),
    ArrowPiece(2, 1, Direction.Right),
    ArrowPiece(2, 3, Direction.Down),
    ArrowPiece(2, 4, Direction.Left),
    ArrowPiece(3, 0, Direction.Up),
    ArrowPiece(3, 2, Direction.Right),
    ArrowPiece(3, 5, Direction.Up),
    ArrowPiece(4, 1, Direction.Up),
    ArrowPiece(4, 3, Direction.Left),
    ArrowPiece(4, 4, Direction.Up),
    ArrowPiece(5, 0, Direction.Right),
    ArrowPiece(5, 2, Direction.Up),
    ArrowPiece(5, 4, Direction.Left)
)

@Composable
fun GameScreen(
    mode: String,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
    level: Int = 2,
    lives: Int = 3
) {
    val palette = AppTheme.palette
    val haptics = LocalHapticFeedback.current
    var toastKey by remember { mutableIntStateOf(0) }
    var toastVisible by remember { mutableStateOf(false) }

    val title = when (mode) {
        GameMode.Daily -> "Daily Challenge"
        GameMode.Tournament -> "Tournament"
        else -> "Level $level"
    }

    LaunchedEffect(toastKey) {
        if (toastKey > 0) {
            toastVisible = true
            kotlinx.coroutines.delay(1800)
            toastVisible = false
        }
    }

    val nudge: () -> Unit = {
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        toastKey++
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(palette.canvas)
    ) {
        ArrowBackdrop(tint = Color(0xFFE8EDF4))

        Column(Modifier.fillMaxSize()) {
            AppTopBar(
                title = title,
                onBack = onExit,
                trailing = { LivesIndicator(lives = lives) }
            )

            Spacer(Modifier.height(8.dp))

            Board(
                pieces = SampleBoard,
                onPieceTap = { nudge() },
                modifier = Modifier
                    .enterFromBelow(delayMillis = Motion.stagger(0), travel = 20f)
                    .padding(horizontal = 20.dp)
            )

            Spacer(Modifier.height(20.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .enterFromBelow(delayMillis = Motion.stagger(2))
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ToolButton(
                    label = "Hint",
                    icon = Icons.Rounded.Lightbulb,
                    onClick = nudge,
                    modifier = Modifier.weight(1f)
                )
                ToolButton(
                    label = "Undo",
                    icon = Icons.Rounded.Undo,
                    onClick = nudge,
                    modifier = Modifier.weight(1f)
                )
                ToolButton(
                    label = "Shuffle",
                    icon = Icons.Rounded.Shuffle,
                    onClick = nudge,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.weight(1f))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 28.dp),
                contentAlignment = Alignment.Center
            ) {
                ComingSoonBadge(
                    text = "Board logic coming soon",
                    modifier = Modifier.enterFromBelow(delayMillis = Motion.stagger(3))
                )
            }
        }

        // Transient feedback. Sits above everything and never blocks a tap.
        AnimatedVisibility(
            visible = toastVisible,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 84.dp),
            enter = slideInVertically(Motion.bouncy()) { it } +
                fadeIn(Motion.smooth(Motion.Quick)),
            exit = slideOutVertically(Motion.smooth(Motion.Normal)) { it } +
                fadeOut(Motion.smooth(Motion.Quick))
        ) {
            Box(
                Modifier
                    .shadow(12.dp, CircleShape)
                    .clip(CircleShape)
                    .background(Ink)
                    .padding(horizontal = 18.dp, vertical = 10.dp)
            ) {
                Text(
                    text = "Moves land once the rules engine ships",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun Board(
    pieces: List<ArrowPiece>,
    onPieceTap: (ArrowPiece) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = AppTheme.palette

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
            val cell = maxWidth / GridSize

            // Cell wells first, so arrows always render on top of the grid.
            Column {
                repeat(GridSize) {
                    Row {
                        repeat(GridSize) { _ ->
                            Box(
                                Modifier
                                    .size(cell)
                                    .padding(3.dp)
                                    .clip(RoundedCornerShape(9.dp))
                                    .background(palette.canvasSunken.copy(alpha = 0.55f))
                            )
                        }
                    }
                }
            }

            pieces.forEach { piece ->
                ArrowTile(
                    piece = piece,
                    onTap = { onPieceTap(piece) },
                    modifier = Modifier
                        .offset(x = cell * piece.column, y = cell * piece.row)
                        .size(cell)
                )
            }
        }
    }
}

@Composable
private fun ArrowTile(
    piece: ArrowPiece,
    onTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val reduced = AppTheme.reducedMotion
    val nudge = remember { Animatable(0f) }
    var tapCount by remember { mutableIntStateOf(0) }

    // A tap always produces motion in the arrow's own direction, immediately.
    // When the rules engine arrives this becomes either a slide-off or a refusal.
    LaunchedEffect(tapCount) {
        if (tapCount > 0 && !reduced) {
            nudge.animateTo(1f, Motion.snappy())
            nudge.animateTo(0f, Motion.bouncy())
        }
    }

    Box(
        modifier = modifier
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button
            ) {
                tapCount++
                onTap()
            }
            .pressScale(interactionSource, pressedScale = 0.9f)
            .graphicsLayer {
                val travel = 6f * density * nudge.value
                translationX = piece.direction.dx * travel
                translationY = piece.direction.dy * travel
            }
            .padding(6.dp),
        contentAlignment = Alignment.Center
    ) {
        ArrowGlyph(
            direction = piece.direction,
            color = Ink,
            modifier = Modifier.fillMaxSize()
        )
    }
}

/** A single arrow, drawn as a stroked path so it stays crisp at any cell size. */
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
                val strokeWidth = w * 0.16f
                val tipX = w * 0.86f
                val midY = h * 0.5f
                val headSpan = h * 0.26f

                val shaft = Path().apply {
                    moveTo(w * 0.14f, midY)
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

@Composable
private fun LivesIndicator(lives: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(3) { index ->
            Icon(
                imageVector = Icons.Rounded.Favorite,
                contentDescription = null,
                tint = if (index < lives) Red500 else Red500.copy(alpha = 0.22f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun ToolButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = AppTheme.palette
    val interactionSource = remember { MutableInteractionSource() }

    Column(
        modifier = modifier
            .pressScale(interactionSource, pressedScale = 0.94f)
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = Ink.copy(alpha = 0.08f),
                spotColor = Ink.copy(alpha = 0.12f)
            )
            .clip(RoundedCornerShape(16.dp))
            .background(palette.surface)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = onClick
            )
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Blue500,
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = palette.inkSoft
        )
    }
}
