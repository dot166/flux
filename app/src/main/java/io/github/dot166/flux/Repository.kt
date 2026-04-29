package io.github.dot166.flux

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.annotation.OptIn
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.preference.PreferenceManager
import com.prof18.rssparser.RssParserBuilder
import com.prof18.rssparser.model.RssChannel
import com.prof18.rssparser.model.RssItem
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlin.collections.ifEmpty

class Repository private constructor(context: Context) {

    private val appContext = context.applicationContext
    private val feedCache = mutableMapOf<String, RSSFeed>()

    companion object {
        @Volatile private var instance: Repository? = null

        fun getInstance(context: Context): Repository =
            instance ?: synchronized(this) {
                instance ?: Repository(context.applicationContext).also { instance = it }
            }
    }

    fun getFeedUrls(): List<String> {
        return PreferenceManager.getDefaultSharedPreferences(appContext).getString("RssUrls", "")!!.split(";")
    }

    suspend fun fetchFeed(urlString: String): RSSFeed {
        val parser = RssParserBuilder().build()
        var channel = try {
            parser.getRssChannel(urlString)
        } catch (e: Exception) {
            val list: MutableList<RssItem> = ArrayList()
            list.add(
                RssItem(
                    null,
                    "Error Handler",
                    null,
                    null,
                    null,
                    e.message,
                    e.message + e.stackTrace.contentToString().replace(", ", "\n"),
                    null,
                    null,
                    null,
                    "flux",
                    null,
                    mutableListOf(),
                    null,
                    null,
                    null,
                    null,
                    null
                )
            )
            RssChannel(
                "Error Handler", null, null, null, null, null,
                list, null, null
            )
        }
        val items = mutableListOf<RssItem>()
        items.addAll(channel.items)
        items.sortWith(
            Comparator.comparingLong { item: RssItem ->
                DateUtils.convertDateToEpochSeconds(
                    item.pubDate ?: "1970-01-01 09:00:00 +0900"
                )
            }.reversed()
        )
        channel = channel.recreateWithNewItems(items)
        val excludeList = PreferenceManager.getDefaultSharedPreferences(appContext).getString("ExcludedRssUrls", "")!!.split(";")
        val feed = RSSFeed(false, urlString, channel, excludeList.contains(urlString))
        feedCache[urlString] = feed
        return feed
    }

