package io.github.dot166.flux

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.preference.PreferenceManager
import coil.compose.SubcomposeAsyncImage
import io.github.dot166.flux.URLUtils.fixTwitterHtml
import io.github.dot166.flux.URLUtils.hasHtmlTags
import io.github.dot166.flux.URLUtils.isDescRendererFeed
import io.github.dot166.flux.URLUtils.toSafeString
import io.github.dot166.jlib.app.SettingsLibAlertDialogBuilder
import io.github.dot166.jlib.app.SettingsLibComposeTheme
import io.github.dot166.jlib.app.jActivity
import io.github.dot166.jlib.time.ReminderItem
import io.github.dot166.jlib.utils.DateUtils
import java.io.File
import java.util.Calendar

class MinusOneActivity: jActivity() {
    lateinit var notificationPermissionLauncher: ActivityResultLauncher<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        notificationPermissionLauncher = registerForActivityResult(
            RequestPermission()
        ) { isGranted: Boolean ->
            if (!isGranted) {
                if (Build.VERSION.SDK_INT_FULL >= Build.VERSION_CODES_FULL.TIRAMISU) {
                    if (shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
                        SettingsLibAlertDialogBuilder(
                            this
                        )
                            .setTitle(R.string.notification_permission)
                            .setMessage(R.string.notif_dialog)
                            .setPositiveButton(R.string.yes) { _, _ ->
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                            .setNegativeButton(R.string.no, null)
                            .show()
                    } else {
                        SettingsLibAlertDialogBuilder(this)
                            .setTitle(R.string.notification_permission)
                            .setMessage(R.string.settings_notif_dialog)
                            .setPositiveButton(android.R.string.ok) { _, _ ->
                                val intent =
                                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                intent.setData(("package:$packageName").toUri())
                                startActivity(intent)
                            }
                            .setNegativeButton(android.R.string.cancel, null)
                            .show()
                    }
                }
            }
        }
        if (Build.VERSION.SDK_INT_FULL >= Build.VERSION_CODES_FULL.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        val repo = Repository.getInstance(this)
        startService(Intent(this, RssAudioService::class.java))
        val cal: Calendar = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, cal.get(Calendar.HOUR_OF_DAY) + 1)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val reminderItem = ReminderItem(cal.getTimeInMillis(), 1)
        RSSAlarmScheduler(this).schedule(reminderItem)
        setContent {
            SettingsLibComposeTheme {
                val viewModel: RSSViewModel = viewModel()
                val uiState by viewModel.state.collectAsState()
                val refreshing = uiState.refreshing
                val data = uiState.data
                Scaffold(
                    containerColor = Color.Transparent,
                    contentColor = Color.Transparent,
                    floatingActionButton = {
                        FloatingActionButton(
                            onClick = {
                                startActivity(
                                    Intent(
                                        this@MinusOneActivity,
                                        PreferenceActivity::class.java
                                    )
                                )
                            }
                        ) {
                            Icon(
                                Icons.Filled.Settings,
                                contentDescription = stringResource(
                                    R.string.settings
                                )
                            )
                        }
                    },
                ) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .padding(innerPadding),
                    ) {
                        PullToRefreshBox(
                            isRefreshing = refreshing,
                            onRefresh = { viewModel.refreshAll(this@MinusOneActivity) }
                        ) {
                            if (refreshing) {
                                Text(text = stringResource(R.string.loading))
                            } else {
                                LazyColumn(Modifier.fillMaxSize()) {
                                    items(data!!.channel!!.items) {
                                        ListItem(
                                            {
                                                ElevatedCard(
                                                    elevation = CardDefaults.cardElevation(
                                                        defaultElevation = 6.dp
                                                    ),
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .heightIn(100.dp, 500.dp),
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
                                                                        this@MinusOneActivity
                                                                    ).getLong(
                                                                        "episode_${items.first[0].mediaMetadata.station?.toString()}_${items.second}_position",
                                                                        0
                                                                    )
                                                                )
                                                                viewModel.controller!!.prepare()
                                                                viewModel.controller!!.play()
                                                            } else {
                                                                val webpage =
                                                                    it.rawEnclosure!!.url!!.toSafeString()
                                                                        .toUri()
                                                                val intent =
                                                                    CustomTabsIntent.Builder()
                                                                        .build()
                                                                intent.launchUrl(
                                                                    this@MinusOneActivity,
                                                                    webpage
                                                                )
                                                            }
                                                        } else if (it.link != null && !it.link!!.isEmpty() && !isDescRendererFeed(
                                                                it.link!!
                                                            )
                                                        ) {
                                                            val webpage =
                                                                it.link!!.toSafeString().toUri()
                                                            val intent = CustomTabsIntent.Builder()
                                                                .build()
                                                            intent.launchUrl(
                                                                this@MinusOneActivity,
                                                                webpage
                                                            )
                                                        } else if (it.content != null && !it.content!!.isEmpty()) {
                                                            val encodedHtml = fixTwitterHtml(
                                                                it.content!!,
                                                                it.link!!.contains("://x.com")
                                                            )
                                                            val file =
                                                                File(cacheDir, "content.html")
                                                            if (file.exists()) {
                                                                file.delete()
                                                            }
                                                            file.writeText(
                                                                encodedHtml,
                                                                Charsets.UTF_16
                                                            )
                                                            val uri = FileProvider.getUriForFile(
                                                                this@MinusOneActivity,
                                                                "${packageName}.provider",
                                                                file
                                                            )
                                                            val intentBuilder =
                                                                CustomTabsIntent.Builder()
                                                            val customTabsIntent =
                                                                intentBuilder.build()
                                                            customTabsIntent.intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                            customTabsIntent.launchUrl(
                                                                this@MinusOneActivity,
                                                                uri
                                                            )
                                                        } else if (it.description != null && !it.description!!.isBlank()) {
                                                            val encodedHtml = fixTwitterHtml(
                                                                it.description!!,
                                                                it.link!!.contains("://x.com")
                                                            )
                                                            val file =
                                                                File(cacheDir, "content.html")
                                                            if (file.exists()) {
                                                                file.delete()
                                                            }
                                                            file.writeText(
                                                                encodedHtml,
                                                                Charsets.UTF_16
                                                            )
                                                            val uri = FileProvider.getUriForFile(
                                                                this@MinusOneActivity,
                                                                "${packageName}.provider",
                                                                file
                                                            )
                                                            val intentBuilder =
                                                                CustomTabsIntent.Builder()
                                                            val customTabsIntent =
                                                                intentBuilder.build()
                                                            customTabsIntent.intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                            customTabsIntent.launchUrl(
                                                                this@MinusOneActivity,
                                                                uri
                                                            )
                                                        } else {
                                                            Toast.makeText(
                                                                this@MinusOneActivity,
                                                                getString(R.string.no_content_or_url_available),
                                                                Toast.LENGTH_SHORT
                                                            ).show()
                                                        }
                                                    }
                                                ) {
                                                    Column(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
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
                                                                    this@MinusOneActivity
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
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}