package io.streamlayer.demo.player

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.viewModels
import androidx.core.view.doOnPreDraw
import androidx.lifecycle.Observer
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout
import io.streamlayer.demo.R
import io.streamlayer.demo.common.mvvm.BaseActivity
import io.streamlayer.demo.common.mvvm.Status
import io.streamlayer.demo.common.recyclerview.DashedDividerDecoration
import io.streamlayer.demo.utils.DoubleClickListener
import io.streamlayer.demo.utils.gone
import io.streamlayer.demo.utils.isScreenPortrait
import io.streamlayer.demo.utils.visible
import io.streamlayer.sdk.StreamLayer
import io.streamlayer.sdk.StreamLayerUI
import kotlinx.android.synthetic.main.activity_player.*
import kotlin.math.min

private const val CONTROLS_AUTO_HIDE_DELAY = 5000L

class PlayerActivity : BaseActivity() {

    private val streamsAdapter: StreamsAdapter by lazy { StreamsAdapter() }

    private val viewModel: PlayerViewModel by viewModels { viewModelFactory }

    private val controlsHandler = Handler()

    private val playerListener = object : Player.EventListener {

        override fun onPlayerError(error: ExoPlaybackException) {
            val exceptionMessage = when (error.type) {
                ExoPlaybackException.TYPE_SOURCE -> error.sourceException.localizedMessage
                ExoPlaybackException.TYPE_RENDERER -> error.rendererException.localizedMessage
                ExoPlaybackException.TYPE_UNEXPECTED -> error.unexpectedException.localizedMessage
                ExoPlaybackException.TYPE_OUT_OF_MEMORY -> error.outOfMemoryError.localizedMessage
                else -> error.localizedMessage
            }
            Toast.makeText(
                playerView.context,
                "Exo error: type=" + error.type + " message=" + exceptionMessage,
                Toast.LENGTH_LONG
            ).show()
        }

        override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
            playerView.keepScreenOn =
                !(playbackState == Player.STATE_IDLE || playbackState == Player.STATE_ENDED || !playWhenReady)
            if (playWhenReady && playbackState == Player.STATE_READY || viewModel.isPlaybackPaused) videoLoader.hide()
            else videoLoader.show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player)

        setupUI()
        bind()
        window.addFlags(
            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                    or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        if (viewModel.isControlsVisible) showControls()
        setPlaybackIcon()
    }

    private fun setupUI() {
        viewModel.exoPlayer.addListener(playerListener)
        if (viewModel.isPlaybackPaused) videoLoader.hide()
        playerView.player = viewModel.exoPlayer
        playerView.videoSurfaceView?.setOnClickListener(DoubleClickListener(
            {
                // single tap
                if (viewModel.isControlsVisible) hideControls() else showControls()
            },
            {
                // double tap
                if (playerView.resizeMode != AspectRatioFrameLayout.RESIZE_MODE_FIT) {
                    playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                } else if (playerView.resizeMode != AspectRatioFrameLayout.RESIZE_MODE_ZOOM) {
                    playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                }
            }
        ))
        if (isScreenPortrait(this)) playerView.doOnPreDraw {
            StreamLayerUI.setOverlayViewHeight(this, container.height - it.height)
        }
        close_button.setOnClickListener { finish() }
        playback_button.setOnClickListener {
            viewModel.isPlaybackPaused = !viewModel.isPlaybackPaused
            viewModel.exoPlayer.playWhenReady = !viewModel.isPlaybackPaused
            setPlaybackIcon()
            showControls()
        }

        recycler?.apply {
            addItemDecoration(DashedDividerDecoration(baseContext))
            adapter = streamsAdapter
            streamsAdapter.onItemSelected = viewModel::selectStream
        }
        shareButton?.setOnClickListener { share() }

        StreamLayer.setAudioDuckingListener(object : StreamLayer.AudioDuckingListener {
            private var initialVolume: Float? = null
            override fun requestAudioDucking() {
                viewModel.exoPlayer.audioComponent?.let { audio ->
                    // decrease volume to 10% if louder, otherwise keep the current volume
                    if (initialVolume == null) {
                        initialVolume = audio.volume
                        audio.volume = min(audio.volume, 0.1f)
                    }
                }
            }

            override fun disableAudioDucking() {
                viewModel.exoPlayer.audioComponent?.let { audio ->
                    initialVolume?.let { volume ->
                        audio.volume = volume
                        initialVolume = null
                    }
                }
            }
        })

        StreamLayer.setStreamEventChangeListener(object : StreamLayer.StreamEventChangeListener {
            override fun onStreamChanged(id: String) {
                viewModel.requestStreamEvent(id)
            }
        })
    }

