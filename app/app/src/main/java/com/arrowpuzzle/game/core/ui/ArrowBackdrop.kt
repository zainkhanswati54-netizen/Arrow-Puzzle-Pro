package com.arrowpuzzle.game.core.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import com.arrowpuzzle.game.core.design.LocalReducedMotion
import kotlin.math.sin

/**
 * The app's signature surface: oversized, softly rounded arrowheads drifting behind
 * the content, exactly as they appear on the launch screen of the reference.
 *
 * Cost control, because this sits under every screen:
 *  - paths are built once per size change, never per frame
 *  - a single infinite transition drives all shapes
 *  - the phase is read inside `drawBehind`, so nothing above it ever recomposes
 *  - it collapses to a static render when the OS asks for reduced motion
 */
@Immutable
private data class Blade(
    val xFraction: Float,
    val yFraction: Float,
    val sizeFraction: Float,
    val rotationDegrees: Float,
    val phase: Float,
    val drift: Float,
    val alpha: Float
)

private val DefaultBlades = listOf(
    Blade(0.46f, 0.08f, 0.62f, 24f, 0.00f, 1.0f, 0.55f),
    Blade(0.34f, 0.30f, 0.72f, 18f, 0.35f, 0.8f, 0.75f),
    Blade(0.62f, 0.72f, 0.66f, 32f, 0.62f, 1.2f, 0.45f),
    Blade(0.28f, 0.94f, 0.55f, 12f, 0.85f, 0.9f, 0.35f)
)

@Composable
fun ArrowBackdrop(
    modifier: Modifier = Modifier,
    tint: Color = Color(0xFFE7ECF3),
    periodMillis: Int = 14_000
) {
    val reduced = LocalReducedMotion.current
    val staticPhase = remember { mutableFloatStateOf(0.2f) }

    val phase: State<Float> = if (reduced) {
        staticPhase
    } else {
        rememberInfiniteTransition(label = "backdrop").animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(periodMillis, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "backdropPhase"
        )
    }

    // Path geometry only depends on the container size, so cache it across frames.
    val pathCache = remember { mutableMapOf<Int, Path>() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .drawBehind {
                val t = phase.value
                DefaultBlades.forEach { blade ->
                    val bladeSize = size.minDimension * blade.sizeFraction
                    val key = bladeSize.toInt()
                    val path = pathCache.getOrPut(key) { arrowheadPath(bladeSize) }

                    val bob = sin((t + blade.phase) * 2f * Math.PI.toFloat()) *
                        24f * blade.drift * density
                    val sway = sin((t + blade.phase) * 4f * Math.PI.toFloat()) *
                        10f * blade.drift * density

                    withTransform({
                        translate(
                            left = size.width * blade.xFraction - bladeSize / 2f + sway,
                            top = size.height * blade.yFraction - bladeSize / 2f + bob
                        )
                        rotate(
                            degrees = blade.rotationDegrees + sin(
                                (t + blade.phase) * 2f * Math.PI.toFloat()
                            ) * 3f,
                            pivot = Offset(bladeSize / 2f, bladeSize / 2f)
                        )
                    }) {
                        drawBlade(path, tint.copy(alpha = tint.alpha * blade.alpha), bladeSize)
                    }
                }
            }
    )
}

/** Fill plus a round-joined stroke — the cheapest way to get soft corners on a polygon. */
private fun DrawScope.drawBlade(path: Path, color: Color, bladeSize: Float) {
    val corner = bladeSize * 0.09f
    drawPath(path = path, color = color)
    drawPath(
        path = path,
        color = color,
        style = Stroke(width = corner, join = StrokeJoin.Round, cap = StrokeCap.Round)
    )
}

/** A wide, forward-leaning arrowhead — the mark used across the brand. */
private fun arrowheadPath(s: Float): Path = Path().apply {
    val w = s
    val h = s * 0.62f
    moveTo(w * 0.06f, h * 0.10f)
    lineTo(w * 0.94f, h * 0.46f)
    lineTo(w * 0.30f, h * 0.98f)
    lineTo(w * 0.40f, h * 0.52f)
    close()
}

/** Convenience: a backdrop sized to a specific area, e.g. a hero header. */
@Composable
fun ArrowBackdropScrim(
    modifier: Modifier = Modifier,
    tint: Color = Color.White.copy(alpha = 0.10f)
) = ArrowBackdrop(modifier = modifier, tint = tint, periodMillis = 18_000)
