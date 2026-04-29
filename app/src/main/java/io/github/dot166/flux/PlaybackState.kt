package io.github.dot166.flux

import androidx.media3.common.MediaItem

data class PlaybackState(
    val items: List<MediaItem>,
    val index: Int,
    val position: Long
)
