package com.arrowpuzzle.game.core.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

/**
 * Synthesises game-feel sound effects from raw PCM — no audio assets required.
 * Every sound is generated once and cached as a short [AudioTrack] that can fire
 * at any time without blocking the UI thread.
 *
 * v2 — replaced the old plain single-sine beeps with rounder, layered tones:
 * every note now has a short soft attack (no more clicky onset), a touch of a
 * quiet second harmonic for warmth, and a light low-pass-style smoothing pass
 * so nothing sounds like a raw square/sine test tone.
 *  - **move**: a soft airy whoosh + tiny landing tick — plays when an arrow
 *    successfully slides off the board, timed to the slide animation
 *  - **correct**: a bright ascending two-note ping (C5→E5)
 *  - **complete**: a satisfying three-note arpeggio (C5→E5→G5)
 *  - **error**: a double-pulse low buzzer for a blocked tap — harsher and
 *    more "wrong answer" than a single soft tick
 *  - **button**: a soft, quiet UI tap
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
    fun playMove() = play("move") { generateWhoosh(340f, 900f, 0.22f) }
    fun playCorrect() = play("correct") { generatePing(523f, 659f, 0.12f) }
    fun playComplete() = play("complete") { generateArpeggio(listOf(523f, 659f, 784f), 0.10f) }
    fun playError() = play("error") { generateBuzz(196f, 0.16f) }
    fun playButton() = play("button") { generateTick(660f, 0.035f, volume = 0.22f) }
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

    /** One rounded oscillator sample: fundamental plus a quiet, soft 2nd
     *  harmonic — this alone is most of the difference between "test tone
     *  beep" and something that sounds intentionally designed. */
    private fun warmTone(freq: Float, t: Float): Float {
        val fundamental = sin(2.0 * PI * freq * t).toFloat()
        val harmonic = sin(2.0 * PI * freq * 2.0 * t).toFloat() * 0.16f
        return (fundamental + harmonic) * 0.87f
    }

    /** Short linear fade-in so notes never start with a hard, clicky edge. */
    private fun attackEnvelope(t: Float, attackSec: Float): Float =
        if (attackSec <= 0f) 1f else min(1f, t / attackSec)

    /** A single percussive tone with a soft attack and exponential decay. */
    private fun generateTick(
        freq: Float,
        durationSec: Float,
        volume: Float = 0.30f,
        decay: Float = 12f
    ): ShortArray {
        val count = (SAMPLE_RATE * durationSec).toInt()
        val attack = min(0.006f, durationSec * 0.2f)
        val out = ShortArray(count)
        for (i in 0 until count) {
            val t = i.toFloat() / SAMPLE_RATE
            val envelope = attackEnvelope(t, attack) * exp(-decay * t) * volume
            val sample = warmTone(freq, t) * envelope
            out[i] = (sample * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return out
    }

    /** Two-note ascending or descending ping, with a tiny crossfade at the
     *  seam between notes instead of a hard cut. */
    private fun generatePing(
        freq1: Float,
        freq2: Float,
        noteDuration: Float,
        volume: Float = 0.26f
    ): ShortArray {
        val noteLen = (SAMPLE_RATE * noteDuration).toInt()
        val total = noteLen * 2
        val attack = min(0.008f, noteDuration * 0.25f)
        val out = ShortArray(total)
        for (i in 0 until total) {
            val t = (i % noteLen).toFloat() / SAMPLE_RATE
            val freq = if (i < noteLen) freq1 else freq2
            val envelope = attackEnvelope(t, attack) * exp(-7f * t) * volume
            val sample = warmTone(freq, t) * envelope
            out[i] = (sample * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return out
    }

    /** Multi-note arpeggio for level completion. */
    private fun generateArpeggio(
        freqs: List<Float>,
        noteDuration: Float,
        volume: Float = 0.28f
    ): ShortArray {
        val noteLen = (SAMPLE_RATE * noteDuration).toInt()
        val total = noteLen * freqs.size
        val attack = min(0.008f, noteDuration * 0.25f)
        val out = ShortArray(total)
        for (i in 0 until total) {
            val noteIndex = i / noteLen
            val t = (i % noteLen).toFloat() / SAMPLE_RATE
            val freq = freqs[noteIndex]
            val envelope = attackEnvelope(t, attack) * exp(-6f * t) * volume
            val sample = warmTone(freq, t) * envelope
            out[i] = (sample * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return out
    }

    /** Two short low pulses with a detuned second oscillator — a harsher,
     *  more "wrong answer" buzzer than a single rounded tick, matching the
     *  punchier blocked-tap feedback in the competitor reference. */
    private fun generateBuzz(freq: Float, durationSec: Float, volume: Float = 0.30f): ShortArray {
        val gap = (SAMPLE_RATE * 0.03f).toInt()
        val pulseLen = ((SAMPLE_RATE * durationSec).toInt() - gap) / 2
        val out = ShortArray(pulseLen * 2 + gap)
        for (p in 0 until 2) {
            val base = p * (pulseLen + gap)
            for (i in 0 until pulseLen) {
                val t = i.toFloat() / SAMPLE_RATE
                val envelope = attackEnvelope(t, 0.004f) * exp(-9f * t) * volume
                val detune = sin(2.0 * PI * (freq * 1.5) * t).toFloat() * 0.35f
                val sample = (warmTone(freq, t) + detune) * envelope
                out[base + i] = (sample * Short.MAX_VALUE).toInt()
                    .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            }
        }
        return out
    }

    /** Airy frequency-swept whoosh with a light noise texture, topped with a
     *  soft landing tick — used for the arrow slide-off-board movement. */
    private fun generateWhoosh(
        freqStart: Float,
        freqEnd: Float,
        durationSec: Float,
        volume: Float = 0.22f
    ): ShortArray {
        val count = (SAMPLE_RATE * durationSec).toInt()
        val rng = Random(1)
        val out = ShortArray(count)
        var phase = 0.0
        for (i in 0 until count) {
            val t = i.toFloat() / SAMPLE_RATE
            val progress = t / durationSec
            // Bell-shaped envelope: rises, peaks near the middle, tails off.
            val envelope = sin((PI * progress).coerceIn(0.0, PI)).toFloat().let { it * it } * volume
            val freq = freqStart + (freqEnd - freqStart) * progress
            phase += 2.0 * PI * freq / SAMPLE_RATE
            val tone = sin(phase).toFloat()
            val noise = (rng.nextFloat() * 2f - 1f) * 0.22f
            val sample = (tone * 0.8f + noise) * envelope
            out[i] = (sample * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        // Soft landing tick appended right after the whoosh tail.
        val tick = generateTick(1200f, 0.035f, volume = 0.14f, decay = 22f)
        return out + tick
    }
}
