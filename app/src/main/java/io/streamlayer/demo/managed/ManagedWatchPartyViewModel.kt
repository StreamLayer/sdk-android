package io.streamlayer.demo.managed

import android.content.Context
import android.net.Uri
import com.google.android.exoplayer2.DefaultLoadControl
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.BaseMediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.MimeTypes
import com.google.android.exoplayer2.util.Util
import io.streamlayer.demo.App
import io.streamlayer.demo.common.ext.MviViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.filterNotNull
import kotlin.math.min


data class State(
    val selectedStream: String? = null, // url
)

class ManagedWatchPartyViewModel : MviViewModel<State>(State(), Dispatchers.Default) {

    val selectedStream = stateSlice { selectedStream }.filterNotNull()

    var isPlaybackPaused = false // check if player was stopped by user
    var isControlsVisible = false // check if player controls are visible
    private var volumeBeforeDucking: Float? = null

    private val context: Context
        get() = App.instance!!

    private val bandwidthMeter by lazy { DefaultBandwidthMeter.Builder(context).build() }

    private val agent by lazy { Util.getUserAgent(context, "streamlayer") }

    private fun defaultDataSourceFactory(): DefaultDataSourceFactory =
        DefaultDataSourceFactory(context, agent, bandwidthMeter)

    val player: ExoPlayer by lazy { initPlayer() }

    init {
        selectStream(App.DEMO_STREAM)
    }

    private fun selectStream(stream: String) {
        updateState { copy(selectedStream = stream) }
        val mediaSource = buildMediaSource(stream)
        player.setMediaSource(mediaSource, 0)
        player.playWhenReady = true
        if (player.playbackState == Player.STATE_IDLE) player.prepare()
        isPlaybackPaused = false
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

    fun notifyDuckingChanged(isEnabled: Boolean, level: Float = 0f) {
        player.audioComponent?.also { audio ->
            if (isEnabled) {
                if (volumeBeforeDucking == null) volumeBeforeDucking = audio.volume
                audio.volume = min(audio.volume, level)
            } else volumeBeforeDucking?.let { volume ->
                audio.volume = volume
                volumeBeforeDucking = null
            }
        }
    }
}