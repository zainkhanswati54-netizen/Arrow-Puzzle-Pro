package com.arrowpuzzle.game

import android.app.Application
import com.arrowpuzzle.game.core.ads.AdManager
import com.arrowpuzzle.game.core.audio.SoundEngine
import com.arrowpuzzle.game.core.notifications.NotificationChannels
import com.arrowpuzzle.game.core.notifications.ReminderScheduler

class ArrowPuzzleApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Preload the bundled sound effects (arrow click / wrong / level
        // complete) once at process start so the very first tap in the game
        // already has zero-latency audio available.
        SoundEngine.init(this)

        // Ads are initialized here so the very first interstitial/rewarded
        // request already has a warm SDK behind it; actual consent (UMP) is
        // requested from MainActivity where an Activity is available.
        AdManager.init(this)

        // Arms the twice-daily "come back and play" reminders. Harmless to
        // call on every process start — setRepeating just replaces the
        // existing alarm with the same schedule. The receiver itself checks
        // the POST_NOTIFICATIONS permission before posting anything.
        NotificationChannels.ensureCreated(this)
        ReminderScheduler.scheduleDaily(this)
    }
}
