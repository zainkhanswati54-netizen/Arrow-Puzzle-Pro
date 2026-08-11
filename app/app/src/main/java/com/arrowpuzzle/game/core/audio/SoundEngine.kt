package com.arrowpuzzle.game.core.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * Synthesises game-feel sound effects from raw PCM — no audio assets required.
 * Every sound is generated once and cached as a short [AudioTrack] that can fire
 * at any time without blocking the UI thread.
 *
 * Sounds are designed to feel clean and tactile rather than musical:
 *  - **rotate**: a crisp 50ms click-tick at 880Hz
 *  - **correct**: a bright ascending two-note ping (C5→E5)
 *  - **complete**: a satisfying three-note arpeggio (C5→E5→G5)
 *  - **error**: a short low buzz at 220Hz
 *  - **button**: a soft UI tap at 660Hz
 *  - **hint**: a gentle descending note (E5→C5)
 */
object SoundEngine {

    private const val SAMPLE_RATE = 22050
    private val scope = CoroutineScope(Dispatchers.Default)

    @Volatile
    private var enabled = true

    private val cache = mutableMapOf<String, ShortArray>()

    fun setEnabled(value: Boolean) {
        enabled = value
    }

    fun playRotate() = play("rotate") { generateTick(880f, 0.045f) }
    fun playCorrect() = play("correct") { generatePing(523f, 659f, 0.12f) }
    fun playComplete() = play("complete") { generateArpeggio(listOf(523f, 659f, 784f), 0.10f) }
    fun playError() = play("error") { generateTick(220f, 0.08f, decay = 6f) }
    fun playButton() = play("button") { generateTick(660f, 0.03f) }
    fun playHint() = play("hint") { generatePing(659f, 523f, 0.10f) }

    private fun play(key: String, generator: () -> ShortArray) {
        if (!enabled) return
        scope.launch {
            try {
                val samples = cache.getOrPut(key) { generator() }
                val track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(SAMPLE_RATE)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(samples.size * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                track.write(samples, 0, samples.size)
                track.setNotificationMarkerPosition(samples.size)
                track.setPlaybackPositionUpdateListener(object :
                    AudioTrack.OnPlaybackPositionUpdateListener {
                    override fun onMarkerReached(t: AudioTrack) { t.release() }
                    override fun onPeriodicNotification(t: AudioTrack) {}
                })
                track.play()
            } catch (_: Exception) {
                // Audio failures must never crash the game.
            }
        }
    }

    // ── Synthesis helpers ────────────────────────────────────────────────────

    /** A single percussive tone with exponential decay. */
    private fun generateTick(
        freq: Float,
        durationSec: Float,
        volume: Float = 0.35f,
        decay: Float = 12f
    ): ShortArray {
        val count = (SAMPLE_RATE * durationSec).toInt()
        val out = ShortArray(count)
        for (i in 0 until count) {
            val t = i.toFloat() / SAMPLE_RATE
            val envelope = exp(-decay * t) * volume
            val sample = sin(2.0 * PI * freq * t).toFloat() * envelope
            out[i] = (sample * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return out
    }

    /** Two-note ascending or descending ping. */
    private fun generatePing(
        freq1: Float,
        freq2: Float,
        noteDuration: Float,
        volume: Float = 0.30f
    ): ShortArray {
        val noteLen = (SAMPLE_RATE * noteDuration).toInt()
        val total = noteLen * 2
        val out = ShortArray(total)
        for (i in 0 until total) {
            val t = (i % noteLen).toFloat() / SAMPLE_RATE
            val freq = if (i < noteLen) freq1 else freq2
            val envelope = exp(-8f * t) * volume
            val sample = sin(2.0 * PI * freq * t).toFloat() * envelope
            out[i] = (sample * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return out
    }

    /** Multi-note arpeggio for level completion. */
    private fun generateArpeggio(
        freqs: List<Float>,
        noteDuration: Float,
        volume: Float = 0.32f
    ): ShortArray {
        val noteLen = (SAMPLE_RATE * noteDuration).toInt()
        val total = noteLen * freqs.size
        val out = ShortArray(total)
        for (i in 0 until total) {
            val noteIndex = i / noteLen
            val t = (i % noteLen).toFloat() / SAMPLE_RATE
            val freq = freqs[noteIndex]
            val envelope = exp(-6f * t) * volume
            val sample = sin(2.0 * PI * freq * t).toFloat() * envelope
            out[i] = (sample * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return out
    }
}
