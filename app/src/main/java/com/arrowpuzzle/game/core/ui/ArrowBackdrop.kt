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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
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

// Kept inside the middle of the canvas and sized down a bit so nothing gets
// cropped awkwardly at the screen edges — that clipped, floating-arrow look was
// the "unusual loading screen" issue.
private val DefaultBlades = listOf(
    Blade(0.50f, 0.22f, 0.40f, 24f, 0.00f, 0.6f, 0.35f),
    Blade(0.30f, 0.42f, 0.46f, 18f, 0.35f, 0.5f, 0.45f),
    Blade(0.68f, 0.62f, 0.42f, 32f, 0.62f, 0.7f, 0.30f)
)

@Composable
fun ArrowBackdrop(
    modifier: Modifier = Modifier,
    tint: Color = Color(0xFFE7ECF3),
    periodMillis: Int = 20_000
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
            .clipToBounds()
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

/** Fill only — a redundant stroke pass on top of the fill was doubling the paint
 *  work every frame for no visible gain, since the fill already covers the shape. */
private fun DrawScope.drawBlade(path: Path, color: Color, bladeSize: Float) {
    drawPath(path = path, color = color)
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
