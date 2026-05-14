package io.github.dot166.flux

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.PersistableBundle
import android.provider.Settings
import android.view.Menu
import android.view.MenuItem
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.core.net.toUri
import androidx.preference.PreferenceManager
import com.android.settingslib.preference.PreferenceFragment
import io.github.dot166.jlib.app.LocalSharedPrefsManager
import io.github.dot166.jlib.app.SettingsLibAlertDialogBuilder
import io.github.dot166.jlib.app.jConfigActivity
import io.github.dot166.jlib.utils.SPUtils


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

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.import_xml_jlib -> {
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
                intent.setType("text/xml") // Or any other MIME type you need
                intent.addCategory(Intent.CATEGORY_OPENABLE)
                startActivityForResult(intent, 61016)
                return true
            }
            R.id.export_xml_jlib -> {
                val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
                intent.addCategory(Intent.CATEGORY_OPENABLE)
                intent.setType("text/xml")
                intent.putExtra(Intent.EXTRA_TITLE, "Prefs-jLib-$packageName.xml") // default filename
                startActivityForResult(intent, 888)
                return true
            }
            R.id.import_xml_app -> {
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
                intent.setType("text/xml") // Or any other MIME type you need
                intent.addCategory(Intent.CATEGORY_OPENABLE)
                startActivityForResult(intent, 39)
                return true
            }
            R.id.export_xml_app -> {
                val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
                intent.addCategory(Intent.CATEGORY_OPENABLE)
                intent.setType("text/xml")
                intent.putExtra(Intent.EXTRA_TITLE, "Prefs-app-$packageName.xml") // default filename
                startActivityForResult(intent, 2525)
                return true
            }
            else -> {
                return super.onOptionsItemSelected(item)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == RESULT_OK) {
            when (requestCode) {
                61016 -> {
                    SPUtils.importSharedPrefsFromSAF(
                        data,
                        this,
                        LocalSharedPrefsManager.getSharedPreferencesStorage(this).sharedPreferences
                    )
                }
                888 -> {
                    SPUtils.exportSharedPrefsToSAF(
                        data,
                        this,
                        LocalSharedPrefsManager.getSharedPreferencesStorage(this).sharedPreferences,
                        null
                    )
                }
                39 -> {
                    SPUtils.importSharedPrefsFromSAF(
                        data,
                        this,
                        PreferenceManager.getDefaultSharedPreferences(this)
                    )
                }
                2525 -> {
                    SPUtils.exportSharedPrefsToSAF(
                        data,
                        this,
                        PreferenceManager.getDefaultSharedPreferences(this),
                        null
                    )
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater = getMenuInflater()
        inflater.inflate(R.menu.import_menu, menu)
        return true
    }
}