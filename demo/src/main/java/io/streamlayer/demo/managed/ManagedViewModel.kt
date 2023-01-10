package io.streamlayer.demo.managed

import android.content.Context
import androidx.lifecycle.ViewModel
import com.google.android.exoplayer2.ExoPlayer
import io.streamlayer.demo.App
import io.streamlayer.demo.R
import io.streamlayer.demo.common.DEMO_HLS_STREAM
import io.streamlayer.demo.common.exo.ExoPlayerHelper
import io.streamlayer.sdk.StreamLayer

class ManagedViewModel : ViewModel() {

    var isPlaybackPaused = false // check if player was stopped by user
    var isControlsVisible = false // check if player controls are visible

    private val context: Context
        get() = App.instance!!

    private val exoHelper: ExoPlayerHelper by lazy {
        ExoPlayerHelper(
            context,
            context.getString(R.string.app_name)
        )
    }

    val player: ExoPlayer
        get() = exoHelper.player

    init {
        selectStream(DEMO_HLS_STREAM)
        StreamLayer.addAudioDuckingListener(exoHelper)
    }

    private fun selectStream(stream: String) {
        exoHelper.init(stream)
        isPlaybackPaused = false
    }

    override fun onCleared() {
        exoHelper.release()
        StreamLayer.removeAudioDuckingListener(exoHelper)
        super.onCleared()
    }
}