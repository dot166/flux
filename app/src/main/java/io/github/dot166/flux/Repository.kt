package io.github.dot166.flux

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import com.android.settingslib.datastore.SharedPreferencesStorage
import com.prof18.rssparser.RssParserBuilder
import com.prof18.rssparser.model.RssChannel
import com.prof18.rssparser.model.RssItem
import io.github.dot166.flux.HandlerUtils.create
import io.github.dot166.jlib.RSSFeed
import io.github.dot166.jlib.app.DefaultSharedPrefsManager
import io.github.dot166.jlib.app.LocalSharedPrefsManager
import io.github.dot166.jlib.utils.DateUtils
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.future.future

class Repository private constructor(context: Context) {

    private val appContext = context.applicationContext
    private val store = LocalSharedPrefsManager(context)
    private var feedCache = store.getRssFeedCache()

    companion object {
        @Volatile private var instance: Repository? = null

        fun getInstance(context: Context): Repository =
            instance ?: synchronized(this) {
                instance ?: Repository(context.applicationContext).also { instance = it }
            }
    }

    fun getFeeds(): MutableList<RSSFeed> {
        return store.getRssFeeds()
    }

    fun updateCache(map: MutableMap<String, RssChannel>) {
        for ((url, feed) in map) {
            if (feed.title == "Error Handler" && feed.items[0].title == "Error Handler") {
                map.remove(url, feed) // do not allow error handler to be cached, always omit from cache
            }
        }
        store.saveRssFeedCache(map)
    }

    fun saveFeeds(list: MutableList<RSSFeed>) {
        store.saveRssFeeds(list)
    }

    suspend fun fetchFeed(feed: RSSFeed): RSSFeed {
        val urlString = feed.url
        val parser = RssParserBuilder().build()
        var channel = try {
            parser.getRssChannel(urlString)
        } catch (e: Exception) {
            e.printStackTrace()
            feedCache[urlString] ?: genErrorHandler(Exception(e))
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
        val feed = feed.populate(channel)
        feedCache[urlString] = channel
        updateCache(feedCache)
        return feed
    }


    fun genErrorHandler(e: Throwable): RssChannel {
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
        return RssChannel(
            "Error Handler", null, null, null, null, null,
            list, null, null
        )
    }

    @OptIn(UnstableApi::class)
    suspend fun getPodcasts(): List<MediaItem> = coroutineScope {
        val urls = getFeeds()

        val feeds = urls.map { url ->
            async { fetchFeed(url) }
        }.awaitAll()

        feeds.filter { it.channel != null && it.channel!!.itunesChannelData != null }.map { feed ->
            MediaItem.Builder()
                .setMediaId("podcast:${feed.channel!!.title}")
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(feed.channel!!.title)
                        .setArtworkUri(getPodcastArtwork(feed.channel!!))
                        .setIsBrowsable(true)
                        .setIsPlayable(false)
                        .build()
                )
                .build()
        }
    }

