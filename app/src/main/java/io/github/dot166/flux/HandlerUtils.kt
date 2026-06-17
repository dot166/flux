package io.github.dot166.flux

import androidx.media3.common.MediaItem
import com.prof18.rssparser.model.RssItem

object HandlerUtils {
    fun MediaItem.Builder.create(item: RssItem, url: String): MediaItem {
        val impl = if (NHKEnglishHandler.isNHKEnglish(url)) {
            NHKEnglishHandler()
        } else {
            DefaultHandler()
        }
        return impl.handle(this, item, url)
    }

    class DefaultHandler: Handler {
        override fun handle(builder: MediaItem.Builder, item: RssItem, url: String): MediaItem {
            return builder.build()
        }

    }
}