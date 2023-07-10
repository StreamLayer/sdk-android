package io.streamlayer.demo.common.exo

import android.content.Context
import android.net.Uri
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.source.BaseMediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.source.dash.DashMediaSource
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter
import com.google.android.exoplayer2.upstream.DefaultDataSource
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource
import com.google.android.exoplayer2.util.MimeTypes
import com.google.android.exoplayer2.util.Util
import io.streamlayer.common.extensions.toast
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.lang.Float.min

class ExoPlayerHelper(private val context: Context, private val appName: String) {

    private val bandwidthMeter by lazy { DefaultBandwidthMeter.Builder(context).build() }

    private val agent by lazy { Util.getUserAgent(context, appName) }

    private fun defaultDataSourceFactory(): DefaultDataSource.Factory =
        DefaultDataSource.Factory(context, DefaultHttpDataSource.Factory().setUserAgent(agent))

    private val errorListener = object : Player.Listener {

        override fun onPlayerError(error: PlaybackException) {
            context.toast("Exo error: core=${error.errorCode} message=$error.localizedMessage")
        }
    }


    val player: ExoPlayer by lazy { initPlayer() }

    fun init(url: String) {
        player.setMediaSource(buildMediaSource(url), 0)
        player.playWhenReady = true
        if (player.playbackState == Player.STATE_IDLE) player.prepare()
    }

    fun release() {
        player.removeListener(errorListener)
        player.removeListener(audioListener)
        player.release()
        volumeBeforeDucking = null
    }

    private fun initPlayer(): ExoPlayer {
        val trackSelector = DefaultTrackSelector(context)
        trackSelector.setParameters(trackSelector.buildUponParameters().setMaxVideoSizeSd())
        return ExoPlayer.Builder(context)
            .setBandwidthMeter(bandwidthMeter)
            .setTrackSelector(trackSelector)
            .setLoadControl(DefaultLoadControl())
            .build().apply {
                repeatMode = Player.REPEAT_MODE_ALL
                playWhenReady = true
                setForegroundMode(true)
                addListener(errorListener)
                addListener(audioListener)
            }
    }

    private fun buildMediaSource(streamUrl: String): BaseMediaSource {
        val builder = mediaItemBuilder(streamUrl)
        return when {
            isHlsStream(streamUrl) -> HlsMediaSource.Factory(defaultDataSourceFactory())
                .createMediaSource(builder.build())
            isDashStream(streamUrl) -> DashMediaSource.Factory(defaultDataSourceFactory())
                .createMediaSource(builder.build())
            else -> ProgressiveMediaSource.Factory(defaultDataSourceFactory())
                .createMediaSource(builder.build())
        }
    }

    private fun mediaItemBuilder(streamUrl: String) =
        MediaItem.Builder().setUri(Uri.parse(streamUrl)).apply {
            when {
                isHlsStream(streamUrl) -> setMimeType(MimeTypes.APPLICATION_M3U8)
                isDashStream(streamUrl) -> setMimeType(MimeTypes.APPLICATION_MPD)
                else -> setMimeType(MimeTypes.APPLICATION_MP4)
            }
        }

    private fun isHlsStream(streamUrl: String): Boolean = streamUrl.endsWith(".m3u8")

    private fun isDashStream(streamUrl: String): Boolean = streamUrl.endsWith(".mpd")

    // audio ducking support
    private var volumeBeforeDucking: Float? = null

    private val audioVolumeListener = MutableStateFlow(1f)

    private val audioListener = object : Player.Listener {
        override fun onVolumeChanged(volume: Float) {
            audioVolumeListener.value = volume
        }
    }

    fun getAudioVolumeListener(): Flow<Float> = audioVolumeListener.asStateFlow()

    fun notifyDuckingChanged(isEnabled: Boolean, level: Float = 0f) = with(player) {
        if (isEnabled) {
            if (volumeBeforeDucking == null) volumeBeforeDucking = volume
            volume = min(volume, level)
        } else volumeBeforeDucking?.let { volume ->
            this.volume = volume
            volumeBeforeDucking = null
        }
    }

    // epoch time code provider
    fun getEpochTimeCodeInMillis(): Long {
        val timeline = player.currentTimeline
        return if (!timeline.isEmpty) timeline.getWindow(
            player.currentWindowIndex, Timeline.Window()
        ).windowStartTimeMs + player.currentPosition
        else player.currentPosition
    }
}