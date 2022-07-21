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
import io.streamlayer.sdk.SLRAudioDuckingListener
import io.streamlayer.sdk.SLRTimeCodeProvider
import java.lang.Float.min

class ExoPlayerHelper(private val context: Context, private val appName: String) :
    SLRAudioDuckingListener, SLRTimeCodeProvider {

    private val bandwidthMeter by lazy { DefaultBandwidthMeter.Builder(context).build() }

    private val agent by lazy { Util.getUserAgent(context, appName) }

    private fun defaultDataSourceFactory(): DefaultDataSource.Factory =
        DefaultDataSource.Factory(context, DefaultHttpDataSource.Factory().setUserAgent(agent))

    val player: ExoPlayer by lazy { initPlayer() }

    fun init(url: String) {
        player.setMediaSource(buildMediaSource(url), 0)
        player.playWhenReady = true
        if (player.playbackState == Player.STATE_IDLE) player.prepare()
    }

    fun release() {
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

    // audio ducking implementation
    private var volumeBeforeDucking: Float? = null

    override fun requestAudioDucking(level: Float) {
        notifyDuckingChanged(true, level)
    }

    override fun disableAudioDucking() {
        notifyDuckingChanged(false)
    }

    private fun notifyDuckingChanged(isEnabled: Boolean, level: Float = 0f) = with(player) {
        if (isEnabled) {
            if (volumeBeforeDucking == null) volumeBeforeDucking = volume
            volume = min(volume, level)
        } else volumeBeforeDucking?.let { volume ->
            this.volume = volume
            volumeBeforeDucking = null
        }
    }

    // timecode provider implementation
    override fun getEpochTimeCodeInMillis(): Long {
        val timeline = player.currentTimeline
        return if (!timeline.isEmpty) timeline.getWindow(
            player.currentWindowIndex, Timeline.Window()
        ).windowStartTimeMs + player.currentPosition
        else player.currentPosition
    }
}