    @OptIn(UnstableApi::class)
    suspend fun getPodcasts(): List<MediaItem> = coroutineScope {
        val urls = getFeedUrls()

        val feeds = urls.map { url ->
            async { fetchFeed(url) }
        }.awaitAll()

        feeds.filter { it.channel.itunesChannelData != null }.map { feed ->
            MediaItem.Builder()
                .setMediaId("podcast:${feed.channel.title}")
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(feed.channel.title)
                        .setArtworkUri(feed.channel.itunesChannelData!!.image!!.toUri())
                        .setIsBrowsable(true)
                        .setIsPlayable(false)
                        .build()
                )
                .build()
        }
    }

    @OptIn(UnstableApi::class)
    fun getPodcastEpisodes(podcastTitle: String): List<MediaItem> {
        val urls = getFeedUrls()
        val list = mutableListOf<MediaItem>()
        for (url in urls) {
            val feed = feedCache[url] ?: continue
            if (feed.channel.itunesChannelData == null) {
                continue
            }

            if (feed.channel.title != podcastTitle) {
                continue
            }

            for (i in 0 until feed.channel.items.size) {
                val episode = feed.channel.items[(feed.channel.items.size-1)-i]
                if (episode.itunesItemData == null) {
                    continue
                }
                if (episode.rawEnclosure == null) {
                    continue
                }
                if (!episode.rawEnclosure!!.type!!.contains("audio")) {
                    continue
                }
                val item = MediaItem.Builder()
                    .setMediaId("episode:${feed.channel.title}:$i")
                    .setUri(episode.rawEnclosure!!.url)
                    .setMediaMetadata(
                        MediaMetadata.Builder()
                            .setTitle(episode.title)
                            .setArtist(feed.channel.title)
                            .setArtworkUri(episode.itunesItemData!!.image!!.toUri())
                            .setIsBrowsable(false)
                            .setIsPlayable(true)
                            .setMediaType(MediaMetadata.MEDIA_TYPE_PODCAST)
                            .build()
                    )
                    .build()
                list.add(item)
            }
        }
        list.sortBy {
            Integer.parseInt(it.mediaId.removePrefix("episode:${podcastTitle}:"))
        }
        return list
    }

    @OptIn(UnstableApi::class)
    fun searchPodcastEpisodes(query: String): List<MediaItem>? {
        val urls = getFeedUrls()
        val list = mutableListOf<MediaItem>()
        for (url in urls) {
            val feed = feedCache[url] ?: continue
            if (feed.channel.itunesChannelData == null) {
                continue
            }
            for (i in 0 until feed.channel.items.size) {
                val episode = feed.channel.items[(feed.channel.items.size-1)-i]
                if ("${feed.channel.title}:$i" != query) {
                    continue
                }
                if (episode.itunesItemData == null) {
                    continue
                }
                if (episode.rawEnclosure == null) {
                    continue
                }
                if (!episode.rawEnclosure!!.type!!.contains("audio")) {
                    continue
                }
                val item = MediaItem.Builder()
                    .setMediaId("episode:${feed.channel.title}:$i")
                    .setUri(episode.rawEnclosure!!.url)
                    .setMediaMetadata(
                        MediaMetadata.Builder()
                            .setTitle(episode.title)
                            .setArtist(feed.channel.title)
                            .setArtworkUri(episode.itunesItemData!!.image!!.toUri())
                            .setIsBrowsable(false)
                            .setIsPlayable(true)
                            .setMediaType(MediaMetadata.MEDIA_TYPE_PODCAST)
                            .build()
                    )
                    .build()
                list.add(item)
            }
        }
        return list.ifEmpty {
            null
        }
    }

    @OptIn(UnstableApi::class)
    fun searchPodcastEpisodesByUrl(query: String): Pair<List<MediaItem>, Int>? {
        val urls = getFeedUrls()
        for (url in urls) {
            val feed = feedCache[url] ?: continue
            if (feed.channel.itunesChannelData == null) {
                continue
            }
            for (i in 0 until feed.channel.items.size) {
                val episode = feed.channel.items[(feed.channel.items.size-1)-i]
                if (episode.itunesItemData == null) {
                    continue
                }
                if (episode.rawEnclosure == null) {
                    continue
                }
                if (episode.rawEnclosure!!.url != query) {
                    continue
                }
                if (!episode.rawEnclosure!!.type!!.contains("audio")) {
                    continue
                }
                val allEps = getPodcastEpisodes(feed.channel.title!!)
                for (j in 0 until allEps.size) {
                    if (allEps[j].mediaId != "episode:${feed.channel.title}:$i") {
                        continue
                    }
                    return Pair(allEps, j)
                }
            }
        }
        return null
    }

    suspend fun getRestoredPlaybackState(): PlaybackState? = coroutineScope {
        val prefs = PreferenceManager.getDefaultSharedPreferences(appContext)
        val podcast = prefs.getString("podcast", null) ?: return@coroutineScope null
        val index = prefs.getInt("queue_index", 0)
        val position = prefs.getLong("episode_${podcast}_${index}_position", 0)
        val urls = getFeedUrls()
        urls.map { url ->
            async { fetchFeed(url) }
        }.awaitAll()

        val items = getPodcastEpisodes(podcast)
        if (items.isEmpty()) return@coroutineScope null

        PlaybackState(items, index, position)
    }
}

fun Player.saveQueue(prefs: SharedPreferences, mediaItems: List<MediaItem>) {
    prefs.edit {
        putString("podcast", mediaItems[0].mediaMetadata.artist?.toString())
        putInt("queue_index", currentMediaItemIndex)
        if (currentPosition != duration) {
            putLong(
                "episode_${mediaItems[0].mediaMetadata.station?.toString()}_${currentMediaItemIndex}_position",
                currentPosition
            )
        } else {
            remove("episode_${mediaItems[0].mediaMetadata.station?.toString()}_${currentMediaItemIndex}_position")
        }
    }
}

fun RssChannel.recreateWithNewItems(items: MutableList<RssItem>): RssChannel {
    return RssChannel(title, link, description, image, lastBuildDate, updatePeriod, items, itunesChannelData, youtubeChannelData)
}
