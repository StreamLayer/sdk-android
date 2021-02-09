package io.streamlayer.demo.player

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.distinctUntilChanged
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
import com.google.android.exoplayer2.upstream.cache.CacheDataSource
import com.google.android.exoplayer2.upstream.cache.SimpleCache
import com.google.android.exoplayer2.util.MimeTypes
import com.google.android.exoplayer2.util.Util
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.disposables.Disposables
import io.reactivex.schedulers.Schedulers
import io.streamlayer.demo.common.mvvm.BaseError
import io.streamlayer.demo.common.mvvm.ResourceState
import io.streamlayer.demo.common.mvvm.Status
import io.streamlayer.demo.common.network.NetworkConnectionLiveData
import io.streamlayer.demo.repository.DemoStreamsRepository
import io.streamlayer.demo.utils.getStreamUrl
import io.streamlayer.sdk.StreamLayerDemo
import javax.inject.Inject

class PlayerViewModel @Inject constructor(
    private val demoStreamsRepository: DemoStreamsRepository,
    private val cache: SimpleCache,
    private val context: Context
) : ViewModel() {

    private var streamsDisposable: Disposable = Disposables.disposed()

    private val _demoStreams: MutableLiveData<ResourceState<List<StreamLayerDemo.Item>>> = MutableLiveData()
    val demoStreams: LiveData<ResourceState<List<StreamLayerDemo.Item>>> = _demoStreams

    private var _selectedStream: MutableLiveData<StreamLayerDemo.Item> = MutableLiveData()
    val selectedStream: LiveData<StreamLayerDemo.Item> = _selectedStream

    @SuppressLint("MissingPermission")
    private val _networkConnectionLiveData = NetworkConnectionLiveData(context)
    val networkConnectionLiveData: LiveData<Boolean> = _networkConnectionLiveData.distinctUntilChanged()

    private var requestedStreamId: String? = null

    init {
        refresh()
    }

    fun refresh() {
        if (_demoStreams.value?.status == Status.LOADING || _demoStreams.value?.status == Status.SUCCESS) return
        streamsDisposable.dispose()
        streamsDisposable = demoStreamsRepository.getDemoStreams()
            .flatMapObservable {
                Observable.fromIterable(it)
            }
            .concatMapEager { item ->
                getStreamUrl(item.streamId).doOnError { Observable.just(item) }
                    .map { item.copy(streamUrl = it) }.toObservable()
            }
            .filter { it.streamUrl.isNotEmpty() }
            .toList()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSubscribe {
                _demoStreams.postValue(ResourceState.loading())
            }
            .doOnSuccess { list ->
                if (_selectedStream.value == null) {
                    selectStream(requestedStreamId?.let {
                        list.first { it!!.eventId.toString() == requestedStreamId } ?: list.first()
                    } ?: list.first())
                }
                _demoStreams.postValue(ResourceState.success(list))
            }
            .subscribe({
                _demoStreams.postValue(ResourceState.success(it))
            }, {
                _demoStreams.postValue(ResourceState.error(it.message?.let { BaseError(it) } ?: BaseError()))
            })
    }

    fun selectStream(stream: StreamLayerDemo.Item) {
        requestedStreamId = null
        _selectedStream.postValue(stream)
        exoPlayer.setMediaSource(buildMediaSource(stream.streamUrl), true)
        if (exoPlayer.playbackState == Player.STATE_IDLE) exoPlayer.prepare()
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
        streamsDisposable.dispose()
        super.onCleared()
    }

    // exo player helpers

    // check if player was stopped by user - don't need live data here - just simple cache instance
    var isPlaybackPaused = false

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