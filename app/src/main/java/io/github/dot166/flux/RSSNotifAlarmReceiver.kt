package io.github.dot166.flux

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import io.github.dot166.jlib.time.ReminderItem
import kotlinx.coroutines.DelicateCoroutinesApi
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

class RSSNotifAlarmReceiver : BroadcastReceiver() {
    @OptIn(DelicateCoroutinesApi::class)
    override fun onReceive(context: Context?, intent: Intent?) {
        Log.i("RSS", "received")
        if (context != null) {
            val repo = Repository.getInstance(context)
            if (!repo.getFeeds().isEmpty()) {
                Log.i("RSS", "notif")
                val notificationManager =
                    context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                RSSNotifier(notificationManager, context).showNotification()
            }
            val now = ZonedDateTime.now()
            val minutesToNextInterval = 15 - (now.minute % 15)
            val nextTrigger = now.plusMinutes(minutesToNextInterval.toLong())
                .truncatedTo(ChronoUnit.MINUTES)
            val reminderItem = ReminderItem(nextTrigger.toInstant().toEpochMilli(), 1)
            RSSAlarmScheduler(context).schedule(reminderItem)
        }
    }
}