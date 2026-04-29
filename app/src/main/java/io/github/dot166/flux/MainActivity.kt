package io.github.dot166.flux

import android.Manifest
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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.util.UnstableApi
import androidx.preference.PreferenceManager
import coil.compose.SubcomposeAsyncImage
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import io.github.dot166.flux.DateUtils.formatTime
import io.github.dot166.flux.URLUtils.fixTwitterHtml
import io.github.dot166.flux.URLUtils.hasHtmlTags
import io.github.dot166.flux.URLUtils.isDescRendererFeed
import io.github.dot166.flux.URLUtils.toSafeString
import io.github.dot166.jlib.app.SettingsLibAlertDialogBuilder
import io.github.dot166.jlib.app.jActivity
import kotlinx.coroutines.launch
import java.io.File
import java.net.URI
import java.util.Calendar


class MainActivity: jActivity() {
    lateinit var notificationPermissionLauncher: ActivityResultLauncher<String>

    @androidx.annotation.OptIn(UnstableApi::class)
    @OptIn(ExperimentalMaterial3Api::class)
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
            AppTheme {
                val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
                val scope = rememberCoroutineScope()
                val viewModel: RSSViewModel = viewModel()
                val uiState by viewModel.state.collectAsState()
                val refreshing = uiState.refreshing
                val data = uiState.data
                val list = uiState.listOfData
                ModalNavigationDrawer(
                    drawerState = drawerState,
                    drawerContent = {
                        ModalDrawerSheet {
                            LazyColumn(Modifier.fillMaxSize()) {
                                items(list.toSortedList()) {
                                    NavigationDrawerItem(
                                        label = { Text(text = it.channel.title!!) },
                                        icon = {
                                            if (it.isAll) {
                                                Image(
                                                    painter = rememberDrawablePainter(drawable = getAppIcon()),
                                                    contentDescription = stringResource(R.string.fallback_icon),
                                                    modifier = Modifier
                                                        .size(32.dp)
                                                        .background(
                                                            MaterialTheme.colorScheme.surfaceColorAtElevation(
                                                                3.dp
                                                            )
                                                        ),
                                                )
                                            } else {
                                                val uri = URI.create(it.url)
                                                var uriNoPath = uri.resolve("/")
                                                if (uriNoPath.toString()
                                                        .contains("feeds.bbci.co.uk")
                                                ) {
                                                    uriNoPath = URI.create("bbc.co.uk")
                                                }

                                                val photoUrl = if (it.channel.image != null) {
                                                    it.channel.image!!.url
                                                } else {
                                                    "https://www.google.com/s2/favicons?domain=${uriNoPath}"
                                                }
                                                SubcomposeAsyncImage(
                                                    model = photoUrl,
                                                    contentDescription = stringResource(R.string.rss_feed_icon),
                                                    modifier = Modifier
                                                        .size(32.dp)
                                                        .background(
                                                            MaterialTheme.colorScheme.surfaceColorAtElevation(
                                                                3.dp
                                                            )
                                                        ),
                                                    loading = {
                                                        Image(
                                                            painter = rememberDrawablePainter(
                                                                drawable = getAppIcon()
                                                            ),
                                                            contentDescription = stringResource(R.string.fallback_icon),
                                                        )
                                                    })
                                            }
                                        },
                                        selected = false,
                                        onClick = {
                                            if (it.isAll) {
                                                viewModel.refreshAll(this@MainActivity)
                                            } else {
                                                viewModel.refresh(it.url, this@MainActivity)
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                ) {
                    val scaffoldState = rememberBottomSheetScaffoldState(
                        bottomSheetState = rememberStandardBottomSheetState(
                            initialValue = SheetValue.PartiallyExpanded
                        )
                    )
                    BottomSheetScaffold(
                        scaffoldState = scaffoldState,
                        topBar = {
                            TopAppBar(
                                title = {
                                    if (data != null && data.channel.title != null) {
                                        Text(text = data.channel.title!!)
                                    } else {
                                        Text(text = stringResource(R.string.loading))
                                    }
                                },
                                actions = {
                                    IconButton(
                                        onClick = {
                                            startActivity(
                                                Intent(
                                                    this@MainActivity,
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
                                navigationIcon = {
                                    IconButton(
                                        onClick = {
                                            scope.launch {
                                                drawerState.apply {
                                                    if (isClosed) open() else close()
                                                }
                                            }
                                        }
                                    ) {
                                        Icon(
                                            Icons.Filled.Menu, contentDescription = stringResource(
                                                R.string.menu
                                            )
                                        )
                                    }
                                }
                            )
                        },
                        sheetPeekHeight = 80.dp,
                        sheetContent = {
                            val controller = viewModel.controller

                            LaunchedEffect(Unit) {
                                viewModel.connectController(this@MainActivity)
                                viewModel.pollPosition()
                            }

                            Column(
                                modifier = Modifier
                                    .fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                val artUrl = viewModel.mediaMetadata.artworkUri
                                if (artUrl != null) {
                                    SubcomposeAsyncImage(
                                        model = artUrl,
                                        contentDescription = stringResource(R.string.podcast_cover_art),
                                        modifier = Modifier
                                            .size(300.dp)
                                            .background(
                                                MaterialTheme.colorScheme.surfaceColorAtElevation(
                                                    3.dp
                                                )
                                            )
                                    )
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Text(
                                    text = viewModel.mediaMetadata.title?.toString()
                                        ?: stringResource(
                                            R.string.unknown
                                        ), style = MaterialTheme.typography.headlineMedium
                                )
                                Text(
                                    text = viewModel.mediaMetadata.artist?.toString()
                                        ?: stringResource(
                                            R.string.unknown
                                        ), style = MaterialTheme.typography.bodyLarge
                                )

                                Spacer(modifier = Modifier.height(32.dp))

                                Slider(
                                    value = viewModel.currentPosition.toFloat(),
                                    valueRange = 0f..viewModel.duration.toFloat().coerceAtLeast(1f),
                                    onValueChange = { controller?.seekTo(it.toLong()) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp)
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(formatTime(viewModel.currentPosition))
                                    Text(formatTime(viewModel.duration))
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(onClick = { controller?.seekBack() }) {
                                        Icon(
                                            Icons.Default.FastRewind,
                                            stringResource(R.string.rewind)
                                        )
                                    }

                                    FilledIconButton(
                                        onClick = { if (viewModel.isPlaying) controller?.pause() else controller?.play() },
                                        modifier = Modifier.size(64.dp)
                                    ) {
                                        Icon(
                                            if (viewModel.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                            stringResource(R.string.play_pause)
                                        )
                                    }

                                    IconButton(onClick = { controller?.seekForward() }) {
                                        Icon(
                                            Icons.Default.FastForward,
                                            stringResource(R.string.fast_forward)
                                        )
                                    }
                                }
                            }
                        },
                        sheetDragHandle = { BottomSheetDefaults.DragHandle() }
                    ) { innerPadding ->
                        Column(
                            modifier = Modifier
                                .padding(innerPadding),
                        ) {
                            PullToRefreshBox(
                                isRefreshing = refreshing,
                                onRefresh = { viewModel.refreshAll(this@MainActivity) }
                            ) {
                                if (refreshing) {
                                    Text(text = stringResource(R.string.loading))
                                } else {
                                    LazyColumn(Modifier.fillMaxSize()) {
                                        items(data!!.channel.items) {
                                            ListItem({
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
                                                                        this@MainActivity
                                                                    ).getLong(
                                                                        "episode_${items.first[0].mediaMetadata.station?.toString()}_${items.second}_position",
                                                                        0
                                                                    )
                                                                )
                                                                viewModel.controller!!.prepare()
                                                                viewModel.controller!!.play()
                                                            } else {
                                                                val webpage =
                                                                    it.rawEnclosure!!.url!!.toSafeString().toUri()
                                                                val intent =
                                                                    CustomTabsIntent.Builder()
                                                                        .build()
                                                                intent.launchUrl(
                                                                    this@MainActivity,
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
                                                                this@MainActivity,
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
                                                            file.writeText(encodedHtml, Charsets.UTF_16)
                                                            val uri = FileProvider.getUriForFile(
                                                                this@MainActivity,
                                                                "${packageName}.provider",
                                                                file
                                                            )
                                                            val intentBuilder = CustomTabsIntent.Builder()
                                                            val customTabsIntent = intentBuilder.build()
                                                            customTabsIntent.intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                            customTabsIntent.launchUrl(this@MainActivity, uri)
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
                                                            file.writeText(encodedHtml, Charsets.UTF_16)
                                                            val uri = FileProvider.getUriForFile(
                                                                this@MainActivity,
                                                                "${packageName}.provider",
                                                                file
                                                            )
                                                            val intentBuilder = CustomTabsIntent.Builder()
                                                            val customTabsIntent = intentBuilder.build()
                                                            customTabsIntent.intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                            customTabsIntent.launchUrl(this@MainActivity, uri)
                                                        } else {
                                                            Toast.makeText(
                                                                this@MainActivity,
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
                                                                    this@MainActivity
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
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun MutableMap<Int, RSSFeed?>.toSortedList(): List<RSSFeed> {
        val list = mutableListOf<RSSFeed>()
        for (i in 0 until size) {
            val feed = get(i)
            if (feed != null) {
                list.add(feed)
            }
        }
        return list
    }

    fun getAppIcon(): Drawable? {
        val iconId = applicationInfo.icon
        return if (iconId == 0) AppCompatResources.getDrawable(
            this,
            android.R.mipmap.sym_def_app_icon
        ) else AppCompatResources.getDrawable(this, iconId)
    }
}