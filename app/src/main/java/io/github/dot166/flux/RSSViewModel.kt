package io.github.dot166.flux

import android.app.Application
import android.content.ComponentName
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.MoreExecutors
import com.prof18.rssparser.model.RssChannel
import io.github.dot166.jlib.RSSFeed
import io.github.dot166.jlib.compose.model.MediaViewModel
import io.github.dot166.jlib.utils.DateUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RSSViewModel(app: Application) : AndroidViewModel(app), MediaViewModel {
    override val isRss = true
    private val refreshingFlow = MutableStateFlow(true)
    private val flow = MutableStateFlow<RSSFeed?>(null)
    private val listFlow = MutableStateFlow<MutableMap<Int, RSSFeed?>>(mutableMapOf())

    override var controller by mutableStateOf<Player?>(null)

    override var currentPosition by mutableLongStateOf(0L)
    override var duration by mutableLongStateOf(0L)
    override var isPlaying by mutableStateOf(false)
    override var mediaMetadata by mutableStateOf(MediaMetadata.EMPTY)

    init {
        refreshAll(app)
    }

    val state = combine(refreshingFlow, flow, listFlow) { refreshing, data, list ->
        RefreshAwareState(refreshing, data, list)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = RefreshAwareState(refreshingFlow.value, flow.value, listFlow.value)
    )

    fun refreshAll(ctx: Context) = viewModelScope.launch(Dispatchers.IO) {
        refreshingFlow.value = true
        listFlow.update { it.toMutableMap().apply {
            clear()
            put(0, null) // dummy
        } }
        val repo = Repository.getInstance(ctx)
        val urls = repo.getFeeds()
        val allFeeds = fetchAllFeeds(urls, ctx)
        listFlow.update { it.toMutableMap().apply {
            put(0, allFeeds)
        } }
        flow.emit(listFlow.value[0])
        repo.updateCache()
        refreshingFlow.value = false
    }

    fun refresh(feed: RSSFeed, ctx: Context) = viewModelScope.launch(Dispatchers.IO) {
        refreshingFlow.value = true
        val repo = Repository.getInstance(ctx)
        var key = 0
        listFlow.update { it.toMutableMap().apply { key = filterValues { urlInReg -> urlInReg!!.url == feed.url }.keys.first() } }
        listFlow.update { it.toMutableMap().apply { put(key, repo.fetchFeed(feed)) } }
        flow.emit(listFlow.value[key])
        repo.updateCache()
        refreshingFlow.value = false
    }

    suspend fun fetchAllFeeds(urls: List<RSSFeed>, ctx: Context): RSSFeed = coroutineScope {
        val repo = Repository.getInstance(ctx)
        val deferredFeeds = urls.mapIndexed { index, url ->
            async {
                val channel = repo.fetchFeed(url)
                listFlow.update { it.toMutableMap().apply { put(index + 1, channel) } }
                channel
            }
        }
        val results = deferredFeeds.awaitAll()
        val allItems = results.flatMap { channel ->
            if (channel.channel != null && channel.channel!!.items.isNotEmpty() && (channel.channel!!.items[0].title != "Error Handler" && channel.channel!!.title != "Error Handler") && !channel.hiddenFromAll) {
                channel.channel!!.items
            } else {
                emptyList()
            }
        }.sortedByDescending { item ->
            DateUtils.convertDateToEpochSeconds(
                item.pubDate ?: "1970-01-01 09:00:00 +0900"
            )
        }

        val channel = RssChannel("All Feeds", null, null, null, null, null, allItems, null, null)
        RSSFeed(true, "", channel, false)
    }

    override fun connectController(context: Context) {
        val token = SessionToken(context, ComponentName(context, RssAudioService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()

        future.addListener({
            val ctrl = future.get()
            controller = ctrl
            updateState(ctrl)

            ctrl.addListener(object : Player.Listener {
                override fun onEvents(player: Player, events: Player.Events) {
                    updateState(ctrl)
                }
            })
        }, MoreExecutors.directExecutor())
    }

    private fun updateState(ctrl: MediaController) {
        isPlaying = ctrl.isPlaying
        duration = ctrl.duration.coerceAtLeast(0L)
        mediaMetadata = ctrl.mediaMetadata
    }

    override suspend fun pollPosition() {
        while (true) {
            controller?.let {
                currentPosition = it.currentPosition
            }
            delay(500)
        }
    }
    override fun toggleRepeatMode() {
        // unsupported
    }

    override fun toggleShuffleMode() {
        // unsupported
    }
}