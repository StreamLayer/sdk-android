package io.streamlayer.demo.main

import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.Observer
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.source.BaseMediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout
import io.streamlayer.demo.R
import io.streamlayer.demo.common.kotlin.visible
import io.streamlayer.demo.common.mvvm.BaseActivity
import io.streamlayer.demo.common.mvvm.ResourceState
import io.streamlayer.demo.common.mvvm.Status
import io.streamlayer.demo.common.network.NetworkConnectionLiveData
import io.streamlayer.demo.common.recyclerview.DashedDividerDecoration
import io.streamlayer.demo.utils.PlayerLifecycleObserver
import io.streamlayer.demo.utils.PlayerScreenEventListener
import io.streamlayer.demo.utils.setOnDoubleClickListener
import io.streamlayer.sdk.StreamLayer
import io.streamlayer.sdk.StreamLayerDemo
import kotlinx.android.synthetic.main.activity_main.*
import javax.inject.Inject


private const val TAG = "MainActivity"
private const val ARG_PLAYBACK_POSITION = "playback_position"

class MainActivity : BaseActivity() {

    private val streamsAdapter: StreamsAdapter by lazy { StreamsAdapter() }

    @Inject
    lateinit var exoPlayer: ExoPlayer

    @Inject
    lateinit var streamSourceFactory: HlsMediaSource.Factory

    @Inject
    lateinit var progressiveSourceFactory: ProgressiveMediaSource.Factory

    @Inject
    lateinit var viewModel: MainViewModel

    private val networkConnection: NetworkConnectionLiveData by lazy {
        NetworkConnectionLiveData(
            application
        )
    }