    private fun bind() {
        viewModel.demoStreams.observe(this, Observer {
            if (it.status == Status.LOADING) dataLoader?.show() else dataLoader?.hide()
            it.data?.let { streamsAdapter.setItems(it) }
            it.error?.let { Toast.makeText(this, it.errorMessage, Toast.LENGTH_SHORT).show() }
        })
        viewModel.selectedStream.observe(this, Observer {
            StreamLayer.changeStreamEvent(it.eventId.toString())
            playingTitle?.text = it.title
            shareButton?.visible()
            playingTitle?.visible()
        })
        viewModel.networkConnectionLiveData.observe(this, Observer {
            if (it) {
                resumePlaying()
                viewModel.refresh()
            } else pausePlaying()
        })
    }

    private fun share() {
        val sendIntent: Intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, getString(R.string.share_msg))
            type = "text/plain"
        }

        val shareIntent = Intent.createChooser(sendIntent, null)
        startActivity(shareIntent)
    }

    private fun showControls() {
        controlsHandler.removeCallbacksAndMessages(null)
        playback_button?.visible()
        close_button?.visible()
        if (!isScreenPortrait(this)) StreamLayerUI.hideLaunchButton(this)
        viewModel.isControlsVisible = true
        controlsHandler.postDelayed({ hideControls() }, CONTROLS_AUTO_HIDE_DELAY)
    }

    private fun hideControls() {
        controlsHandler.removeCallbacksAndMessages(null)
        playback_button?.gone()
        close_button?.gone()
        if (!isScreenPortrait(this)) StreamLayerUI.showLaunchButton(this)
        viewModel.isControlsVisible = false
    }

    private fun setPlaybackIcon() {
        playback_button.setImageResource(
            if (viewModel.isPlaybackPaused) R.drawable.sl_play_ic
            else R.drawable.sl_pause_ic
        )
    }

    private fun resumePlaying() {
        if (!viewModel.exoPlayer.isPlaying && !viewModel.isPlaybackPaused) viewModel.exoPlayer.playWhenReady = true
    }

    private fun pausePlaying() {
        if (viewModel.exoPlayer.isPlaying) viewModel.exoPlayer.playWhenReady = false
    }

    /**
     * Add a deep link handler which will listen if the activity is started from StreamLayer notification
     * and will automatically open the deep link destination in one of the StreamLayer overlays.
     *
     * Returns true if the intent was handled by StreamLayer SDK, otherwise false.
     */
    override fun onResume() {
        super.onResume()
        if (!StreamLayer.handleDeepLink(intent, this)) {
            // do host logic if needed
        }
    }

    override fun onStart() {
        super.onStart()
        resumePlaying()
    }

    override fun onStop() {
        super.onStop()
        pausePlaying()
    }

    /**
     * If your activity launch mode is set to [Intent.FLAG_ACTIVITY_SINGLE_TOP], listen for [onNewIntent] as well
     * because the [onResume] will not be triggered if the activity is already running.
     */
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        if (intent == null) return
        if (!StreamLayer.handleDeepLink(intent, this)) {
            // do host logic if needed
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        controlsHandler.removeCallbacksAndMessages(null)
        viewModel.exoPlayer.removeListener(playerListener)
        StreamLayer.setAudioDuckingListener(null)
        StreamLayer.setStreamEventChangeListener(null)
    }
}
