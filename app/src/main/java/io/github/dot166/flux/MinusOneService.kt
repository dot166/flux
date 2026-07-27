package io.github.dot166.flux

import android.content.Context

class MinusOneService: com.google.android.gsa.overlay.MinusOneService() {

    override fun getOverlay(uid: Int, context: Context): com.google.android.gsa.overlay.NexusOverlay {
        return NexusOverlay(uid, context)
    }
}
