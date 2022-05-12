package io.streamlayer.demo.live

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.viewModelScope
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.source.BaseMediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.MimeTypes
import com.google.android.exoplayer2.util.Util
import io.streamlayer.demo.App
import io.streamlayer.demo.common.ext.BaseErrorEvent
import io.streamlayer.demo.common.ext.MviViewModel
import io.streamlayer.sdk.SLREventSession
import io.streamlayer.sdk.SLRTimeCodeProvider
import io.streamlayer.sdk.StreamLayer
import io.streamlayer.sdk.base.StreamLayerDemo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.min

private const val TAG = "LiveViewModel"

data class Stream(val id: String, val url: String)

data class State(
    val selectedStream: Stream? = null
)

class LiveViewModel : MviViewModel<State>(State(), Dispatchers.Default) {

    val selectedStream = stateSlice { selectedStream }.filterNotNull()

    var isPlaybackPaused = false // check if player was stopped by user
    var isControlsVisible = false // check if player controls are visible
    private var volumeBeforeDucking: Float? = null

    private var eventSession: SLREventSession? = null

    private val context: Context
        get() = App.instance!!

    private val bandwidthMeter by lazy { DefaultBandwidthMeter.Builder(context).build() }

    private val agent by lazy { Util.getUserAgent(context, "streamlayer") }

    private var createEventSessionJob: Job? = null

    private fun defaultDataSourceFactory(): DefaultDataSourceFactory = DefaultDataSourceFactory(context, agent, bandwidthMeter)

    val player: ExoPlayer by lazy { initPlayer() }

    init {
        loadFakeStreams()
    }

    private fun loadFakeStreams() {
        viewModelScope.launch {
            // don't change date - it's for testing purposes
            val result =
                withContext(Dispatchers.IO) { kotlin.runCatching { StreamLayerDemo.getDemoStreams("2022-01-01") } }
            result.getOrNull()?.let { list ->
                list.firstOrNull()?.let { selectStream(Stream(it.eventId.toString(), App.DEMO_STREAM)) }
            } ?: kotlin.run {
                result.exceptionOrNull()?.let { Log.e(TAG, "can not load stream", it) }
                _viewEvents.trySend(BaseErrorEvent("Can not load stream"))
            }
        }
    }

    private fun selectStream(stream: Stream) {
        updateState { copy(selectedStream = stream) }
        val mediaSource = buildMediaSource(stream.url)
        player.setMediaSource(mediaSource, 0)
        player.playWhenReady = true
        if (player.playbackState == Player.STATE_IDLE) player.prepare()
        createEventSession(stream)
        isPlaybackPaused = false
    }

    private fun createEventSession(stream: Stream) {
        if (eventSession?.getExternalEventId() == stream.id) return
        createEventSessionJob?.cancel()
        createEventSessionJob = viewModelScope.launch {
            try {
                eventSession?.release()
                eventSession = StreamLayer.createEventSession(stream.id, getTimeCodeProvider())
            } catch (t: Throwable) {
                Log.e(TAG, "createEventSession failed:", t)
            }
        }
    }

    override fun onCleared() {
        player.release()
        super.onCleared()
    }

    private fun initPlayer(): ExoPlayer {
        val trackSelector = DefaultTrackSelector(context)
        trackSelector.setParameters(trackSelector.buildUponParameters().setMaxVideoSizeSd())
        return SimpleExoPlayer.Builder(context)
            .setBandwidthMeter(bandwidthMeter)
            .setTrackSelector(trackSelector)
            .setLoadControl(DefaultLoadControl())
            .build().apply {
                repeatMode = Player.REPEAT_MODE_ALL
                playWhenReady = true
                setForegroundMode(true)
            }
    }

    private fun buildMediaSource(streamUrl: String): BaseMediaSource {
        val builder = mediaItemBuilder(streamUrl)
        return when {
            isHlsStream(streamUrl) -> {
                HlsMediaSource.Factory(defaultDataSourceFactory())
                    .createMediaSource(builder.build())
            }
            else -> {
                ProgressiveMediaSource.Factory(defaultDataSourceFactory())
                    .createMediaSource(builder.build())
            }
        }
    }

    private fun mediaItemBuilder(streamUrl: String) =
        MediaItem.Builder().setUri(Uri.parse(streamUrl)).apply {
            when {
                isHlsStream(streamUrl) -> setMimeType(MimeTypes.APPLICATION_M3U8)
                else -> setMimeType(MimeTypes.APPLICATION_MP4)
            }
        }

    private fun isHlsStream(streamUrl: String): Boolean = streamUrl.endsWith(".m3u8")

    // timecode provider
    private fun getTimeCodeProvider(): SLRTimeCodeProvider = object : SLRTimeCodeProvider {
        override fun getEpochTimeCodeInMillis(): Long {
            val timeline = player.currentTimeline
            return if (!timeline.isEmpty) timeline.getWindow(
                player.currentWindowIndex, Timeline.Window()
            ).windowStartTimeMs + player.currentPosition
            else player.currentPosition
        }
    }

    fun notifyDuckingChanged(isEnabled: Boolean) {
        player.audioComponent?.also { audio ->
            if (isEnabled) {
                if (volumeBeforeDucking == null) {
                    // decrease volume to 10% if louder, otherwise keep the current volume
                    volumeBeforeDucking = audio.volume
                    audio.volume = min(audio.volume, 0.1f)
                }
            } else volumeBeforeDucking?.let { volume ->
                // reset volume
                audio.volume = volume
                volumeBeforeDucking = null
            }
        }
    }

    fun verifyEventSession() {
        // check and recreate new event session if needed
        currentState.selectedStream?.let {
            if (eventSession == null || eventSession!!.isReleased()) selectStream(it)
        }
    }

    fun releaseEventSession() {
        eventSession?.release()
    }
}