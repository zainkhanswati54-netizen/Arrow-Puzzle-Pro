package com.arrowpuzzle.game.core.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import com.arrowpuzzle.game.core.design.LocalReducedMotion
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Slow rotating light rays radiating from the centre — the "sunburst" behind
 * the competitor's Level Completed card. Purely decorative, cheap to draw
 * (one Canvas pass, no per-frame allocation).
 */
@Composable
fun SunburstBackground(modifier: Modifier = Modifier, color: Color = Color.White.copy(alpha = 0.08f)) {
    val reduced = LocalReducedMotion.current
    val transition = rememberInfiniteTransition(label = "sunburst")
    val angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = if (reduced) 0f else 360f,
        animationSpec = infiniteRepeatable(tween(24000, easing = LinearEasing)),
        label = "sunburstAngle"
    )
    val rays = 20
    Canvas(modifier.fillMaxSize()) {
        val cx = size.width / 2f; val cy = size.height * 0.32f
        val maxR = size.width + size.height
        rotate(angle, pivot = Offset(cx, cy)) {
            for (i in 0 until rays) {
                val a0 = (i.toFloat() / rays) * 2f * PI.toFloat()
                val sweep = (PI.toFloat() / rays) * 0.55f
                val p1 = Offset(cx + cos(a0) * maxR, cy + sin(a0) * maxR)
                val p2 = Offset(cx + cos(a0 + sweep) * maxR, cy + sin(a0 + sweep) * maxR)
                if (i % 2 == 0) {
                    drawPath(
                        path = androidx.compose.ui.graphics.Path().apply {
                            moveTo(cx, cy); lineTo(p1.x, p1.y); lineTo(p2.x, p2.y); close()
                        },
                        color = color
                    )
                }
            }
        }
    }
}

private data class ConfettiPiece(
    val x0: Float,
    val phase: Float,
    val fallSpeed: Float,
    val driftAmp: Float,
    val driftFreq: Float,
    val rotSpeed: Float,
    val size: Float,
    val color: Color,
    val elongated: Boolean
)

private val ConfettiColors = listOf(
    Color(0xFFFFC93C), Color(0xFFFF6B81), Color(0xFF37D5A0),
    Color(0xFF4A9BFF), Color(0xFFB78CFF), Color(0xFFFFFFFF)
)

/** Looping falling confetti across the full size of the composable. Purely
 *  decorative — cheap enough to run for the lifetime of a win dialog. */
@Composable
fun ConfettiOverlay(modifier: Modifier = Modifier, pieceCount: Int = 60) {
    val reduced = LocalReducedMotion.current
    if (reduced) return

    val pieces = remember(pieceCount) {
        val rng = Random(42)
        List(pieceCount) {
            ConfettiPiece(
                x0 = rng.nextFloat(),
                phase = rng.nextFloat(),
                fallSpeed = 0.7f + rng.nextFloat() * 0.6f,
                driftAmp = 14f + rng.nextFloat() * 22f,
                driftFreq = 1.2f + rng.nextFloat() * 1.6f,
                rotSpeed = (if (rng.nextBoolean()) 1f else -1f) * (180f + rng.nextFloat() * 360f),
                size = 6f + rng.nextFloat() * 7f,
                color = ConfettiColors[rng.nextInt(ConfettiColors.size)],
                elongated = rng.nextBoolean()
            )
        }
    }
    val transition = rememberInfiniteTransition(label = "confetti")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(3200, easing = LinearEasing)),
        label = "confettiT"
    )

    Canvas(modifier.fillMaxSize()) {
        val h = size.height; val w = size.width
        for (p in pieces) {
            val fall = ((t * p.fallSpeed + p.phase) % 1f)
            val y = -40f + fall * (h + 80f)
            val x = p.x0 * w + sin((fall * p.driftFreq + p.phase) * 2f * PI.toFloat()) * p.driftAmp
            val rot = (t * p.rotSpeed + p.phase * 360f) % 360f
            val alpha = (1f - fall).coerceIn(0.25f, 1f)
            translate(x, y) {
                rotate(rot, pivot = Offset.Zero) {
                    val w2 = if (p.elongated) p.size * 0.45f else p.size * 0.5f
                    val h2 = if (p.elongated) p.size * 1.15f else p.size * 0.5f
                    drawRect(
                        color = p.color.copy(alpha = p.color.alpha * alpha),
                        topLeft = Offset(-w2, -h2),
                        size = androidx.compose.ui.geometry.Size(w2 * 2, h2 * 2)
                    )
                }
            }
        }
    }
}
