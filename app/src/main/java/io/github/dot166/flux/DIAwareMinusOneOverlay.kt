package io.github.dot166.flux

import android.content.Context
import com.nononsenseapps.feeder.util.ActivityLauncher
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.android.closestDI
import org.kodein.di.bind
import org.kodein.di.direct
import org.kodein.di.instance
import org.kodein.di.singleton

abstract class DIAwareMinusOneOverlay(uid: Int, context: Context) :
    com.google.android.gsa.overlay.NexusOverlay(uid, context),
    DIAware {
    private val parentDI: DI by closestDI()
    override val di: DI by DI.lazy {
        extend(parentDI)
        bind<DIAwareMinusOneOverlay>() with instance(this@DIAwareMinusOneOverlay)
        bind<ActivityLauncher>() with
                singleton {
                    ActivityLauncher(
                        this@DIAwareMinusOneOverlay,
                        di.direct.instance(),
                    )
                }
    }
}
