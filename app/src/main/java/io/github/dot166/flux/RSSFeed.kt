package io.github.dot166.flux

import com.prof18.rssparser.model.RssChannel

data class RSSFeed(val isAll: Boolean = false, val url: String, val channel: RssChannel, val hiddenFromAll: Boolean)