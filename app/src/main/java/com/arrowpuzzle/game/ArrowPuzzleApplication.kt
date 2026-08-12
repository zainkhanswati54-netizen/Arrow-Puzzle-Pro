package com.arrowpuzzle.game

import android.app.Application
import com.arrowpuzzle.game.core.audio.SoundEngine

class ArrowPuzzleApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Preload the bundled sound effects (arrow click / wrong / level
        // complete) once at process start so the very first tap in the game
        // already has zero-latency audio available.
        SoundEngine.init(this)
    }
}
