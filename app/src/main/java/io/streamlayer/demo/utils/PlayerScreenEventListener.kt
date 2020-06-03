package io.streamlayer.demo.utils

import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ui.PlayerView

class PlayerScreenEventListener(private val playerView: PlayerView) : Player.EventListener {

    override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
        playerView.keepScreenOn =
            !(playbackState == Player.STATE_IDLE || playbackState == Player.STATE_ENDED || !playWhenReady)
    }

}