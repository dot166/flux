package io.github.dot166.flux

import androidx.media3.common.MediaItem
import com.prof18.rssparser.model.RssItem

class NHKEnglishHandler: Handler {
    companion object {
        private const val NHK_ENGLISH_FEED_IDENTIFIER = "nhk.or.jp/rj/podcast/rss/english.xml"
        private const val OUTRO_TRIM_DURATION = 62_000L // why 62 seconds?, that is just about the average length of the useless data I am trying to cut out

        fun isNHKEnglish(url: String): Boolean {
            return url.contains(NHK_ENGLISH_FEED_IDENTIFIER, ignoreCase = true)
        }
    }

    override fun handle(builder: MediaItem.Builder, item: RssItem, url: String): MediaItem {
        if (!isNHKEnglish(url)) {
            return builder.build() // not NHK, do not try to force it
        }
        if (item.itunesItemData == null) {
            return builder.build() // no idea how a rss podcast has no itunes data, handle it anyway
        }
        if (item.itunesItemData!!.duration == null) {
            return builder.build() // abort mission, no duration provided
        }
        val totalDurationMs = timeToSeconds(item.itunesItemData!!.duration!!) * 1000
        if (totalDurationMs == 0) {
            return builder.build() // cant trim if no audio
        }
        if (totalDurationMs <= OUTRO_TRIM_DURATION) {
            return builder.build() // cant trim if it is too short
        }
        val safeEndPosition = totalDurationMs - OUTRO_TRIM_DURATION
        val clippingConfiguration = MediaItem.ClippingConfiguration.Builder()
            .setEndPositionMs(safeEndPosition)
            .build()
        return builder.setClippingConfiguration(clippingConfiguration).build()
    }
}