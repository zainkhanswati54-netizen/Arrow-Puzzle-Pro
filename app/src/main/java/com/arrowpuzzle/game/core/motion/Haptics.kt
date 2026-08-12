package com.arrowpuzzle.game.core.motion

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * Talks to the platform vibrator directly instead of going through Compose's
 * [androidx.compose.ui.hapticfeedback.HapticFeedback] — that API only exposes
 * `LongPress`/`TextHandleMove` on the Compose version this app is pinned to,
 * which is why every tap used to feel identical. This gives each outcome its
 * own distinct pulse:
 *
 *  - [tapCorrect] — a single light tick, fired on every arrow that escapes.
 *  - [tapWrong]   — a firmer double-buzz, so a blocked tap reads as a "no"
 *                   through touch alone, not just the red flash.
 *  - [levelComplete] — a short escalating pattern, reserved for the win beat.
 *  - [tapButton]  — the lightest possible click, for plain UI buttons.
 *
 * All calls are no-ops on devices without a vibrator and never throw.
 */
object Haptics {

    private var vibrator: Vibrator? = null

    fun init(context: Context) {
        if (vibrator != null) return
        vibrator = try {
            if (Build.VERSION.SDK_INT >= 31) {
                val manager = context.getSystemService(VibratorManager::class.java)
                manager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
        } catch (_: Exception) {
            null
        }
    }

    /** Light, quick tick — an arrow escaping the board. */
    fun tapCorrect() = predefined(VibrationEffect.EFFECT_TICK, fallbackMs = 12)

    /** Firmer double pulse — a blocked/wrong tap. */
    fun tapWrong() = pattern(longArrayOf(0, 22, 40, 22))

    /** Light click — ordinary buttons (hint, retry, settings, etc). */
    fun tapButton() = predefined(VibrationEffect.EFFECT_CLICK, fallbackMs = 10)

    /** Short celebratory ramp — fired once when a level completes. */
    fun levelComplete() = pattern(longArrayOf(0, 18, 45, 18, 45, 45))

    private fun predefined(effect: Int, fallbackMs: Long) {
        val v = vibrator ?: return
        try {
            if (Build.VERSION.SDK_INT >= 29) {
                v.vibrate(VibrationEffect.createPredefined(effect))
            } else {
                legacyOneShot(v, fallbackMs)
            }
        } catch (_: Exception) {
            // Never let haptics crash gameplay.
        }
    }

    private fun pattern(timings: LongArray) {
        val v = vibrator ?: return
        try {
            if (Build.VERSION.SDK_INT >= 26) {
                v.vibrate(VibrationEffect.createWaveform(timings, -1))
            } else {
                @Suppress("DEPRECATION")
                v.vibrate(timings, -1)
            }
        } catch (_: Exception) {
        }
    }

    private fun legacyOneShot(v: Vibrator, ms: Long) {
        if (Build.VERSION.SDK_INT >= 26) {
            v.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            v.vibrate(ms)
        }
    }
}