    @OptIn(UnstableApi::class)
    fun getPodcastEpisodes(podcastTitle: String): List<MediaItem> {
        val feeds = getFeeds()
        val list = mutableListOf<MediaItem>()
        for (url in feeds) {
            val feed = feedCache[url.url] ?: continue
            if (feed.itunesChannelData == null) {
                continue
            }

            if (feed.title != podcastTitle) {
                continue
            }

            for (i in feed.items.indices) {
                val episode = feed.items[(feed.items.size-1)-i]
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
                    .setMediaId("episode:${feed.title}:$i")
                    .setUri(episode.rawEnclosure!!.url)
                    .setMediaMetadata(
                        MediaMetadata.Builder()
                            .setTitle(episode.title)
                            .setArtist(feed.title)
                            .setArtworkUri(getPodcastEpisodeArtwork(feed, episode))
                            .setIsBrowsable(false)
                            .setIsPlayable(true)
                            .setMediaType(MediaMetadata.MEDIA_TYPE_PODCAST)
                            .build()
                    )
                    .create(episode, url.url)
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
        val urls = getFeeds()
        val list = mutableListOf<MediaItem>()
        for (url in urls) {
            val feed = feedCache[url.url] ?: continue
            if (feed.itunesChannelData == null) {
                continue
            }
            for (i in feed.items.indices) {
                val episode = feed.items[(feed.items.size-1)-i]
                if ("${feed.title}:$i" != query) {
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
                    .setMediaId("episode:${feed.title}:$i")
                    .setUri(episode.rawEnclosure!!.url)
                    .setMediaMetadata(
                        MediaMetadata.Builder()
                            .setTitle(episode.title)
                            .setArtist(feed.title)
                            .setArtworkUri(getPodcastEpisodeArtwork(feed, episode))
                            .setIsBrowsable(false)
                            .setIsPlayable(true)
                            .setMediaType(MediaMetadata.MEDIA_TYPE_PODCAST)
                            .build()
                    )
                    .create(episode, url.url)
                list.add(item)
            }
        }
        return list.ifEmpty {
            null
        }
    }

    @OptIn(UnstableApi::class)
    fun searchPodcastEpisodesByUrl(query: String): Triple<List<MediaItem>, Int, Int>? {
        val urls = getFeeds()
        for (url in urls) {
            val feed = feedCache[url.url] ?: continue
            if (feed.itunesChannelData == null) {
                continue
            }
            for (i in feed.items.indices) {
                val episode = feed.items[(feed.items.size-1)-i]
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
                val allEps = getPodcastEpisodes(feed.title!!)
                for ((j, element) in allEps.withIndex()) {
                    if (element.mediaId != "episode:${feed.title}:$i") {
                        continue
                    }
                    return Triple(allEps, j, episode.hashCode())
                }
            }
        }
        return null
    }

    @OptIn(UnstableApi::class)
    fun getPodcastEpisodeHashCode(podcastTitle: String, index: Int): Int {
        val feeds = getFeeds()
        for (url in feeds) {
            val feed = feedCache[url.url] ?: continue
            if (feed.itunesChannelData == null) {
                continue
            }

            if (feed.title != podcastTitle) {
                continue
            }

            for (i in feed.items.indices) {
                val episode = feed.items[(feed.items.size-1)-i]
                if (episode.itunesItemData == null) {
                    continue
                }
                if (episode.rawEnclosure == null) {
                    continue
                }
                if (!episode.rawEnclosure!!.type!!.contains("audio")) {
                    continue
                }
                if (i == index) {
                    return episode.hashCode()
                }
            }
        }
        return 0
    }

    suspend fun getRestoredPlaybackState(): PlaybackState? = coroutineScope {
        val prefs = DefaultSharedPrefsManager.getSharedPreferencesStorage(appContext)
        val podcast = prefs.getString("podcast") ?: return@coroutineScope null
        val index = prefs.getInt("queue_index") ?: 0
        val urls = getFeeds()
        urls.map { url ->
            async { fetchFeed(url) }
        }.awaitAll()

        val items = getPodcastEpisodes(podcast)
        if (items.isEmpty()) return@coroutineScope null
        val position = prefs.getLong("episode_${podcast}_${getPodcastEpisodeHashCode(podcast, index)}_position") ?: 0

        PlaybackState(items, index, position)
    }

    fun getPodcastArtwork(feed: RssChannel): Uri {
        return if (feed.itunesChannelData != null && feed.itunesChannelData!!.image != null) {
            feed.itunesChannelData!!.image!!.toUri()
        } else if (feed.image != null && feed.image!!.url != null) {
            feed.image!!.url!!.toUri()
        } else {
            // oh shit, we don't have an image, try and pass in the default cover and hope it works
            "${ContentResolver.SCHEME_ANDROID_RESOURCE}://${appContext.packageName}/${R.drawable.def_art}".toUri()
        }
    }

    fun getPodcastEpisodeArtwork(feed: RssChannel, episode: RssItem): Uri {
        return if (episode.itunesItemData != null && episode.itunesItemData!!.image != null) {
            episode.itunesItemData!!.image!!.toUri()
        } else if (episode.image != null) {
            episode.image!!.toUri()
        } else {
            // oh shit, we don't have an image for that episode, use the podcasts image
            getPodcastArtwork(feed)
        }
    }

    @kotlin.OptIn(DelicateCoroutinesApi::class)
    fun cleanupSavedPositions() {
        val feeds = getFeeds()
        val prefs = DefaultSharedPrefsManager.getSharedPreferencesStorage(appContext).sharedPreferences // needs sharedpref functions I have not abstracted yet
        GlobalScope.future {
            val list = mutableListOf("RssUrls", "ExcludedRssUrls", "podcast", "queue_index", "notified")
            for (i in feeds.indices) {
                val feed = fetchFeed(feeds[i])
                val episodes = getPodcastEpisodes(feed.channel!!.title!!)
                for (j in episodes.indices) {
                    list.add("episode_${feed.channel!!.title!!}_${getPodcastEpisodeHashCode(feed.channel!!.title!!, j)}_position")
                }
            }
            val keys = prefs.all.keys
            for (key in keys) {
                if (!list.contains(key)) {
                    prefs.edit { remove(key) }
                }
            }
        }
    }

    suspend fun cleanupNotified() {
        val feeds = getFeeds()
        for (i in feeds.indices) {
            val feed = fetchFeed(feeds[i])
            if (feed.channel!!.items[0].title == "Error Handler" && feed.channel!!.title == "Error Handler") {
                continue
            }
            val newItems = mutableListOf<Int>()
            for (j in feed.channel!!.items.indices) {
                newItems.add(feed.channel!!.items[j].hashCode())
            }
            val notified = FeedUtils.getNotified(appContext, feed.url)
            for (item in notified) {
                if (!newItems.contains(item.hashCode())) {
                    notified.remove(item)
                }
            }
            FeedUtils.setItems(notified, feed.url, appContext)
        }
    }
}

fun Player.saveQueue(prefs: SharedPreferencesStorage, mediaItems: List<MediaItem>, repo: Repository) {
    prefs.apply {
        setString("podcast", mediaItems[currentMediaItemIndex].mediaMetadata.artist?.toString())
        setInt("queue_index", currentMediaItemIndex)
        setLong(
            "episode_${mediaItems[currentMediaItemIndex].mediaMetadata.artist?.toString()}_${
                repo.getPodcastEpisodeHashCode(
                    mediaItems[currentMediaItemIndex].mediaMetadata.artist?.toString()!!,
                    currentMediaItemIndex
                )
            }_position",
            currentPosition
        )
    }
}

fun RssChannel.recreateWithNewItems(items: MutableList<RssItem>): RssChannel {
    return RssChannel(title, link, description, image, lastBuildDate, updatePeriod, items, itunesChannelData, youtubeChannelData)
}

fun RSSFeed.populate(channel: RssChannel?): RSSFeed {
    return RSSFeed(isAll, url, channel, hiddenFromAll)
}
