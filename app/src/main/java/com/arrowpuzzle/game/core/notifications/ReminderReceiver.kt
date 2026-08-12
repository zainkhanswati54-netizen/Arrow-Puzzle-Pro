package com.arrowpuzzle.game.core.notifications

import android.Manifest
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.arrowpuzzle.game.MainActivity
import com.arrowpuzzle.game.R

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val slot = intent.getStringExtra(ReminderScheduler.EXTRA_SLOT)
        val (title, body) = copyFor(slot)

        val contentIntent = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, NotificationChannels.DAILY_REMINDER)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(contentIntent)
            .build()

        val notificationId = if (slot == ReminderScheduler.SLOT_MIDDAY) 501 else 502
        NotificationManagerCompat.from(context).notify(notificationId, notification)
    }

    private fun copyFor(slot: String?): Pair<String, String> = when (slot) {
        ReminderScheduler.SLOT_MIDDAY -> "Quick puzzle break? 🧩" to "A new Arrow Puzzle level is waiting — clear it in under a minute."
        else -> "Don't lose your streak! 🔥" to "Come back and finish today's puzzle before the day resets."
    }
}
