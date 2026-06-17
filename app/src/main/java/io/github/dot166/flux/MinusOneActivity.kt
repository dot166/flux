package io.github.dot166.flux

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.settingslib.spa.framework.theme.SettingsTheme
import io.github.dot166.jlib.app.PreferenceMainActivity
import io.github.dot166.jlib.app.SettingsLibAlertDialogBuilder
import io.github.dot166.jlib.app.jActivity
import io.github.dot166.jlib.time.ReminderItem
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

class MinusOneActivity: jActivity() {
    lateinit var notificationPermissionLauncher: ActivityResultLauncher<String>

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
        val now = ZonedDateTime.now()
        val minutesToNextInterval = 15 - (now.minute % 15)
        val nextTrigger = now.plusMinutes(minutesToNextInterval.toLong())
            .truncatedTo(ChronoUnit.MINUTES)
        val reminderItem = ReminderItem(nextTrigger.toInstant().toEpochMilli(), 1)
        RSSAlarmScheduler(this).schedule(reminderItem)
        setContent {
            SettingsTheme {
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
                                        RSSItemCard(it, repo, viewModel, this@MinusOneActivity, data.url, isFallback = it.title == "Error Handler" && data.channel!!.title == "Error Handler")
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