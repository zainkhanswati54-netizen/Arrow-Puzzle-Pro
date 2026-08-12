package com.arrowpuzzle.game.core.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.util.Calendar

/**
 * Two fixed daily reminders — one midday, one evening — each with its own
 * request code so they don't clobber each other. Alarms are inexact
 * (setRepeating) since a reminder landing a few minutes off schedule is
 * harmless; that also means no special "exact alarm" permission is needed.
 */
object ReminderScheduler {

    private const val REQUEST_CODE_MIDDAY = 4001
    private const val REQUEST_CODE_EVENING = 4002
    const val EXTRA_SLOT = "reminder_slot"
    const val SLOT_MIDDAY = "midday"
    const val SLOT_EVENING = "evening"

    /** Arms (or re-arms) both daily reminders. Safe to call repeatedly — replaces existing alarms. */
    fun scheduleDaily(context: Context) {
        schedule(context, hour = 12, minute = 0, requestCode = REQUEST_CODE_MIDDAY, slot = SLOT_MIDDAY)
        schedule(context, hour = 19, minute = 30, requestCode = REQUEST_CODE_EVENING, slot = SLOT_EVENING)
    }

    fun cancelAll(context: Context) {
        cancel(context, REQUEST_CODE_MIDDAY, SLOT_MIDDAY)
        cancel(context, REQUEST_CODE_EVENING, SLOT_EVENING)
    }

    private fun schedule(context: Context, hour: Int, minute: Int, requestCode: Int, slot: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val triggerAt = nextOccurrence(hour, minute)
        val pendingIntent = pendingIntentFor(context, requestCode, slot)
        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            triggerAt,
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )
    }

    private fun cancel(context: Context, requestCode: Int, slot: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntentFor(context, requestCode, slot))
    }

    private fun pendingIntentFor(context: Context, requestCode: Int, slot: String): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).putExtra(EXTRA_SLOT, slot)
        return PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun nextOccurrence(hour: Int, minute: Int): Long {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (target.before(now)) target.add(Calendar.DAY_OF_YEAR, 1)
        return target.timeInMillis
    }
}
