package com.arrowpuzzle.game.core.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

object NotificationChannels {
    const val DAILY_REMINDER = "daily_reminder"

    fun ensureCreated(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            DAILY_REMINDER,
            "Daily reminders",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Reminds you to come back and play your daily Arrow Puzzle."
        }
        manager.createNotificationChannel(channel)
    }
}
