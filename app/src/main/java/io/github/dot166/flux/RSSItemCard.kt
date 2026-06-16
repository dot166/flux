package io.github.dot166.flux

import android.Manifest
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.appcompat.content.res.AppCompatResources
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.util.UnstableApi
import androidx.preference.PreferenceManager
import coil.compose.SubcomposeAsyncImage
import com.android.settingslib.spa.framework.theme.SettingsTheme
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import com.prof18.rssparser.model.RssItem
import io.github.dot166.flux.URLUtils.fixTwitterHtml
import io.github.dot166.flux.URLUtils.hasHtmlTags
import io.github.dot166.flux.URLUtils.isDescRendererFeed
import io.github.dot166.flux.URLUtils.toSafeString
import io.github.dot166.jlib.RSSFeed
import io.github.dot166.jlib.app.SettingsLibAlertDialogBuilder
import io.github.dot166.jlib.app.SettingsLibComposeTheme
import io.github.dot166.jlib.app.jActivity
import io.github.dot166.jlib.compose.MediaBottomSheetScaffold
import io.github.dot166.jlib.time.ReminderItem
import io.github.dot166.jlib.utils.DateUtils
import kotlinx.coroutines.launch
import java.io.File
import java.net.URI
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

@Composable
fun RSSItemCard(it: RssItem, repo: Repository, viewModel: RSSViewModel, ctx: Context) {
    ListItem({
        ElevatedCard(
            elevation = CardDefaults.cardElevation(
                defaultElevation = 6.dp
            ),
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            onClick = {
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
                            PreferenceManager.getDefaultSharedPreferences(
                                ctx
                            ).getLong(
                                "episode_${items.first[items.second].mediaMetadata.artist?.toString()}_${items.third}_position",
                                0
                            )
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
    })
}