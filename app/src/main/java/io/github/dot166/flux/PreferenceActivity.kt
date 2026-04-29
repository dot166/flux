package io.github.dot166.flux

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.PersistableBundle
import android.provider.Settings
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import com.android.settingslib.preference.PreferenceFragment
import io.github.dot166.jlib.app.SettingsLibAlertDialogBuilder
import io.github.dot166.jlib.app.jConfigActivity
import androidx.core.net.toUri


class PreferenceActivity: jConfigActivity() {
    lateinit var notificationPermissionLauncher: ActivityResultLauncher<String>
    override fun onCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
        super.onCreate(savedInstanceState, persistentState)
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
                                val intent: Intent =
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
    }
    override fun preferenceFragment(): PreferenceFragment {
        return RSSPreferenceFragment()
    }

    class RSSPreferenceFragment : PreferenceFragment() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            super.onCreatePreferences(savedInstanceState, rootKey)
            setPreferencesFromResource(R.xml.config, rootKey)
        }
    }
}