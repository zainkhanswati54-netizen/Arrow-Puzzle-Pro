package com.arrowpuzzle.game.core.motion

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween

/**
 * One motion vocabulary for the whole app. Screens never write a raw `tween(300)` —
 * they pick an intent from here, so timing stays consistent as features land.
 *
 * The curves are asymmetric on purpose: things enter with a little overshoot
 * (springs) and leave on a fast ease-in, which is what makes an interface read as
 * responsive rather than merely animated.
 */
object Motion {

    // Easings ---------------------------------------------------------------
    /** Standard in-and-out for anything that both appears and moves. */
    val Standard: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

    /** Content arriving on screen — decelerates hard, feels "caught". */
    val Emphasized: Easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)

    /** Content leaving — accelerates away so it never lingers. */
    val Exit: Easing = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)

    // Durations (ms) --------------------------------------------------------
    const val Instant = 90
    const val Quick = 160
    const val Normal = 260
    const val Slow = 420
    const val Ambient = 900

    // Springs ---------------------------------------------------------------
    /** Buttons and taps: fast, barely any wobble. */
    fun <T> snappy(): FiniteAnimationSpec<T> = spring(
        dampingRatio = 0.82f,
        stiffness = Spring.StiffnessMedium
    )

    /** Cards and sheets: a touch of overshoot to feel physical. */
    fun <T> bouncy(): FiniteAnimationSpec<T> = spring(
        dampingRatio = 0.62f,
        stiffness = 380f
    )

    /** Celebratory pops — stars, trophies, level complete. */
    fun <T> playful(): FiniteAnimationSpec<T> = spring(
        dampingRatio = 0.45f,
        stiffness = 320f
    )

    /** Large surfaces where a spring would look nervous. */
    fun <T> smooth(durationMillis: Int = Normal): FiniteAnimationSpec<T> =
        tween(durationMillis, easing = Emphasized)

    /** Collapses every spec above to a single frame when the OS asks for it. */
    fun <T> respecting(reducedMotion: Boolean, spec: FiniteAnimationSpec<T>): FiniteAnimationSpec<T> =
        if (reducedMotion) tween(0) else spec

    // Staggering ------------------------------------------------------------
    /** Per-item delay for list entrances, capped so long lists never feel slow. */
    fun stagger(index: Int, step: Int = 45, max: Int = 320): Int =
        (index * step).coerceAtMost(max)
}
