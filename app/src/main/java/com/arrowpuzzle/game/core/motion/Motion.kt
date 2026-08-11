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
    // Slowed down from the original vocabulary (Instant 90/Quick 160/Normal 260/
    // Slow 420/Ambient 900) — calmer motion also means fewer frames of work per
    // transition, which is part of what was making the app feel laggy.
    const val Instant = 120
    const val Quick = 220
    const val Normal = 360
    const val Slow = 560
    const val Ambient = 1200

    // Springs ---------------------------------------------------------------
    /** Buttons and taps: fast, barely any wobble. */
    fun <T> snappy(): FiniteAnimationSpec<T> = spring(
        dampingRatio = 0.9f,
        stiffness = Spring.StiffnessLow
    )

    /** Cards and sheets: gentle settle, overshoot dialed back so it doesn't wobble. */
    fun <T> bouncy(): FiniteAnimationSpec<T> = spring(
        dampingRatio = 0.78f,
        stiffness = 220f
    )

    /** Celebratory pops — stars, trophies, level complete. Still lively, less frantic. */
    fun <T> playful(): FiniteAnimationSpec<T> = spring(
        dampingRatio = 0.58f,
        stiffness = 200f
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
