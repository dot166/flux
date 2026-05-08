package io.github.dot166.flux

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.future.future

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
                val rssNotifier = RSSNotifier(notificationManager, context)
                GlobalScope.future {
                    rssNotifier.showNotification()
                }.get()
            }
        }
    }
}