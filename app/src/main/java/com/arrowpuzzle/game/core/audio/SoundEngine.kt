package com.arrowpuzzle.game.core.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.SoundPool
import com.arrowpuzzle.game.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.min
import kotlin.math.sin

/**
 * Game-feel audio.
 *
 * v3 — the three "big" feedback moments now play real recorded effects
 * (bundled as MP3 in `res/raw`) instead of synthesized tones:
 *  - **arrow tap that clears** → [playMove] → `sfx_arrow_click`
 *  - **wrong / blocked tap**   → [playError] → `sfx_wrong`
 *  - **level complete**        → [playComplete] → `sfx_level_complete`
 *
 * These load into a [SoundPool] once, in [init] (called from
 * `ArrowPuzzleApplication.onCreate`), so they fire with no perceptible
 * latency on tap. If, for any reason, the pool hasn't finished loading yet
 * (e.g. a very fast first frame), each call falls back to a synthesized
 * tone so the game is never silent.
 *
 * Small UI sounds that have no bundled asset — [playButton], [playHint],
 * [playRotate] — are still synthesized from raw PCM, as before.
 */
object SoundEngine {

    private const val SAMPLE_RATE = 22050
    private val scope = CoroutineScope(Dispatchers.Default)

    @Volatile
    private var enabled = true

    private val cache = mutableMapOf<String, ShortArray>()

    // ── Recorded effects ─────────────────────────────────────────────────────
    private var soundPool: SoundPool? = null
    private val clipIds = mutableMapOf<String, Int>()
    private val loadedClips = mutableSetOf<Int>()

    /** Call once, from `Application.onCreate()`. Loads the bundled MP3s. */
    fun init(context: Context) {
        if (soundPool != null) return
        val pool = SoundPool.Builder()
            .setMaxStreams(4)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .build()
        pool.setOnLoadCompleteListener { _, sampleId, status ->
            if (status == 0) loadedClips.add(sampleId)
        }
        val app = context.applicationContext
        clipIds["click"] = pool.load(app, R.raw.sfx_arrow_click, 1)
        clipIds["wrong"] = pool.load(app, R.raw.sfx_wrong, 1)
        clipIds["complete"] = pool.load(app, R.raw.sfx_level_complete, 1)
        soundPool = pool
    }

    fun setEnabled(value: Boolean) {
        enabled = value
    }

    /** Plays a loaded recorded clip. Returns false (and plays nothing) if the
     *  pool isn't ready yet, so the caller can fall back to a synth tone. */
    private fun playClip(name: String, volume: Float): Boolean {
        if (!enabled) return true // "handled" — caller should not also fall back while muted
        val pool = soundPool ?: return false
        val id = clipIds[name] ?: return false
        if (id !in loadedClips) return false
        pool.play(id, volume, volume, 1, 0, 1f)
        return true
    }

    fun playRotate() = play("rotate") { generateTick(880f, 0.045f) }

    /** Arrow tap that successfully escapes the board. */
    fun playMove() {
        if (!playClip("click", volume = 0.9f)) {
            play("move_fallback") { generateWhoosh(340f, 900f, 0.22f) }
        }
    }

    /** Kept for call-site compatibility; the tap-succeeded feedback is fully
     *  covered by [playMove]'s recorded click, so this no longer layers a
     *  second synthesized tone on top of it. */
    fun playCorrect() { /* no-op — see playMove() */ }

    /** Level cleared. */
    fun playComplete() {
        if (!playClip("complete", volume = 0.95f)) {
            play("complete_fallback") { generateArpeggio(listOf(523f, 659f, 784f), 0.10f) }
        }
    }

    /** Blocked / wrong tap. */
    fun playError() {
        if (!playClip("wrong", volume = 0.9f)) {
            play("error_fallback") { generateBuzz(196f, 0.16f) }
        }
    }

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

    // ── Synthesis helpers (fallback + small UI sounds only) ────────────────

    private fun warmTone(freq: Float, t: Float): Float {
        val fundamental = sin(2.0 * PI * freq * t).toFloat()
        val harmonic = sin(2.0 * PI * freq * 2.0 * t).toFloat() * 0.16f
        return (fundamental + harmonic) * 0.87f
    }

    private fun attackEnvelope(t: Float, attackSec: Float): Float =
        if (attackSec <= 0f) 1f else min(1f, t / attackSec)

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

    private fun generateWhoosh(
        freqStart: Float,
        freqEnd: Float,
        durationSec: Float,
        volume: Float = 0.22f
    ): ShortArray {
        val count = (SAMPLE_RATE * durationSec).toInt()
        val rng = kotlin.random.Random(1)
        val out = ShortArray(count)
        var phase = 0.0
        for (i in 0 until count) {
            val t = i.toFloat() / SAMPLE_RATE
            val progress = t / durationSec
            val envelope = sin((PI * progress).coerceIn(0.0, PI)).toFloat().let { it * it } * volume
            val freq = freqStart + (freqEnd - freqStart) * progress
            phase += 2.0 * PI * freq / SAMPLE_RATE
            val tone = sin(phase).toFloat()
            val noise = (rng.nextFloat() * 2f - 1f) * 0.22f
            val sample = (tone * 0.8f + noise) * envelope
            out[i] = (sample * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        val tick = generateTick(1200f, 0.035f, volume = 0.14f, decay = 22f)
        return out + tick
    }
}
