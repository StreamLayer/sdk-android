package io.streamlayer.demo.twitter.ui

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.exoplayer2.ExoPlayer
import io.streamlayer.demo.common.DEMO_HLS_STREAM
import io.streamlayer.demo.common.exo.ExoPlayerHelper
import io.streamlayer.demo.twitter.App
import io.streamlayer.demo.twitter.R
import io.streamlayer.sdk.SLRAppHost
import io.streamlayer.sdk.SLREventSession
import io.streamlayer.sdk.StreamLayer
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

private const val TAG = "TwitterViewModel"

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

    val appHostPlayer: SLRAppHost.Player
        get() = exoHelper.appHostPlayer

    init {
        exoHelper.init(DEMO_HLS_STREAM)
        createEventSession(App.DEMO_EVENT_ID)
    }

    private var createEventSessionJob: Job? = null
    private var eventSession: SLREventSession? = null

    private fun createEventSession(id: String) {
        if (eventSession?.getExternalEventId() == id) return
        createEventSessionJob?.cancel()
        createEventSessionJob = viewModelScope.launch {
            try {
                eventSession?.release()
                eventSession = StreamLayer.createEventSession(id, null)
            } catch (t: Throwable) {
                Log.e(TAG, "createEventSession failed:", t)
            }
        }
    }

    override fun onCleared() {
        exoHelper.release()
        eventSession?.release()
        super.onCleared()
    }

}