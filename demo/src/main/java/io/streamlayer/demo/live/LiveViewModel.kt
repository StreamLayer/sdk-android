package io.streamlayer.demo.live

import android.content.Context
import android.util.Log
import androidx.lifecycle.viewModelScope
import com.google.android.exoplayer2.ExoPlayer
import io.streamlayer.demo.App
import io.streamlayer.demo.R
import io.streamlayer.demo.common.DEMO_HLS_STREAM
import io.streamlayer.demo.common.exo.ExoPlayerHelper
import io.streamlayer.demo.common.ext.BaseErrorEvent
import io.streamlayer.demo.common.ext.MviViewModel
import io.streamlayer.sdk.SLRAppHost
import io.streamlayer.sdk.SLREventSession
import io.streamlayer.sdk.StreamLayer
import io.streamlayer.sdk.base.StreamLayerDemo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "LiveViewModel"

data class Stream(val id: String, val url: String)

data class State(
    val selectedStream: Stream? = null
)

class LiveViewModel : MviViewModel<State>(State(), Dispatchers.Default) {

    val selectedStream = stateSlice { selectedStream }.filterNotNull()

    var isPlaybackPaused = false // check if player was stopped by user
    var isControlsVisible = false // check if player controls are visible

    private val context: Context
        get() = App.instance!!

    private var createEventSessionJob: Job? = null
    private var eventSession: SLREventSession? = null

    private val exoHelper: ExoPlayerHelper by lazy {
        ExoPlayerHelper(context, context.getString(R.string.app_name)) { id ->
            // sdk requested new event - process it's if needed
        }
    }

    val player: ExoPlayer
        get() = exoHelper.player

    val appHostPlayer: SLRAppHost.Player
        get() = exoHelper.appHostPlayer

    init {
        loadDemoStream()
    }

    private fun loadDemoStream() {
        viewModelScope.launch {
            // don't change date - it's for testing purposes
            val result =
                withContext(Dispatchers.IO) { kotlin.runCatching { StreamLayerDemo.getDemoStreams("2022-01-01") } }
            result.getOrNull()?.let { list ->
                list.firstOrNull()
                    ?.let { selectStream(Stream(it.eventId.toString(), DEMO_HLS_STREAM)) }
            } ?: kotlin.run {
                result.exceptionOrNull()?.let { Log.e(TAG, "can not load stream", it) }
                _viewEvents.trySend(BaseErrorEvent("Can not load stream"))
            }
        }
    }

    private fun selectStream(stream: Stream) {
        updateState { copy(selectedStream = stream) }
        exoHelper.init(stream.url)
        createEventSession(stream)
        isPlaybackPaused = false
    }

    private fun createEventSession(stream: Stream) {
        if (eventSession?.getExternalEventId() == stream.id) return
        createEventSessionJob?.cancel()
        createEventSessionJob = viewModelScope.launch {
            try {
                eventSession?.release()
                eventSession = StreamLayer.createEventSession(stream.id, exoHelper)
            } catch (t: Throwable) {
                Log.e(TAG, "createEventSession failed:", t)
            }
        }
    }

    override fun onCleared() {
        player.release()
        eventSession?.release()
        super.onCleared()
    }

    fun verifyEventSession() {
        // check and recreate new event session if needed
        currentState.selectedStream?.let {
            if (eventSession == null || eventSession!!.isReleased()) selectStream(it)
        }
    }
}