package io.github.dot166.flux

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.graphics.BitmapFactory
import android.util.Log
import android.widget.RemoteViews
import androidx.annotation.OptIn
import androidx.core.content.edit
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionError
import androidx.preference.PreferenceManager
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.AppWidgetTarget
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.guava.future
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RssAudioService : MediaLibraryService() {

    private lateinit var player: ExoPlayer
    private lateinit var session: MediaLibrarySession
    private lateinit var savedMediaItems: List<MediaItem>
    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.Main + job)

    override fun onCreate() {
        super.onCreate()

        val repository = Repository.getInstance(this)

        repository.cleanupSavedPositions()

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
            .build()
        player = ExoPlayer.Builder(this)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .setSeekBackIncrementMs(10000)
            .setSeekForwardIncrementMs(10000)
            .build()
        scope.launch {
            val state = withContext(Dispatchers.IO) {
                repository.getRestoredPlaybackState()
            }

            state?.let { (items, index, position) ->
                savedMediaItems = items
                player.setMediaItems(items, index, position)
                player.prepare()
            }
        }

        player.addListener(object: Player.Listener {
            override fun onMediaItemTransition(item: MediaItem?, reason: Int) {
                player.saveQueue(PreferenceManager.getDefaultSharedPreferences(this@RssAudioService), savedMediaItems, repository)
            }

            override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
                updateWidget()
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                player.saveQueue(PreferenceManager.getDefaultSharedPreferences(this@RssAudioService), savedMediaItems, repository)
                updateWidget()
            }
        })
        updateWidget()

        val rootItem = MediaItem.Builder()
            .setMediaId("root")
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(getString(R.string.podcasts))
                    .setIsBrowsable(true)
                    .setIsPlayable(false)
                    .build()
            )
            .build()

        val rootPodcastsItem = MediaItem.Builder()
            .setMediaId("podcasts")
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(getString(R.string.podcasts))
                    .setIsBrowsable(true)
                    .setIsPlayable(false)
                    .build()
            )
            .build()

        val callback = object : MediaLibrarySession.Callback {

            override fun onGetLibraryRoot(
                session: MediaLibrarySession,
                browser: MediaSession.ControllerInfo,
                params: LibraryParams?
            ): ListenableFuture<LibraryResult<MediaItem>> {
                Log.i("AUTO - dbg", "onGetLibraryRoot()")
                Log.i("AUTO - dbg", "noID, this is root")
                return Futures.immediateFuture(LibraryResult.ofItem(rootItem, params))
            }

            @OptIn(UnstableApi::class)
            override fun onGetChildren(
                session: MediaLibrarySession,
                browser: MediaSession.ControllerInfo,
                parentId: String,
                page: Int,
                pageSize: Int,
                params: LibraryParams?
            ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
                Log.i("AUTO - dbg", "onGetChildren() for parentId: $parentId")

                return scope.future {
                    try {
                        val items: List<MediaItem> = when (parentId) {
                            "root" -> listOf(rootPodcastsItem)
                            "podcasts" -> repository.getPodcasts()
                            else -> {
                                if (parentId.startsWith("podcast:")) {
                                    val podcast = parentId.removePrefix("podcast:")
                                    repository.getPodcastEpisodes(podcast).map { item ->
                                        item.buildUpon().setMediaId("AUTO|${item.mediaId}").build()
                                    }
                                } else {
                                    emptyList()
                                }
                            }
                        }

                        LibraryResult.ofItemList(ImmutableList.copyOf(items), params)
                    } catch (e: Exception) {
                        Log.e("AUTO - dbg", "Failed to load children", e)
                        LibraryResult.ofError(SessionError.ERROR_UNKNOWN)
                    }
                }
            }

            @OptIn(UnstableApi::class)
            override fun onSetMediaItems(
                mediaSession: MediaSession,
                controller: MediaSession.ControllerInfo,
                mediaItems: MutableList<MediaItem>,
                startIndex: Int,
                startPositionMs: Long
            ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {

                val firstItem = mediaItems.firstOrNull() ?: return super.onSetMediaItems(mediaSession, controller, mediaItems, startIndex, startPositionMs)

                if (firstItem.mediaId.contains("|")) {
                    val parts = firstItem.mediaId.split("|")
                    val parentId = parts[1].split(":")[1]
                    val realId = parts[1]

                    val fullQueue = repository.getPodcastEpisodes(parentId)
                    val index = fullQueue.indexOfFirst { it.mediaId == realId }.coerceAtLeast(0)
                    return Futures.immediateFuture(
                        MediaSession.MediaItemsWithStartPosition(fullQueue, index, startPositionMs)
                    )
                }

                return super.onSetMediaItems(mediaSession, controller, mediaItems, startIndex, startPositionMs)
            }

            override fun onAddMediaItems(
                mediaSession: MediaSession,
                controller: MediaSession.ControllerInfo,
                mediaItems: MutableList<MediaItem>
            ): ListenableFuture<MutableList<MediaItem>> {
                val resolvedItems = mediaItems.map { item ->
                    if (item.localConfiguration != null) return@map item
                    val id = item.mediaId.removePrefix("episode:")
                    repository.searchPodcastEpisodes(id)?.firstOrNull() ?: item
                }.toMutableList()

                savedMediaItems = resolvedItems

                return Futures.immediateFuture(resolvedItems)
            }
        }

        session = MediaLibrarySession.Builder(this, player, callback)
            .setSessionActivity(PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE))
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession {
        return session
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "ACTION_TOGGLE_PLAY_PAUSE" -> {
                startService(Intent(this, this::class.java))
                when {
                    player.playbackState == Player.STATE_ENDED -> {
                        player.seekTo(0)
                        player.play()
                    }
                    player.isPlaying -> {
                        player.pause()
                    }
                    else -> {
                        player.play()
                    }
                }
                updateWidget()
            }
            "ACTION_FAST_FORWARD" -> {
                startService(Intent(this, this::class.java))
                player.seekForward()
                updateWidget()
            }
            "ACTION_REWIND" -> {
                startService(Intent(this, this::class.java))
                player.seekBack()
                updateWidget()
            }
            "ACTION_START_ACTIVITY" -> {
                startActivity(Intent(this, MainActivity::class.java))
                updateWidget()
            }
            "UPDATE" -> {
                updateWidget()
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    @OptIn(UnstableApi::class)
    fun updateWidget() {
        val manager = AppWidgetManager.getInstance(this)
        val ids = manager.getAppWidgetIds(
            ComponentName(this, MediaControlsWidget::class.java)
        )
        Log.d("MusicWidget", ids.size.toString())
        if (ids.isEmpty()) return
        val views = RemoteViews(packageName, R.layout.widget)
        val mediaMetadata = player.mediaMetadata
        val artBytes = mediaMetadata.artworkUri
        if (artBytes != null) {
            val appWidgetTarget = AppWidgetTarget(this, R.id.art, views, *ids)
            Glide.with(this)
                .asBitmap()
                .load(artBytes)
                .into(appWidgetTarget)
        } else {
            views.setImageViewResource(R.id.art, R.drawable.def_art)
        }
        views.setTextViewText(R.id.line1, mediaMetadata.title ?: "")
        views.setTextViewText(R.id.line2, mediaMetadata.artist ?: "")
        if (player.isPlaying) {
            views.setImageViewResource(
                R.id.button6,
                androidx.media3.session.R.drawable.media3_icon_pause
            )
        } else {
            views.setImageViewResource(
                R.id.button6,
                androidx.media3.session.R.drawable.media3_icon_play
            )
        }
        views.setOnClickPendingIntent(
            R.id.button5,
            PendingIntent.getForegroundService(
                this,
                1,
                Intent(this, RssAudioService::class.java).apply {
                    action = "ACTION_REWIND"
                }, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        )
        views.setOnClickPendingIntent(
            R.id.button6,
            PendingIntent.getForegroundService(
                this,
                2,
                Intent(this, RssAudioService::class.java).apply {
                    action = "ACTION_TOGGLE_PLAY_PAUSE"
                }, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        )
        views.setOnClickPendingIntent(
            R.id.button7,
            PendingIntent.getForegroundService(
                this,
                3,
                Intent(this, RssAudioService::class.java).apply {
                    action = "ACTION_FAST_FORWARD"
                }, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        )
        views.setOnClickPendingIntent(
            R.id.layout,
            PendingIntent.getActivity(
                this,
                4,
                Intent(this, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        )
        manager.updateAppWidget(ids, views)
    }

    override fun onDestroy() {
        session.release()
        player.release()
        super.onDestroy()
        job.cancel()
    }
}
