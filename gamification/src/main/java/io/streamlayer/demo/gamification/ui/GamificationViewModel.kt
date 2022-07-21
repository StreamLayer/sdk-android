package io.streamlayer.demo.gamification.ui

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.exoplayer2.ExoPlayer
import io.streamlayer.demo.common.DEMO_HLS_STREAM
import io.streamlayer.demo.common.exo.ExoPlayerHelper
import io.streamlayer.demo.gamification.App
import io.streamlayer.demo.gamification.BuildConfig
import io.streamlayer.demo.gamification.R
import io.streamlayer.sdk.SLREventSession
import io.streamlayer.sdk.StreamLayer
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

private const val TAG = "GamificationViewModel"

class GamificationViewModel : ViewModel() {

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
        createEventSession(BuildConfig.SL_EVENT_ID)
        StreamLayer.addAudioDuckingListener(exoHelper)
    }

    private var createEventSessionJob: Job? = null
    private var eventSession: SLREventSession? = null

    private fun createEventSession(id: String) {
        if (eventSession?.getExternalEventId() == id) return
        createEventSessionJob?.cancel()
        createEventSessionJob = viewModelScope.launch {
            try {
                eventSession?.release()
                eventSession = StreamLayer.createEventSession(id, exoHelper)
            } catch (t: Throwable) {
                Log.e(TAG, "createEventSession failed:", t)
            }
        }
    }

    override fun onCleared() {
        exoHelper.release()
        eventSession?.release()
        StreamLayer.removeAudioDuckingListener(exoHelper)
        super.onCleared()
    }

}