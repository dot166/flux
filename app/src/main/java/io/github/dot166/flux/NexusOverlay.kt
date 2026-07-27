package io.github.dot166.flux

import android.content.Context
import android.view.View
import androidx.compose.ui.platform.ComposeView
import com.android.settingslib.spa.framework.theme.SettingsTheme
import com.google.android.gsa.overlay.ui.panel.OverlayControllerSlidingPanelLayout
import com.nononsenseapps.feeder.base.diAwareViewModel
import com.nononsenseapps.feeder.ui.compose.utils.withAllProviders

class NexusOverlay(
    private val uid: Int,
    context: Context
): DIAwareMinusOneOverlay(
    uid,
    context
) {
    override fun content(): View {
        return ComposeView(this).apply {
            setContent {
                withAllProviders {
                    FeedScreen((slidingPanelLayout as OverlayControllerSlidingPanelLayout).diAwareViewModel())
                }
            }
        }
    }
}
