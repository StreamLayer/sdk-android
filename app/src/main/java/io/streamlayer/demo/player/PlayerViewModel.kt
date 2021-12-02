package io.streamlayer.demo.player

import android.content.Context
import android.net.Uri
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import androidx.lifecycle.viewModelScope
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.DefaultLoadControl
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.PlaybackException
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.Timeline
import com.google.android.exoplayer2.ext.cast.CastPlayer
import com.google.android.exoplayer2.source.BaseMediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.upstream.cache.CacheDataSource
import com.google.android.exoplayer2.upstream.cache.SimpleCache
import com.google.android.exoplayer2.util.MimeTypes
import com.google.android.exoplayer2.util.Util
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.CastSession
import com.google.android.gms.cast.framework.SessionManagerListener
import io.streamlayer.demo.R
import io.streamlayer.demo.common.dispatcher.CoroutineDispatcherProvider
import io.streamlayer.demo.common.mvvm.BaseError
import io.streamlayer.demo.common.mvvm.BaseErrorEvent
import io.streamlayer.demo.common.mvvm.MviViewModel
import io.streamlayer.demo.common.network.NetworkConnectionUseCase
import io.streamlayer.demo.repository.Stream
import io.streamlayer.demo.repository.StreamsRepository
import io.streamlayer.sdk.EventSession
import io.streamlayer.sdk.StreamLayer
import io.streamlayer.sdk.TimeCodeProvider
import io.streamlayer.sdk.VideoPlayer
import io.streamlayer.sdk.VideoPlayerProvider
import io.streamlayer.sdk.VideoPlayerView
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import kotlin.math.min

data class State(
    val streams: List<Stream> = emptyList(),
    val selectedStream: Stream? = null,
    val hasNetworkConnection: Boolean = true,
    val isCastSupported: Boolean = false, // show if cast is supported
    val isCastActive: Boolean = false, // show if cast is active
)

