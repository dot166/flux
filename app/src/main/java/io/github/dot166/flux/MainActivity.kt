package io.github.dot166.flux

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import coil.compose.SubcomposeAsyncImage
import com.android.settingslib.spa.framework.theme.SettingsTheme
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import com.google.common.util.concurrent.MoreExecutors
import io.github.dot166.jlib.RSSFeed
import io.github.dot166.jlib.app.DefaultSharedPrefsManager
import io.github.dot166.jlib.app.PreferenceMainActivity
import io.github.dot166.jlib.app.SettingsLibAlertDialogBuilder
import io.github.dot166.jlib.app.jActivity
import io.github.dot166.jlib.compose.MediaBottomSheetScaffold
import io.github.dot166.jlib.time.ReminderItem
import kotlinx.coroutines.launch
import java.net.URI
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit


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
        if (intent.extras != null && intent.extras!!.getString("url") != null) {
            val intent = Intent(this, RssAudioService::class.java).apply {
                putExtra("EXTRA_SHIM_CALLED", true)
            }
            startService(intent)
            val token = SessionToken(this, ComponentName(this, RssAudioService::class.java))
            val future = MediaController.Builder(this, token).buildAsync()
            future.addListener({
                val ctrl = future.get()
                val items =
                    repo.searchPodcastEpisodesByUrl(
                        intent.extras!!.getString("url")!!
                    )!!
                ctrl.setMediaItems(
                    items.first, items.second,
                    DefaultSharedPrefsManager.getSharedPreferencesStorage(this).getLong(
                        "episode_${items.first[items.second].mediaMetadata.artist?.toString()}_${items.third}_position"
                    ) ?: 0
                )
                ctrl.prepare()
                if (ctrl.currentPosition >= ctrl.duration) {
                    ctrl.seekTo(0)
                }
                ctrl.play()
            }, MoreExecutors.directExecutor())
        } else {
            startService(Intent(this, RssAudioService::class.java)) // start service normally
        }
        val now = ZonedDateTime.now()
        val minutesToNextInterval = 15 - (now.minute % 15)
        val nextTrigger = now.plusMinutes(minutesToNextInterval.toLong())
            .truncatedTo(ChronoUnit.MINUTES)
        val reminderItem = ReminderItem(nextTrigger.toInstant().toEpochMilli(), 1)
        RSSAlarmScheduler(this).schedule(reminderItem)
        setContent {
            SettingsTheme {
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
                                        label = { Text(text = it.channel!!.title!!) },
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

                                                val photoUrl = if (it.channel!!.image != null) {
                                                    it.channel!!.image!!.url
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
                                                viewModel.refresh(it, this@MainActivity)
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
                    MediaBottomSheetScaffold(
                        viewModel = viewModel,
                        context = this@MainActivity,
                        scaffoldState = scaffoldState,
                        topBar = {
                            TopAppBar(
                                title = {
                                    if (data != null && data.channel!!.title != null) {
                                        Text(text = data.channel!!.title!!)
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
                                                    PreferenceMainActivity::class.java
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
                                        items(data!!.channel!!.items) {
                                            RSSItemCard(
                                                it = it,
                                                repo = repo,
                                                viewModel = viewModel,
                                                ctx = this@MainActivity,
                                                url = data.url,
                                                isFallback = it.title == "Error Handler" && data.channel!!.title == "Error Handler"
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