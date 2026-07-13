package io.github.dot166.flux

import com.nononsenseapps.feeder.archmodel.Repository
import com.nononsenseapps.feeder.base.DIAwareViewModel
import org.kodein.di.DI
import org.kodein.di.instance

class OpenLinkInVanadiumCustomTabViewModel(
    di: DI,
) : DIAwareViewModel(di) {
    private val repository: Repository by instance()

    suspend fun markAsReadAndNotified(itemId: Long) {
        repository.markAsReadAndNotified(itemId)
    }
}
