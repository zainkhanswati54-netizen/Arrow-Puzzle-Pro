package com.arrowpuzzle.game.core.motion

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import com.arrowpuzzle.game.core.design.LocalReducedMotion
import kotlinx.coroutines.launch

/**
 * Animation helpers shared across screens.
 *
 * Every one of these writes its animated value inside a `graphicsLayer { }` or
 * `drawWithCache { }` lambda. That matters: reading an animation State inside those
 * blocks skips recomposition and re-layout entirely and only re-runs the draw pass,
 * which is what keeps 120 Hz devices at 120 Hz while several things move at once.
 */

/** Squashes a control while it is held. The whole app's tap feel comes from here. */
@Composable
fun Modifier.pressScale(
    interactionSource: InteractionSource,
    pressedScale: Float = 0.955f
): Modifier {
    val reduced = LocalReducedMotion.current
    val scale = remember { Animatable(1f) }

    LaunchedEffect(interactionSource, reduced) {
        val scope = this
        val active = mutableListOf<PressInteraction.Press>()
        interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is PressInteraction.Press -> active += interaction
                is PressInteraction.Release -> active -= interaction.press
                is PressInteraction.Cancel -> active -= interaction.press
            }
            val target = if (active.isNotEmpty()) pressedScale else 1f
            scope.launch {
                scale.animateTo(target, Motion.respecting(reduced, Motion.snappy()))
            }
        }
    }

    return graphicsLayer {
        scaleX = scale.value
        scaleY = scale.value
    }
}

/**
 * Entrance for list and grid items: fades up into place after [delayMillis].
 * Returns a modifier rather than wrapping in AnimatedVisibility so the item is
 * measured once and only its layer is animated.
 */
@Composable
fun Modifier.enterFromBelow(
    delayMillis: Int = 0,
    travel: Float = 26f,
    key: Any? = Unit
): Modifier {
    val reduced = LocalReducedMotion.current
    val progress = remember(key) { Animatable(if (reduced) 1f else 0f) }

    LaunchedEffect(key, reduced) {
        if (reduced) {
            progress.snapTo(1f)
        } else {
            progress.snapTo(0f)
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = Motion.Slow,
                    delayMillis = delayMillis,
                    easing = Motion.Emphasized
                )
            )
        }
    }

    return graphicsLayer {
        alpha = progress.value
        translationY = (1f - progress.value) * travel * density
        val s = 0.96f + 0.04f * progress.value
        scaleX = s
        scaleY = s
    }
}

/** Slow vertical drift for hero art. Amplitude stays small so it reads as depth. */
@Composable
fun Modifier.gentleFloat(
    amplitudeDp: Float = 6f,
    periodMillis: Int = 3800,
    phase: Float = 0f
): Modifier {
    val reduced = LocalReducedMotion.current
    if (reduced) return this

    val transition = rememberInfiniteTransition(label = "float")
    val t = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(periodMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "floatPhase"
    )

    return graphicsLayer {
        val radians = (t.value + phase) * 2f * Math.PI.toFloat()
        translationY = kotlin.math.sin(radians) * amplitudeDp * density
    }
}

/** Breathing scale used on trophies, stars and empty-state art. */
@Composable
fun Modifier.pulse(
    min: Float = 0.98f,
    max: Float = 1.03f,
    periodMillis: Int = 2400
): Modifier {
    val reduced = LocalReducedMotion.current
    if (reduced) return this

    val transition = rememberInfiniteTransition(label = "pulse")
    val scale = transition.animateFloat(
        initialValue = min,
        targetValue = max,
        animationSpec = infiniteRepeatable(
            animation = tween(periodMillis, easing = Motion.Standard),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    return graphicsLayer {
        scaleX = scale.value
        scaleY = scale.value
    }
}

/**
 * Short horizontal "no!" shake — fires once whenever [trigger] changes to a
 * new non-null value. Used on a blocked/wrong tap, mirroring the competitor
 * reference's shake-and-flash-red feedback instead of a plain static flash.
 */
@Composable
fun Modifier.shakeOnce(trigger: Any?): Modifier {
    val reduced = LocalReducedMotion.current
    val offset = remember { Animatable(0f) }

    LaunchedEffect(trigger) {
        if (trigger == null || reduced) return@LaunchedEffect
        // Fast damped back-and-forth: right, left, smaller right, settle.
        offset.snapTo(0f)
        offset.animateTo(1f, tween(50, easing = LinearEasing))
        offset.animateTo(-1f, tween(70, easing = LinearEasing))
        offset.animateTo(0.5f, tween(60, easing = LinearEasing))
        offset.animateTo(0f, tween(60, easing = LinearEasing))
    }

    return graphicsLayer {
        translationX = offset.value * 8f * density
    }
}

/** Diagonal light sweep. Signals "this surface is a placeholder, not a dead pixel". */
@Composable
fun Modifier.shimmer(
    highlight: Color = Color.White.copy(alpha = 0.55f),
    periodMillis: Int = 1600
): Modifier {
    val reduced = LocalReducedMotion.current
    if (reduced) return this

    val transition = rememberInfiniteTransition(label = "shimmer")
    val progress: State<Float> = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(periodMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerSweep"
    )

    return drawWithCache {
        val band = size.width * 0.45f
        onDrawWithContent {
            drawContent()
            val travel = size.width + band * 2f
            val x = -band + travel * progress.value
            drawRect(
                brush = Brush.linearGradient(
                    colors = listOf(Color.Transparent, highlight, Color.Transparent),
                    start = Offset(x, 0f),
                    end = Offset(x + band, size.height)
                )
            )
        }
    }
}
