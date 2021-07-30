package io.streamlayer.demo.player

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.viewModelScope
import com.google.android.exoplayer2.DefaultLoadControl
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
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
import io.streamlayer.demo.R
import io.streamlayer.demo.common.mvvm.ResourceState
import io.streamlayer.demo.common.mvvm.Status
import io.streamlayer.demo.common.network.NetworkConnectionLiveData
import io.streamlayer.demo.repository.DemoStreamsRepository
import io.streamlayer.sdk.EventSession
import io.streamlayer.sdk.StreamLayer
import io.streamlayer.sdk.VideoPlayer
import io.streamlayer.sdk.VideoPlayerProvider
import io.streamlayer.sdk.VideoPlayerView
import io.streamlayer.sdk.base.StreamLayerDemo
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

class PlayerViewModel @Inject constructor(
    private val demoStreamsRepository: DemoStreamsRepository,
    private val cache: SimpleCache,
    private val context: Context
) : ViewModel() {

    private var job: Job? = null

    private val _demoStreams: MutableLiveData<ResourceState<List<StreamLayerDemo.Stream>>> = MutableLiveData()
    val demoStreams: LiveData<ResourceState<List<StreamLayerDemo.Stream>>> = _demoStreams

    private var _selectedStream: MutableLiveData<StreamLayerDemo.Stream> = MutableLiveData()
    val selectedStream: LiveData<StreamLayerDemo.Stream> = _selectedStream

    @SuppressLint("MissingPermission")
    val networkConnectionLiveData: LiveData<Boolean> = NetworkConnectionLiveData(context).distinctUntilChanged()

    private var requestedStreamId: String? = null

    private var eventSession: EventSession? = null

    init {
        refresh()
    }

    fun refresh() {
        if (_demoStreams.value?.status == Status.LOADING || _demoStreams.value?.status == Status.SUCCESS) return
        job?.cancel()
        job = viewModelScope.launch {
            _demoStreams.postValue(ResourceState.loading())
            val result = demoStreamsRepository.getDemoStreams()
            result.data?.let { list ->
                if (_selectedStream.value == null) {
                    selectStream(requestedStreamId?.let {
                        list.firstOrNull { it.eventId.toString() == requestedStreamId } ?: list.firstOrNull()
                    } ?: list.firstOrNull())
                }
                _demoStreams.postValue(ResourceState.success(list))
            } ?: kotlin.run { result.error?.let { _demoStreams.postValue(result) } }
        }
    }

    fun selectStream(stream: StreamLayerDemo.Stream?) {
        if (stream == null) return
        requestedStreamId = null
        _selectedStream.postValue(stream)
        exoPlayer.setMediaSource(buildMediaSource(stream.streamUrl), true)
        if (exoPlayer.playbackState == Player.STATE_IDLE) exoPlayer.prepare()
        eventSession?.release()
        eventSession = StreamLayer.createEventSession(stream.eventId.toString(), null)
        isPlaybackPaused = false
        exoPlayer.playWhenReady = true
    }

    fun requestStreamEvent(eventId: String) {
        if (_demoStreams.value?.status == Status.SUCCESS && _demoStreams.value?.data != null)
            _demoStreams.value?.data?.first { it.eventId.toString() == eventId }?.let {
                if (_selectedStream.value != it) selectStream(it)
            }
        else requestedStreamId = eventId
    }

    override fun onCleared() {
        exoPlayer.release()
        eventSession?.release()
        super.onCleared()
    }

    // exo player helpers
    var isPlaybackPaused = false // check if player was stopped by user
    var isControlsVisible = false // check if player controls are visible
    var volumeBeforeDucking: Float? = null

    private val bandwidthMeter by lazy { DefaultBandwidthMeter.Builder(context).build() }

    private val agent by lazy { Util.getUserAgent(context, "streamlayer") }

    private fun defaultDataSourceFactory(): DefaultDataSourceFactory =
        DefaultDataSourceFactory(context, agent, bandwidthMeter)

    val exoPlayer: ExoPlayer by lazy { initPlayer() }

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
        val streamUri = MediaItem.Builder().setUri(Uri.parse(streamUrl))
        return when {
            streamUrl.endsWith(".m3u8") -> {
                streamUri.setMimeType(MimeTypes.APPLICATION_M3U8)
                HlsMediaSource.Factory(defaultDataSourceFactory()).createMediaSource(streamUri.build())
            }
            else -> {
                streamUri.setMimeType(MimeTypes.APPLICATION_MP4)
                ProgressiveMediaSource.Factory(
                    CacheDataSource.Factory().setCache(cache)
                        .setUpstreamDataSourceFactory(defaultDataSourceFactory())
                        .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
                ).createMediaSource(streamUri.build())
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
        VideoPlayerView.Type.SURFACE -> ExoVideoPlayerView(context)
        VideoPlayerView.Type.TEXTURE -> LayoutInflater.from(context)
            .inflate(R.layout.texture_player_view, null) as ExoVideoPlayerView
    }
}