class PlayerViewModel(
    private val streamsRepository: StreamsRepository,
    private val cache: SimpleCache,
    private val networkConnectionUseCase: NetworkConnectionUseCase,
    private val context: Context,
    coroutineDispatcherProvider: CoroutineDispatcherProvider
) : MviViewModel<State>(State(), coroutineDispatcherProvider) {

    val streams = stateSlice { streams }
    val selectedStream = stateSlice { selectedStream }.filterNotNull()
    val castState = stateSlice { Pair(isCastSupported, isCastActive) }
    val hasNetworkConnection = stateSlice { hasNetworkConnection }

    private var requestedEventId: String? = null // requested event id from sdk

    private var eventSession: EventSession? = null

    var isPlaybackPaused = false // check if player was stopped by user
    var isControlsVisible = false // check if player controls are visible
    private var volumeBeforeDucking: Float? = null

    private var castContext: CastContext? = null  // chromecast session

    private val bandwidthMeter by lazy { DefaultBandwidthMeter.Builder(context).build() }

    private val agent by lazy { Util.getUserAgent(context, "streamlayer") }

    private fun defaultDataSourceFactory(): DefaultDataSourceFactory =
        DefaultDataSourceFactory(context, agent, bandwidthMeter)

    // internal default instance of ExoPlayer
    private val exoPlayer: ExoPlayer by lazy { initPlayer() }

    // chrome cast can be not available - keep it as separated nullable instance
    private var castPlayer: CastPlayer? = null

    var player: Player = exoPlayer
        private set

    private val castSessionListener by lazy { initCastSessionListener() }

    init {
        initCast()
        subToStreams()
        subToNetworkChanges()
    }

    private fun initCast() {
        try {
            castContext = CastContext.getSharedInstance(context)
            castContext?.let {
                updateState(immediate = true) { copy(isCastSupported = true) }
                it.sessionManager.addSessionManagerListener(castSessionListener, CastSession::class.java)
            }
        } catch (e: RuntimeException) {
            // cast is not supported
            _viewEvents.trySend(BaseErrorEvent(BaseError("Chromecast is not available.")))
        }
    }

    private fun releaseCast() {
        castContext?.let {
            castPlayer?.release()
            it.sessionManager.removeSessionManagerListener(castSessionListener, CastSession::class.java)
        }
    }

    private fun subToNetworkChanges() {
        viewModelScope.launch {
            networkConnectionUseCase.state.collect {
                if (it) streamsRepository.refresh()
                updateState(immediate = true) { copy(hasNetworkConnection = it) }
            }
        }
    }

    private fun subToStreams() {
        viewModelScope.launch {
            streamsRepository.getStreams().collect {
                it.consume { list ->
                    updateState { copy(streams = list) }
                    // in case when stream was requested before list was loaded
                    if (currentState.selectedStream == null) selectStream(requestedEventId?.let {
                        list.firstOrNull { it.eventId.toString() == requestedEventId } ?: list.firstOrNull()
                    } ?: list.firstOrNull())
                }
            }
        }
    }

    fun selectStream(stream: Stream?) {
        if (stream == null) return
        requestedEventId = null
        updateState { copy(selectedStream = stream) }
        attachStream(stream, 0)
        eventSession?.release()
        eventSession = StreamLayer.createEventSession(
            stream.eventId.toString(),
            if (streamsRepository.isPDTStream(stream.streamUrl)) getTimeCodeProvider() else null
        )
        isPlaybackPaused = false
    }

    private fun attachStream(stream: Stream, startPositionMs: Long) {
        // cast and exo players has different api - exo player should be setup with media source,
        // otherwise cache is not working properly and live streams can be broken
        if (player == exoPlayer) {
            val mediaSource = buildMediaSource(stream.streamUrl)
            if (startPositionMs == C.TIME_UNSET) exoPlayer.setMediaSource(mediaSource, true)
            else exoPlayer.setMediaSource(mediaSource, startPositionMs)
        } else player.setMediaItem(mediaItemBuilder(stream.streamUrl).build(), startPositionMs)
        player.playWhenReady = true
        if (player.playbackState == Player.STATE_IDLE) player.prepare()
    }

    fun requestStreamEvent(eventId: String) {
        with(currentState) {
            if (streams.isNotEmpty()) streams.firstOrNull { it.eventId.toString() == eventId }
                ?.let { if (selectedStream != it) selectStream(it) }
            else requestedEventId = eventId
        }
    }

    override fun onCleared() {
        exoPlayer.release()
        eventSession?.release()
        releaseCast()
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

    private fun initCastPlayer() {
        if (castPlayer == null) castContext?.let {
            castPlayer = CastPlayer(it).apply {
                //don't use setSessionAvailabilityListener - it required to create instance of player
                repeatMode = Player.REPEAT_MODE_ALL
            }
        }
    }

    private fun buildMediaSource(streamUrl: String): BaseMediaSource {
        val builder = mediaItemBuilder(streamUrl)
        return when {
            isHlsStream(streamUrl) -> {
                HlsMediaSource.Factory(defaultDataSourceFactory()).createMediaSource(builder.build())
            }
            else -> {
                ProgressiveMediaSource.Factory(
                    CacheDataSource.Factory().setCache(cache)
                        .setUpstreamDataSourceFactory(defaultDataSourceFactory())
                        .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
                ).createMediaSource(builder.build())
            }
        }
    }

    private fun mediaItemBuilder(streamUrl: String) = MediaItem.Builder().setUri(Uri.parse(streamUrl)).apply {
        when {
            isHlsStream(streamUrl) -> setMimeType(MimeTypes.APPLICATION_M3U8)
            else -> setMimeType(MimeTypes.APPLICATION_MP4)
        }
    }

    private fun isHlsStream(streamUrl: String): Boolean = streamUrl.endsWith(".m3u8")

    // timecode provider
    private fun getTimeCodeProvider(): TimeCodeProvider = object : TimeCodeProvider {
        override fun getEpochTimeCodeInMillis(): Long {
            val timeline = player.currentTimeline
            return if (!timeline.isEmpty) timeline.getWindow(
                player.currentWindowIndex, Timeline.Window()
            ).windowStartTimeMs + player.currentPosition
            else player.currentPosition
        }
    }

    private fun initCastSessionListener() = object : SessionManagerListener<CastSession> {
        override fun onSessionStarting(p0: CastSession) {}

        override fun onSessionStarted(p0: CastSession, p1: String) {
            initCastPlayer()
            castPlayer?.let { setCurrentPlayer(it) }
        }

        override fun onSessionStartFailed(p0: CastSession, p1: Int) {}

        override fun onSessionEnding(p0: CastSession) {}

        override fun onSessionEnded(p0: CastSession, p1: Int) {
            setCurrentPlayer(exoPlayer)
        }

        override fun onSessionResuming(p0: CastSession, p1: String) {}

        override fun onSessionResumed(p0: CastSession, p1: Boolean) {
            initCastPlayer()
            castPlayer?.let { setCurrentPlayer(it) }
        }

        override fun onSessionResumeFailed(p0: CastSession, p1: Int) {}

        override fun onSessionSuspended(p0: CastSession, p1: Int) {
            setCurrentPlayer(exoPlayer)
            // optional: remove connection in case when session were suspended
            castContext?.sessionManager?.let { if (it.currentCastSession == p0) it.endCurrentSession(true) }
        }
    }

    private fun setCurrentPlayer(newPlayer: Player) {
        if (player == newPlayer) return
        val playbackPositionMs =
            if (player.playbackState != Player.STATE_ENDED) player.currentPosition else C.TIME_UNSET
        player.stop()
        player.clearMediaItems()
        player = newPlayer
        currentState.selectedStream?.let { attachStream(it, playbackPositionMs) }
        updateState { copy(isCastActive = newPlayer == castPlayer) }
    }

    fun notifyDuckingChanged(isEnabled: Boolean) {
        // support ducking only for in-app player, ignore if chrome cast is active
        if (player is ExoPlayer) (player as ExoPlayer).audioComponent?.also { audio ->
            if (isEnabled) {
                if (volumeBeforeDucking == null) {
                    // decrease volume to 10% if louder, otherwise keep the current volume
                    volumeBeforeDucking = audio.volume
                    audio.volume = min(audio.volume, 0.1f)
                }
            } else volumeBeforeDucking?.let { volume ->
                audio.volume = volume
                volumeBeforeDucking = null
            }
        }
    }
}

// keep it as internal function - cast apis
internal class ExoVideoPlayer(internal val simpleExoPlayer: SimpleExoPlayer) : VideoPlayer {

    // keep it for mapping different listeners
    private val listeners: MutableList<Pair<VideoPlayer.Listener, Player.Listener>> = mutableListOf()

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

    override fun addListener(listener: VideoPlayer.Listener) {
        val exoListener = object : Player.Listener {
            override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
                listener.onPlayerStateChanged(
                    when (playbackState) {
                        Player.STATE_BUFFERING -> VideoPlayer.State.BUFFERING
                        Player.STATE_READY -> VideoPlayer.State.READY
                        Player.STATE_ENDED -> VideoPlayer.State.ENDED
                        else -> VideoPlayer.State.IDLE
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

    override fun removeListener(listener: VideoPlayer.Listener) {
        listeners.find { it.first == listener }?.let {
            simpleExoPlayer.removeListener(it.second)
            listeners.remove(it)
        }
    }
}

internal class ExoVideoPlayerView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : PlayerView(context, attrs, defStyleAttr), VideoPlayerView {

    override fun getView(): View = this

    override fun setVideoPlayer(player: VideoPlayer?) {
        if (player is ExoVideoPlayer?) this.player = player?.simpleExoPlayer
    }

    override fun setShowControls(showControls: Boolean) {
        useController = showControls
        controllerAutoShow = showControls
    }

    override fun setResizeMode(mode: VideoPlayerView.ResizeMode) {
        resizeMode = when (mode) {
            VideoPlayerView.ResizeMode.FIT -> AspectRatioFrameLayout.RESIZE_MODE_FIT
            VideoPlayerView.ResizeMode.FIXED_WIDTH -> AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH
            VideoPlayerView.ResizeMode.FIXED_HEIGHT -> AspectRatioFrameLayout.RESIZE_MODE_FIXED_HEIGHT
            VideoPlayerView.ResizeMode.FILL -> AspectRatioFrameLayout.RESIZE_MODE_FILL
            VideoPlayerView.ResizeMode.ZOOM -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
        }
    }
}

internal class ExoVideoPlayerProvider(private val context: Context) : VideoPlayerProvider {

    private val bandwidthMeter by lazy { DefaultBandwidthMeter.Builder(context).build() }

    private val agent by lazy { Util.getUserAgent(context, "streamlayer") }

    private fun defaultDataSourceFactory(): DefaultDataSourceFactory =
        DefaultDataSourceFactory(context, agent, bandwidthMeter)

    override fun getVideoPlayer(url: String, type: VideoPlayer.Type, mode: VideoPlayer.RepeatMode): VideoPlayer {
        val player = SimpleExoPlayer.Builder(context).build()
        val streamUri = MediaItem.Builder().setUri(url)
        val mediaSource = when (type) {
            VideoPlayer.Type.HLS -> {
                streamUri.setMimeType(MimeTypes.APPLICATION_M3U8)
                HlsMediaSource.Factory(defaultDataSourceFactory()).createMediaSource(streamUri.build())
            }
            else -> {
                streamUri.setMimeType(MimeTypes.APPLICATION_MP4)
                ProgressiveMediaSource.Factory(defaultDataSourceFactory()).createMediaSource(streamUri.build())
            }
        }
        player.setMediaSource(mediaSource)
        player.repeatMode = when (mode) {
            VideoPlayer.RepeatMode.OFF -> Player.REPEAT_MODE_OFF
            VideoPlayer.RepeatMode.ONE -> Player.REPEAT_MODE_ONE
            VideoPlayer.RepeatMode.ALL -> Player.REPEAT_MODE_ALL
        }
        player.prepare()
        return ExoVideoPlayer(player)
    }

    override fun getVideoPlayerView(context: Context, type: VideoPlayerView.Type): VideoPlayerView = when (type) {
        VideoPlayerView.Type.SURFACE -> LayoutInflater.from(context)
            .inflate(R.layout.surface_player_view, null) as ExoVideoPlayerView
        VideoPlayerView.Type.TEXTURE -> LayoutInflater.from(context)
            .inflate(R.layout.texture_player_view, null) as ExoVideoPlayerView
    }
}
