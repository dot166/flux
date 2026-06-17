package io.github.dot166.flux

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider
import com.prof18.rssparser.model.RssItem
import androidx.core.net.toUri
import io.github.dot166.flux.URLUtils.fixTwitterHtml
import io.github.dot166.flux.URLUtils.isDescRendererFeed
import io.github.dot166.flux.URLUtils.toSafeString
import io.github.dot166.jlib.RSSFeed
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.future.future
import java.io.File

class RSSNotifier(
    private val notificationManager: NotificationManager,
    private val context: Context
) {

    @OptIn(DelicateCoroutinesApi::class)
    fun showNotification() {
        GlobalScope.future {
            val notificationChannelId = "rss_channel_id"
            val notificationChannelName = "RSS Feed Notifications"
            val repo = Repository.getInstance(context)
            repo.cleanupNotified()
            val rssUrls: List<RSSFeed> = repo.getFeeds()
            for (feed in rssUrls) {
                val rssChannel = repo.fetchFeed(feed)
                if (rssChannel.channel == null) {
                    continue
                }
                if (rssChannel.channel!!.items[0].title == "Error Handler" && rssChannel.channel!!.title == "Error Handler") {
                    Log.e(
                        "RSS",
                        "cannot display notification as RSS reader is in fallback mode"
                    )
                    continue
                }
                if (rssChannel.hiddenFromAll) {
                    Log.e("RSS", "cannot display notification as feed is hidden")
                    continue
                }
                val articles: List<RssItem> = rssChannel.channel!!.items

                val notified = FeedUtils.getNotified(context, feed.url)
                for (j in articles.size downTo 0) {
                    val article = articles[j]
                    if (!notified.contains(article.hashCode())) {
                        val channel = NotificationChannel(
                            notificationChannelId,
                            notificationChannelName,
                            NotificationManager.IMPORTANCE_DEFAULT
                        )
                        notificationManager.createNotificationChannel(channel)
                        val intent =
                            if (article.rawEnclosure != null && article.rawEnclosure!!.url != null && !article.rawEnclosure!!.url!!.isEmpty()) {
                                if (article.rawEnclosure!!.type != null && article.rawEnclosure!!.type!!.contains(
                                        "audio"
                                    )
                                ) {
                                    val intent = Intent(context, MainActivity::class.java)
                                    intent.putExtra(
                                        "url",
                                        article.rawEnclosure!!.url!!
                                    )
                                    intent
                                } else {
                                    val webpage =
                                        article.rawEnclosure!!.url!!.toSafeString()
                                            .toUri()
                                    val intent =
                                        CustomTabsIntent.Builder()
                                            .build()
                                    val intent2 = intent.intent
                                    intent2.setData(webpage)
                                    intent2
                                }
                            } else if (article.link != null && !article.link!!.isEmpty() && !isDescRendererFeed(
                                    article.link!!
                                )
                            ) {
                                val webpage = article.link!!.toSafeString().toUri()
                                val intent = CustomTabsIntent.Builder()
                                    .build()
                                val intent2 = intent.intent
                                intent2.setData(webpage)
                                intent2
                            } else if (article.content != null && !article.content!!.isEmpty()) {
                                val encodedHtml = fixTwitterHtml(
                                    article.content!!,
                                    article.link!!.contains("://x.com")
                                )
                                val file =
                                    File(context.cacheDir, "content.html")
                                if (file.exists()) {
                                    file.delete()
                                }
                                file.writeText(encodedHtml, Charsets.UTF_16)
                                val uri = FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.provider",
                                    file
                                )
                                val intentBuilder = CustomTabsIntent.Builder()
                                val customTabsIntent = intentBuilder.build()
                                customTabsIntent.intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                val intent2 = customTabsIntent.intent
                                intent2.setData(uri)
                                intent2
                            } else if (article.description != null && !article.description!!.isBlank()) {
                                val encodedHtml = fixTwitterHtml(
                                    article.description!!,
                                    article.link!!.contains("://x.com")
                                )
                                val file =
                                    File(context.cacheDir, "content.html")
                                if (file.exists()) {
                                    file.delete()
                                }
                                file.writeText(encodedHtml, Charsets.UTF_16)
                                val uri = FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.provider",
                                    file
                                )
                                val intentBuilder = CustomTabsIntent.Builder()
                                val customTabsIntent = intentBuilder.build()
                                customTabsIntent.intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                val intent2 = customTabsIntent.intent
                                intent2.setData(uri)
                                intent2
                            } else {
                                Intent(context, MainActivity::class.java)
                            }
                        val notification =
                            NotificationCompat.Builder(context, notificationChannelId)
                                .setContentTitle(rssChannel.channel!!.title)
                                .setContentText(
                                    article.title + " - " + if (article.itunesItemData != null) context.getString(
                                        R.string.click_to_listen
                                    ) else context.getString(R.string.click_to_read)
                                )
                                .setSmallIcon(R.drawable.outline_rss_feed_24)
                                .setAutoCancel(true)
                                .setContentIntent(
                                    PendingIntent.getActivity(
                                        context,
                                        0,
                                        intent,
                                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                                    )
                                )
                                .build()
                        notificationManager.notify(
                            feed.url,
                            j,
                            notification
                        )
                        notified.add(article.hashCode())
                    }
                }
                FeedUtils.setItems(notified, feed.url, context)
            }
        }.get()
    }
}

