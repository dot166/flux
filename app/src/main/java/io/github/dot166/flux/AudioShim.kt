package io.github.dot166.flux

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.preference.PreferenceManager
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.common.util.concurrent.MoreExecutors
import io.github.dot166.jlib.app.jActivity

class AudioShim: jActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val repo = Repository.getInstance(this)
        startService(Intent(this, RssAudioService::class.java))
        val token = SessionToken(this, ComponentName(this, RssAudioService::class.java))
        val future = MediaController.Builder(this, token).buildAsync()
        future.addListener({
            val ctrl = future.get()
            val items =
                repo.searchPodcastEpisodesByUrl(
                    intent.extras!!.getString("url")!!
                )!!
            ctrl.setMediaItems(
                items.first, items.second,
                PreferenceManager.getDefaultSharedPreferences(
                    this
                ).getLong(
                    "episode_${items.first[0].mediaMetadata.station?.toString()}_${items.second}_position",
                    0
                )
            )
            ctrl.prepare()
            ctrl.play()
            finish()
        }, MoreExecutors.directExecutor())
        setContentView(CircularProgressIndicator(this))
    }
}