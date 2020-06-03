package io.streamlayer.demo.utils

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.ui.PlayerView

class PlayerLifecycleObserver(
    private val exoPlayer: ExoPlayer,
    private val playerView: PlayerView,
    private val onGetCurrentTrack: () -> MediaSource?
) : LifecycleObserver {

    var stopPlaybackOnPause: Boolean = true

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    fun onPause() {
        if (stopPlaybackOnPause) {
            exoPlayer.stop(false)
            playerView.onPause()
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    fun onResume() {
        if (stopPlaybackOnPause) {
            playerView.onResume()
            onGetCurrentTrack()?.let { exoPlayer.prepare(it, false, false) }
        }
        stopPlaybackOnPause = true
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun onDestroy() {
        exoPlayer.release()
    }

}