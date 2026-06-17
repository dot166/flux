package io.github.dot166.flux

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import coil.compose.SubcomposeAsyncImage
import com.prof18.rssparser.model.RssItem
import io.github.dot166.flux.URLUtils.fixTwitterHtml
import io.github.dot166.flux.URLUtils.hasHtmlTags
import io.github.dot166.flux.URLUtils.isDescRendererFeed
import io.github.dot166.flux.URLUtils.toSafeString
import io.github.dot166.jlib.app.DefaultSharedPrefsManager
import io.github.dot166.jlib.utils.DateUtils
import java.io.File

@Composable
fun RSSItemCard(it: RssItem, repo: Repository, viewModel: RSSViewModel, ctx: Context, url: String, isFallback: Boolean) {
    ListItem(
        {
            ElevatedCard(
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 6.dp
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                onClick = {
                    val notified = FeedUtils.getNotified(ctx, url)
                    if (!notified.contains(it.hashCode()) && !isFallback) {
                        notified.add(it.hashCode())
                        FeedUtils.setItems(notified, url, ctx)
                    }
                    if (it.rawEnclosure != null && it.rawEnclosure!!.url != null && !it.rawEnclosure!!.url!!.isEmpty()) {
                        if (it.rawEnclosure!!.type != null && it.rawEnclosure!!.type!!.contains(
                                "audio"
                            )
                        ) {
                            val items =
                                repo.searchPodcastEpisodesByUrl(
                                    it.rawEnclosure!!.url!!
                                )!!
                            viewModel.controller!!.setMediaItems(
                                items.first, items.second,
                                DefaultSharedPrefsManager.getSharedPreferencesStorage(ctx).getLong(
                                    "episode_${items.first[items.second].mediaMetadata.artist?.toString()}_${items.third}_position",
                                ) ?: 0
                            )
                            viewModel.controller!!.prepare()
                            if (viewModel.controller!!.currentPosition >= viewModel.controller!!.duration) {
                                viewModel.controller!!.seekTo(0)
                            }
                            viewModel.controller!!.play()
                        } else {
                            val webpage =
                                it.rawEnclosure!!.url!!.toSafeString().toUri()
                            val intent =
                                CustomTabsIntent.Builder()
                                    .build()
                            intent.launchUrl(
                                ctx,
                                webpage
                            )
                        }
                    } else if (it.link != null && !it.link!!.isEmpty() && !isDescRendererFeed(
                            it.link!!
                        )
                    ) {
                        val webpage = it.link!!.toSafeString().toUri()
                        val intent = CustomTabsIntent.Builder()
                            .build()
                        intent.launchUrl(
                            ctx,
                            webpage
                        )
                    } else if (it.content != null && !it.content!!.isEmpty()) {
                        val encodedHtml = fixTwitterHtml(
                            it.content!!,
                            it.link!!.contains("://x.com")
                        )
                        val file =
                            File(ctx.cacheDir, "content.html")
                        if (file.exists()) {
                            file.delete()
                        }
                        file.writeText(encodedHtml, Charsets.UTF_16)
                        val uri = FileProvider.getUriForFile(
                            ctx,
                            "${ctx.packageName}.provider",
                            file
                        )
                        val intentBuilder = CustomTabsIntent.Builder()
                        val customTabsIntent = intentBuilder.build()
                        customTabsIntent.intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        customTabsIntent.launchUrl(ctx, uri)
                    } else if (it.description != null && !it.description!!.isBlank()) {
                        val encodedHtml = fixTwitterHtml(
                            it.description!!,
                            it.link!!.contains("://x.com")
                        )
                        val file =
                            File(ctx.cacheDir, "content.html")
                        if (file.exists()) {
                            file.delete()
                        }
                        file.writeText(encodedHtml, Charsets.UTF_16)
                        val uri = FileProvider.getUriForFile(
                            ctx,
                            "${ctx.packageName}.provider",
                            file
                        )
                        val intentBuilder = CustomTabsIntent.Builder()
                        val customTabsIntent = intentBuilder.build()
                        customTabsIntent.intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        customTabsIntent.launchUrl(ctx, uri)
                    } else {
                        Toast.makeText(
                            ctx,
                            ctx.getString(R.string.no_content_or_url_available),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .padding(10.dp)
                ) {
                    val imageUrl = it.image
                    if (imageUrl != null) {
                        SubcomposeAsyncImage(
                            model = imageUrl,
                            contentDescription = stringResource(
                                R.string.image_from_rss_feed_item
                            ),
                            modifier = Modifier
                                .size(300.dp)
                                .background(
                                    MaterialTheme.colorScheme.surfaceColorAtElevation(
                                        3.dp
                                    )
                                )
                        )
                    }
                    Text(text = it.title!!)
                    val sourceDateString = it.pubDate

                    var pubDateString = sourceDateString

                    if (sourceDateString != null) {
                        pubDateString =
                            DateUtils.convertFromCommonFormats(
                                sourceDateString,
                                ctx
                            )
                    }
                    Text(
                        text = pubDateString ?: stringResource(
                            R.string.invalid_date
                        )
                    )
                    val text = if (it.categories.isEmpty()) {
                        if ((it.description
                                ?: "").hasHtmlTags()
                        ) {
                            ""
                        } else {
                            it.description ?: ""
                        }
                    } else {
                        it.categories.toTypedArray()
                            .contentToString()
                    }
                    if (!text.isBlank()) {
                        Text(text = text)
                    }
                }
            }
        },
        colors = ListItemDefaults.colors(
            containerColor = Color.Transparent
        )
    )
}