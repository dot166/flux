package io.github.dot166.flux

import android.annotation.SuppressLint
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.Wallpapers
import com.android.settingslib.spa.framework.common.SettingsPageProvider
import com.android.settingslib.spa.framework.common.SpaEnvironmentFactory
import com.android.settingslib.spa.framework.theme.SettingsTheme
import com.android.settingslib.spa.widget.preference.Preference
import com.android.settingslib.spa.widget.preference.PreferenceModel
import com.android.settingslib.spa.widget.scaffold.MoreOptionsAction
import com.android.settingslib.spa.widget.scaffold.RegularScaffold
import com.android.settingslib.spa.widget.ui.Category
import io.github.dot166.jlib.app.DefaultSharedPrefsManager
import io.github.dot166.jlib.app.LocalSharedPrefsManager
import io.github.dot166.jlib.utils.SPUtils

object HomePageProvider : SettingsPageProvider {
    override val name = "Flux"
    override val displayName = "Home"

    override fun getTitle(arguments: Bundle?): String {
        return SpaEnvironmentFactory.instance.appContext.getString(R.string.app_name)
    }

    @Composable
    override fun Page(arguments: Bundle?) {
        val title = remember { getTitle(arguments) }
        val pickerOneLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocument()
        ) { uri: Uri? ->
            SPUtils.importSharedPrefsFromSAF(
                uri,
                SpaEnvironmentFactory.instance.appContext,
                LocalSharedPrefsManager.getSharedPreferencesStorage(SpaEnvironmentFactory.instance.appContext)
            )
        }
        val pickerTwoLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.CreateDocument("text/xml")
        ) { uri: Uri? ->
            SPUtils.exportSharedPrefsToSAF(
                uri,
                SpaEnvironmentFactory.instance.appContext,
                LocalSharedPrefsManager.getSharedPreferencesStorage(SpaEnvironmentFactory.instance.appContext),
                null
            )
        }
        val pickerThreeLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocument()
        ) { uri: Uri? ->
            SPUtils.importSharedPrefsFromSAF(
                uri,
                SpaEnvironmentFactory.instance.appContext,
                DefaultSharedPrefsManager.getSharedPreferencesStorage(SpaEnvironmentFactory.instance.appContext)
            )
        }
        val pickerFourLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.CreateDocument("text/xml")
        ) { uri: Uri? ->
            SPUtils.exportSharedPrefsToSAF(
                uri,
                SpaEnvironmentFactory.instance.appContext,
                DefaultSharedPrefsManager.getSharedPreferencesStorage(SpaEnvironmentFactory.instance.appContext),
                null
            )
        }
        RegularScaffold(title, actions = {
            MoreOptionsAction {
                val packageName = SpaEnvironmentFactory.instance.appContext.packageName
                MenuItem(stringResource(R.string.import_jlib_preferences)) {
                    pickerOneLauncher.launch(arrayOf("text/xml"))
                }
                MenuItem(stringResource(R.string.export_jlib_preferences)) {
                    pickerTwoLauncher.launch("Prefs-jLib-$packageName.xml")
                }
                MenuItem(stringResource(R.string.import_app_preferences)) {
                    pickerThreeLauncher.launch(arrayOf("text/xml"))
                }
                MenuItem(stringResource(R.string.export_app_preferences)) {
                    pickerFourLauncher.launch("Prefs-app-$packageName.xml")
                }
            }
        }) {
            Category {
                Preference(object : PreferenceModel {
                    override val title: String
                        get() = SpaEnvironmentFactory.instance.appContext.getString(R.string.rss_feed_urls)
                    override val onClick: (() -> Unit)
                        get() = {
                            val intent = Intent(
                                SpaEnvironmentFactory.instance.appContext,
                                RSSUrlsPreferenceActivity::class.java
                            )
                            intent.addFlags(FLAG_ACTIVITY_NEW_TASK)
                            SpaEnvironmentFactory.instance.appContext.startActivity(intent)
                        }
                })
            }
        }
    }
}

@Preview(showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL,
    wallpaper = Wallpapers.RED_DOMINATED_EXAMPLE
)
@Composable
private fun HomeScreenPreview() {
    SpaEnvironmentFactory.resetForPreview2()
    SettingsTheme {
        HomePageProvider.Page(null)
    }
}

@SuppressLint("ComposableNaming")
@Composable
private fun SpaEnvironmentFactory.resetForPreview2() {
    val context = LocalContext.current
    reset(FluxSpaEnv(context))
}