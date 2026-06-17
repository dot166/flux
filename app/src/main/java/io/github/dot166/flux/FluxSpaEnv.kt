package io.github.dot166.flux

import android.content.Context
import com.android.settingslib.spa.framework.common.SettingsPageProviderRepository
import com.android.settingslib.spa.framework.common.createSettingsPage
import io.github.dot166.jlib.app.JLibSpaEnvironment

class FluxSpaEnv(ctx: Context): JLibSpaEnvironment(ctx) {
    override val pageProviderRepository = lazy {
        SettingsPageProviderRepository(
            allPageProviders =
                listOf(
                    HomePageProvider,
                ),
            rootPages = listOf(HomePageProvider.createSettingsPage()),
        )
    }
}
