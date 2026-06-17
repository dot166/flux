package io.github.dot166.flux

import androidx.media3.common.MediaItem
import com.prof18.rssparser.model.RssItem

interface Handler {
    fun handle(builder: MediaItem.Builder, item: RssItem, url: String): MediaItem

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