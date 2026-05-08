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
import androidx.preference.PreferenceManager
import com.prof18.rssparser.model.RssItem
import androidx.core.content.edit
import androidx.core.net.toUri
import io.github.dot166.flux.URLUtils.fixTwitterHtml
import io.github.dot166.flux.URLUtils.isDescRendererFeed
import io.github.dot166.flux.URLUtils.toSafeString
import io.github.dot166.jlib.RSSFeed
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.io.File

class RSSNotifier(
    private val notificationManager: NotificationManager,
    private val context: Context
) {

    suspend fun showNotification() {
        coroutineScope {
            val notificationChannelId = "rss_channel_id"
            val notificationChannelName = "RSS Feed Notifications"
            val repo = Repository.getInstance(context)
            val rssUrls: List<RSSFeed> = repo.getFeeds()
            rssUrls.mapIndexed { index, feed ->
                async {
                    val rssChannel = repo.fetchFeed(feed)
                    if (rssChannel.channel == null) {
                        return@async
                    }
                    if (rssChannel.channel!!.items[0].title == "Error Handler" && rssChannel.channel!!.title == "Error Handler") {
                        Log.e(
                            "RSS",
                            "cannot display notification as RSS reader is in fallback mode"
                        )
                        return@async
                    }
                    if (rssChannel.hiddenFromAll) {
                        Log.e("RSS", "cannot display notification as feed $index is hidden")
                        return@async
                    }
                    val articles: List<RssItem> = rssChannel.channel!!.items

                    val savedRssUrl: String = PreferenceManager.getDefaultSharedPreferences(context)
                        .getString("lastRssUrl-" + feed.url, "")!!
                    if (savedRssUrl != articles[0].link) {
                        val channel = NotificationChannel(
                            notificationChannelId,
                            notificationChannelName,
                            NotificationManager.IMPORTANCE_DEFAULT
                        )
                        notificationManager.createNotificationChannel(channel)
                        val intent =
                            if (articles[0].rawEnclosure != null && articles[0].rawEnclosure!!.url != null && !articles[0].rawEnclosure!!.url!!.isEmpty()) {
                                if (articles[0].rawEnclosure!!.type != null && articles[0].rawEnclosure!!.type!!.contains(
                                        "audio"
                                    )
                                ) {
                                    val intent = Intent(context, AudioShim::class.java)
                                    intent.extras!!.putString(
                                        "url",
                                        articles[0].rawEnclosure!!.url!!
                                    )
                                    intent
                                } else {
                                    val webpage =
                                        articles[0].rawEnclosure!!.url!!.toSafeString()
                                            .toUri()
                                    val intent =
                                        CustomTabsIntent.Builder()
                                            .build()
                                    val intent2 = intent.intent
                                    intent2.setData(webpage)
                                    intent2
                                }
                            } else if (articles[0].content != null && !articles[0].content!!.isEmpty()) {
                                val encodedHtml = fixTwitterHtml(
                                    articles[0].content!!,
                                    articles[0].link!!.contains("://x.com")
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
                            } else if (articles[0].link != null && !articles[0].link!!.isEmpty() && !isDescRendererFeed(
                                    articles[0].link!!
                                )
                            ) {
                                val webpage = articles[0].link!!.toSafeString().toUri()
                                val intent = CustomTabsIntent.Builder()
                                    .build()
                                val intent2 = intent.intent
                                intent2.setData(webpage)
                                intent2
                            } else if (articles[0].description != null && !articles[0].description!!.isBlank()) {
                                val encodedHtml = fixTwitterHtml(
                                    articles[0].description!!,
                                    articles[0].link!!.contains("://x.com")
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
                                .setContentTitle(rssChannel.channel!!.title + " - " + articles[0].title)
                                .setContentText(articles[0].description)
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
                            index,
                            notification
                        )
                        PreferenceManager.getDefaultSharedPreferences(context).edit {
                            putString("lastRssUrl-" + feed.url, articles[0].link)
                        }
                    }
                }
            }.awaitAll()
        }
    }
}

