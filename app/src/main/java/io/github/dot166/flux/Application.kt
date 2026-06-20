package io.github.dot166.flux

import io.github.dot166.jlib.app.RestorableSettingsApplication

class Application : RestorableSettingsApplication() {

    override fun onCreate() {
        super.onCreate()
        setSpaEnvironment(FluxSpaEnv(this))
    }

}
