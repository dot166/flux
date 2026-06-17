package io.github.dot166.flux

import android.app.Application
import com.android.settingslib.spa.framework.common.SettingsPageProviderRepository
import io.github.dot166.jlib.app.JLibSpaEnvironment
import io.github.dot166.jlib.app.RestorableSettingsApplication

class Application : RestorableSettingsApplication() {

    override fun onCreate() {
        super.onCreate()
        setSpaEnvironment(FluxSpaEnv(this))
    }

}