    private lateinit var playerLifecycleObserver: PlayerLifecycleObserver
    private var resumePlayback = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        playerView.useController = false
        exoPlayer.playWhenReady = true
        exoPlayer.repeatMode = ExoPlayer.REPEAT_MODE_ALL
        savedInstanceState?.getLong(ARG_PLAYBACK_POSITION)?.let {
            exoPlayer.seekTo(it)
            resumePlayback = true
        }
        playerView.player = exoPlayer.apply {
            addListener(object : Player.EventListener {
                override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
                    if (playWhenReady && playbackState == Player.STATE_READY) {
                        videoLoader.hide()
                    } else {
                        videoLoader.show()
                    }
                }
            })
        }
        playerView.setOnDoubleClickListener {
            if (playerView.resizeMode != AspectRatioFrameLayout.RESIZE_MODE_FIT) {
                playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
            } else if (playerView.resizeMode != AspectRatioFrameLayout.RESIZE_MODE_ZOOM) {
                playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
            }
        }

        recycler?.apply {
            addItemDecoration(DashedDividerDecoration(baseContext))
            adapter = streamsAdapter
            streamsAdapter.onItemSelected = viewModel::selectStream
        }
        shareButton?.setOnClickListener { share() }
        viewModel.demoStreams.observe(this, Observer(this::onDemoStreams))
        viewModel.selectedStream.observe(this, Observer(this::onStreamSelected))

        playerLifecycleObserver = PlayerLifecycleObserver(exoPlayer, playerView) {
            return@PlayerLifecycleObserver viewModel.selectedStream.value?.streamUrl?.let {
                getMediaSource(
                    it
                )
            }
        }
        lifecycle.addObserver(playerLifecycleObserver)
        exoPlayer.addListener(PlayerScreenEventListener(playerView))

        networkConnection.observe(this, Observer { isConnected ->
            if (isConnected) {
                resumePlaying()
            } else {
                pausePlaying()
            }
        })

        StreamLayer.setAudioDuckingListener(object : StreamLayer.AudioDuckingListener {
            private var initialVolume: Float? = null
            override fun requestAudioDucking() {
                exoPlayer.audioComponent?.let { audio ->
                    if (initialVolume == null) {
                        initialVolume = audio.volume
                        audio.volume = audio.volume * 0.2f
                    }
                }
            }

            override fun disableAudioDucking() {
                exoPlayer.audioComponent?.let { audio ->
                    initialVolume?.let { volume ->
                        initialVolume = null
                        audio.volume = volume
                    }
                }
            }
        })
    }

    private fun onDemoStreams(state: ResourceState<List<StreamLayerDemo.Item>>) {
        when (state.status) {
            Status.ERROR -> {
                dataLoader?.hide()
                Log.e(TAG, state.error?.errorMessage.orEmpty())
                Toast.makeText(baseContext, state.error?.errorMessage.orEmpty(), Toast.LENGTH_SHORT)
                    .show()
            }
            Status.LOADING -> {
                dataLoader?.show()
            }
            Status.SUCCESS -> {
                dataLoader?.hide()
                streamsAdapter.setItems(state.data.orEmpty())
            }
        }
    }

    private fun onStreamSelected(stream: StreamLayerDemo.Item) {
        StreamLayer.changeStreamEvent(stream.eventId.toString(), {})
        playingTitle?.text = stream.title
        shareButton?.visible()
        playingTitle?.visible()
        exoPlayer.prepare(getMediaSource(stream.streamUrl), !resumePlayback, !resumePlayback)
        resumePlayback = false
    }

    private fun getMediaSource(streamUrl: String): BaseMediaSource {
        val streamUri = Uri.parse(streamUrl)
        return when {
            streamUrl.endsWith(".m3u8") -> streamSourceFactory.createMediaSource(streamUri)
            else -> progressiveSourceFactory.createMediaSource(streamUri)
        }
    }

    private fun share() {
        val sendIntent: Intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(
                Intent.EXTRA_TEXT,
                getString(R.string.share_msg)
            )
            type = "text/plain"
        }

        val shareIntent = Intent.createChooser(sendIntent, null)
        playerLifecycleObserver.stopPlaybackOnPause = false
        startActivity(shareIntent)
    }

    private fun resumePlaying() {
        playerView?.onResume()
        exoPlayer.retry()
    }

    private fun pausePlaying() {
        exoPlayer.stop()
        playerView?.onPause()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
            playerView.layoutParams.apply {
                width = MATCH_PARENT
                height = baseContext.resources.getDimensionPixelSize(R.dimen.player_height_portrait)
                playerView.layoutParams = this
            }
            (streamLayerFragment.layoutParams as ConstraintLayout.LayoutParams).topToBottom =
                R.id.playerView
            (streamLayerFragment.layoutParams as ConstraintLayout.LayoutParams).topToTop =
                ConstraintLayout.LayoutParams.UNSET
        } else {
            window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION)
            playerView.layoutParams.apply {
                width = MATCH_PARENT
                height = MATCH_PARENT
                playerView.layoutParams = this
            }
            (streamLayerFragment.layoutParams as ConstraintLayout.LayoutParams).topToBottom =
                ConstraintLayout.LayoutParams.UNSET
            (streamLayerFragment.layoutParams as ConstraintLayout.LayoutParams).topToTop =
                ConstraintLayout.LayoutParams.PARENT_ID
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putLong(ARG_PLAYBACK_POSITION, exoPlayer.currentPosition)
    }

    /**
     * Add a deep link handler which will listen if the activity is started from StreamLayer notification
     * and will automatically open the deep link destination in one of the StreamLayer overlays.
     *
     * Returns true if the intent was handled by StreamLayer SDK, otherwise false.
     */
    override fun onResume() {
        super.onResume()
        if (!StreamLayer.handleDeepLinkIntent(intent, this)) {
            // do host logic if needed
        }
    }

    /**
     * If your activity launch mode is set to [Intent.FLAG_ACTIVITY_SINGLE_TOP], listen for [onNewIntent] as well
     * because the [onResume] will not be triggered if the activity is already running.
     */
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        if (intent == null) return

        if (!StreamLayer.handleDeepLinkIntent(intent, this)) {
            // do host logic if needed
        }
    }
}
