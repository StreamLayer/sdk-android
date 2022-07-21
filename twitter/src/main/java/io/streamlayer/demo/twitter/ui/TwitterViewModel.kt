package io.streamlayer.demo.twitter.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import com.google.android.exoplayer2.ExoPlayer
import io.streamlayer.demo.common.DEMO_HLS_STREAM
import io.streamlayer.demo.common.exo.ExoPlayerHelper
import io.streamlayer.demo.twitter.App
import io.streamlayer.demo.twitter.R
import io.streamlayer.sdk.StreamLayer

class TwitterViewModel : ViewModel() {

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
        exoHelper.init(DEMO_HLS_STREAM)
        StreamLayer.addAudioDuckingListener(exoHelper)
    }

    override fun onCleared() {
        exoHelper.release()
        StreamLayer.removeAudioDuckingListener(exoHelper)
        super.onCleared()
    }

}