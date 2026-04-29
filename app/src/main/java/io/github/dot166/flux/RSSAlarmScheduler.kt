package io.github.dot166.flux

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent

class RSSAlarmScheduler(private val context: Context) {

    private val alarmManager: AlarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun createPendingIntent(reminderItem: ReminderItem): PendingIntent {
        val intent = Intent(context, RSSNotifAlarmReceiver::class.java)

        return PendingIntent.getBroadcast(
            context,
            reminderItem.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun schedule(reminderItem: ReminderItem) {
        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            reminderItem.time,
            AlarmManager.INTERVAL_HOUR,
            createPendingIntent(reminderItem)
        )
    }

    fun cancel(reminderItem: ReminderItem) {
        alarmManager.cancel(createPendingIntent(reminderItem))
    }
}
