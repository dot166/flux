package io.github.dot166.flux

import androidx.media3.common.MediaItem
import com.prof18.rssparser.model.RssItem

object NHKHandler {
    private const val NHK_FEED_IDENTIFIER = "nhk.or.jp"
    private const val OUTRO_TRIM_DURATION = 62_000L // why 62 seconds?, that is just about the average length of the useless data I am trying to cut out
    fun isNHK(url: String): Boolean {
        return url.contains(NHK_FEED_IDENTIFIER, ignoreCase = true)
    }

    fun MediaItem.Builder.buildCorrected(item: RssItem, url: String): MediaItem {
        if (!isNHK(url)) {
            return build() // not NHK, do not try to force it
        }
        if (item.itunesItemData == null) {
            return build() // no idea how a rss podcast has no itunes data, handle it anyway
        }
        if (item.itunesItemData!!.duration == null) {
            return build() // abort mission, no duration provided
        }
        val totalDurationMs = timeToSeconds(item.itunesItemData!!.duration!!) * 1000
        if (totalDurationMs == 0) {
            return build() // cant trim if no audio
        }
        if (totalDurationMs <= OUTRO_TRIM_DURATION) {
            return build() // cant trim if it is too short
        }
        val safeEndPosition = totalDurationMs - OUTRO_TRIM_DURATION
        val clippingConfiguration = MediaItem.ClippingConfiguration.Builder()
            .setEndPositionMs(safeEndPosition)
            .build()
        return setClippingConfiguration(clippingConfiguration).build()
    }

    fun timeToSeconds(timeString: String): Int {
        val parts = timeString.split(":").map { it.toIntOrNull() ?: 0 }
        var totalSeconds = 0

        when (parts.size) {
            1 -> totalSeconds = parts[0] // ss
            2 -> totalSeconds = (parts[0] * 60) + parts[1] // mm:ss
            3 -> totalSeconds = (parts[0] * 3600) + (parts[1] * 60) + parts[2] // hh:mm:ss
        }
        return totalSeconds
    }
}