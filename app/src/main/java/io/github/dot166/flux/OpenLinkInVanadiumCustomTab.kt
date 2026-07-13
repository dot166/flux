package io.github.dot166.flux

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.nononsenseapps.feeder.base.DIAwareComponentActivity
import com.nononsenseapps.feeder.db.COL_LINK
import com.nononsenseapps.feeder.db.room.ID_UNSET
import com.nononsenseapps.feeder.model.cancelNotification
import com.nononsenseapps.feeder.util.ActivityLauncher
import kotlinx.coroutines.launch
import org.kodein.di.instance

/**
 * Proxy activity to mark item as read and notified in database as well as cancelling the
 * notification before performing a notification action such as opening in the browser.
 *
 * If link is null, then item is only marked as read and notified.
 */
class OpenLinkInVanadiumCustomTab : DIAwareComponentActivity() {
    private val viewModel: OpenLinkInVanadiumCustomTabViewModel by instance(arg = this)
    private val activityLauncher: ActivityLauncher by instance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        intent?.let { intent ->
            val id: Long = intent.data?.lastPathSegment?.toLong() ?: ID_UNSET
            val link: String? = intent.data?.getQueryParameter(COL_LINK)

            lifecycleScope.launch {
                viewModel.markAsReadAndNotified(id)
                cancelNotification(this@OpenLinkInVanadiumCustomTab, id)
            }

            if (link != null) {
                try {
                    activityLauncher.openLinkInCustomTab(
                        link,
                        -1,
                        openAdjacentIfSuitable = false,
                    )
                } catch (e: Throwable) {
                    e.printStackTrace()
                    Toast.makeText(this, R.string.no_activity_for_link, Toast.LENGTH_SHORT).show()
                    Log.e("RSSREADEROpenInVanadium", "Failed to start browser", e)
                }
            }
        }

        // Terminate activity immediately
        finish()
    }
}
