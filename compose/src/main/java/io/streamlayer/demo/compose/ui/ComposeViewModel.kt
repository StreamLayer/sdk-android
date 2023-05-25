package io.streamlayer.demo.compose.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import com.google.android.exoplayer2.ExoPlayer
import io.streamlayer.demo.common.DEMO_HLS_STREAM
import io.streamlayer.demo.common.exo.ExoPlayerHelper
import io.streamlayer.demo.compose.App
import io.streamlayer.demo.compose.R
import io.streamlayer.sdk.SLRAppHost

class ComposeViewModel : ViewModel() {

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

    val appHostPlayer: SLRAppHost.Player
        get() = exoHelper.appHostPlayer

    init {
        exoHelper.init(DEMO_HLS_STREAM)
    }

    override fun onCleared() {
        exoHelper.release()
        super.onCleared()
    }

}