package io.streamlayer.demo.common.exo

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout
import com.google.android.exoplayer2.ui.StyledPlayerView
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter
import com.google.android.exoplayer2.upstream.DefaultDataSource
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource
import com.google.android.exoplayer2.util.MimeTypes
import com.google.android.exoplayer2.util.Util
import io.streamlayer.demo.common.R
import io.streamlayer.sdk.SLRVideoPlayer
import io.streamlayer.sdk.SLRVideoPlayerProvider
import io.streamlayer.sdk.SLRVideoPlayerView

class ExoVideoPlayer(internal val simpleExoPlayer: ExoPlayer) : SLRVideoPlayer {

    // keep it for mapping different listeners
    private val listeners: MutableList<Pair<SLRVideoPlayer.Listener, Player.Listener>> =
        mutableListOf()

    override fun play() {
        simpleExoPlayer.playWhenReady = true
    }

    override fun pause() {
        simpleExoPlayer.playWhenReady = false
    }

    override fun isPlaying(): Boolean = simpleExoPlayer.isPlaying

    override fun release() {
        simpleExoPlayer.release()
    }

    override fun seekTo(position: Long) {
        simpleExoPlayer.seekTo(position)
    }

    override fun getCurrentPosition(): Long {
        return simpleExoPlayer.currentPosition
    }

    override fun getDuration(): Long {
        return simpleExoPlayer.duration
    }

    override fun addListener(listener: SLRVideoPlayer.Listener) {
        val exoListener = object : Player.Listener {

            override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
                listener.onPlayerStateChanged(
                    when (playbackState) {
                        Player.STATE_BUFFERING -> SLRVideoPlayer.State.BUFFERING
                        Player.STATE_READY -> SLRVideoPlayer.State.READY
                        Player.STATE_ENDED -> SLRVideoPlayer.State.ENDED
                        else -> SLRVideoPlayer.State.IDLE
                    }
                )
            }

            override fun onPlayerError(error: PlaybackException) {
                listener.onPlayerError(error.cause)
            }
        }
        simpleExoPlayer.addListener(exoListener)
        listeners.add(Pair(listener, exoListener))
    }

    override fun removeListener(listener: SLRVideoPlayer.Listener) {
        listeners.find { it.first == listener }?.let {
            simpleExoPlayer.removeListener(it.second)
            listeners.remove(it)
        }
    }
}

class ExoVideoPlayerView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : StyledPlayerView(context, attrs, defStyleAttr), SLRVideoPlayerView {

    override fun getView(): View = this

    override fun setVideoPlayer(player: SLRVideoPlayer?) {
        if (player is ExoVideoPlayer?) this.player = player?.simpleExoPlayer
    }

    override fun setShowControls(showControls: Boolean) {
        useController = showControls
        controllerAutoShow = showControls
    }

    override fun setResizeMode(mode: SLRVideoPlayerView.ResizeMode) {
        resizeMode = when (mode) {
            SLRVideoPlayerView.ResizeMode.FIT -> AspectRatioFrameLayout.RESIZE_MODE_FIT
            SLRVideoPlayerView.ResizeMode.FIXED_WIDTH -> AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH
            SLRVideoPlayerView.ResizeMode.FIXED_HEIGHT -> AspectRatioFrameLayout.RESIZE_MODE_FIXED_HEIGHT
            SLRVideoPlayerView.ResizeMode.FILL -> AspectRatioFrameLayout.RESIZE_MODE_FILL
            SLRVideoPlayerView.ResizeMode.ZOOM -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
        }
    }
}

class ExoVideoPlayerProvider(private val context: Context) : SLRVideoPlayerProvider {

    private val bandwidthMeter by lazy { DefaultBandwidthMeter.Builder(context).build() }

    private val agent by lazy { Util.getUserAgent(context, "ExoVideoPlayerProvider") }

    private fun defaultDataSourceFactory(): DefaultDataSource.Factory =
        DefaultDataSource.Factory(context, DefaultHttpDataSource.Factory().setUserAgent(agent))

    override fun getVideoPlayer(
        url: String,
        type: SLRVideoPlayer.Type,
        mode: SLRVideoPlayer.RepeatMode
    ): SLRVideoPlayer {
        val trackSelector = DefaultTrackSelector(context)
        trackSelector.setParameters(trackSelector.buildUponParameters().setMaxVideoSizeSd())
        val player = ExoPlayer.Builder(context)
            .setBandwidthMeter(bandwidthMeter)
            .setTrackSelector(trackSelector)
            .setLoadControl(DefaultLoadControl())
            .build()
        val streamUri = MediaItem.Builder().setUri(url)
        val mediaSource = when (type) {
            SLRVideoPlayer.Type.HLS -> {
                streamUri.setMimeType(MimeTypes.APPLICATION_M3U8)
                HlsMediaSource.Factory(defaultDataSourceFactory())
                    .createMediaSource(streamUri.build())
            }
            else -> {
                streamUri.setMimeType(MimeTypes.APPLICATION_MP4)
                ProgressiveMediaSource.Factory(defaultDataSourceFactory())
                    .createMediaSource(streamUri.build())
            }
        }
        player.setMediaSource(mediaSource)
        player.repeatMode = when (mode) {
            SLRVideoPlayer.RepeatMode.OFF -> Player.REPEAT_MODE_OFF
            SLRVideoPlayer.RepeatMode.ONE -> Player.REPEAT_MODE_ONE
            SLRVideoPlayer.RepeatMode.ALL -> Player.REPEAT_MODE_ALL
        }
        player.prepare()
        return ExoVideoPlayer(player)
    }

    override fun getVideoPlayerView(
        context: Context,
        type: SLRVideoPlayerView.Type
    ): SLRVideoPlayerView = when (type) {
        SLRVideoPlayerView.Type.SURFACE -> LayoutInflater.from(context)
            .inflate(R.layout.surface_player_view, null) as ExoVideoPlayerView
        SLRVideoPlayerView.Type.TEXTURE -> LayoutInflater.from(context)
            .inflate(R.layout.texture_player_view, null) as ExoVideoPlayerView
    }
}
