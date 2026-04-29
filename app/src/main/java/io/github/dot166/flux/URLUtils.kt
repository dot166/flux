package io.github.dot166.flux

import androidx.core.text.HtmlCompat
import kotlin.text.contains

object URLUtils {

    fun String.toSafeString(): String {
        val str: String =
            if (contains("://x.com") || contains("://www.x.com") || contains("://twitter.com") || contains("://www.twitter.com")) {
                replace("://x.com", "://xcancel.com")
                    .replace("://www.x.com", "://xcancel.com")
                    .replace("://twitter.com", "://xcancel.com")
                    .replace("://www.twitter.com", "://xcancel.com")
            } else if (contains("://reddit.com") || contains("://www.reddit.com")) {
                replace("://reddit.com", "://kddit.kalli.st")
                    .replace("://www.reddit.com", "://kddit.kalli.st")
            } else {
                this
            }
        return str
    }

    fun String.hasHtmlTags(): Boolean {
        val stripped = HtmlCompat.fromHtml(this, HtmlCompat.FROM_HTML_MODE_LEGACY).toString()
        return stripped.length != length
    }

    fun isDescRendererFeed(str: String): Boolean {
        if (str.contains("://x.com") || str.contains("://www.x.com") || str.contains("://twitter.com") || str.contains("://www.twitter.com")) { // RSSHub feeds that point to twitter put the post in the description, so handle it there, xcancel cant draw retweets properly :(
            return true
        } else {
            return false
        }
    }

    fun fixTwitterHtml(input: String, isTwitter: Boolean): String {
        if (isTwitter) {
            val userMatches = """href="https://x.com/([^/"]+)"""".toRegex().findAll(input).toList()
            val retweeterName = userMatches.getOrNull(0)?.groupValues?.get(1) ?: "twitter"
            val retweetedName = userMatches.getOrNull(1)?.groupValues?.get(1) ?: retweeterName
            val retweeterAvatar = "https://unavatar.io/twitter/$retweeterName"
            val retweetedAvatar = "https://unavatar.io/twitter/$retweetedName"
            var processedHtml = input
            processedHtml = processedHtml.replace(
                "rel=\"noopener noreferrer\"><strong>",
                """rel="noopener noreferrer"><img width="48" height="48" src="$retweeterAvatar" hspace="8" vspace="8" align="left" referrerpolicy="no-referrer"><strong>"""
            )
            processedHtml = processedHtml.replace(
                """width="48" height="48" src="https://x.com/[^"]+/status/undefined"""".toRegex(),
                "width=\"24\" height=\"24\" src=\"$retweetedAvatar\""
            )
            if (!processedHtml.contains("width=\"48\" height=\"48\" src=\"")) {
                processedHtml.replace("width=\"24\" height=\"24\" src=\"",
                    "width=\"48\" height=\"48\" src=\"")
            }
            val urlRegex = """(?<!href="|src=")(https?://[^\s<]+)""".toRegex()
            processedHtml = urlRegex.replace(processedHtml) { matchResult ->
                val url = matchResult.value
                """<a href="$url" target="_blank" rel="noopener noreferrer">$url</a>"""
            }
            val css = """
        <style>
            .container { font-family: sans-serif; border: 1px solid #ddd; border-radius: 12px; padding: 15px; line-height: 1.4; height: 100vh; width: 100vw; margin: 0; display: flex; flex-direction: column; }
            
            img[align="left"] { 
                border-radius: 50%; 
                margin-right: 12px; 
                float: left;
            }
            
            .body-text { color: #0f1419; }
            a { color: #1d9bf0; text-decoration: none; word-break: break-all; }
            
            img:not([align="left"]) { 
                border-radius: 12px; margin-top: 10px; flex: 1;
            }
            .clearfix::after { content: ""; clear: both; display: table; }
        </style>
    """.trimIndent()

            return "<html>\n<head>\n$css\n</head>\n<body>\n<div class=\"container clearfix\">\n$processedHtml\n</div>\n</body>\n</html>"
        } else {
            return input
        }
    }
}