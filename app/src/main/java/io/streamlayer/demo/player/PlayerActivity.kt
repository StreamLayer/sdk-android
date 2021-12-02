package io.streamlayer.demo.player

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.doOnPreDraw
import com.google.android.exoplayer2.PlaybackException
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout
import com.google.android.gms.cast.framework.CastButtonFactory
import io.streamlayer.demo.R
import io.streamlayer.demo.common.koin.injectViewModel
import io.streamlayer.demo.common.kotlin.fullScreen
import io.streamlayer.demo.common.kotlin.gone
import io.streamlayer.demo.common.kotlin.setInputKeyboardEventListener
import io.streamlayer.demo.common.kotlin.visible
import io.streamlayer.demo.common.kotlin.visibleIf
import io.streamlayer.demo.common.kotlin.windowController
import io.streamlayer.demo.common.mvvm.BaseErrorEvent
import io.streamlayer.demo.common.mvvm.collectWhenResumed
import io.streamlayer.demo.common.mvvm.collectWhenStarted
import io.streamlayer.demo.databinding.ActivityPlayerBinding
import io.streamlayer.demo.utils.DoubleTapListener
import io.streamlayer.demo.utils.isScreenPortrait
import io.streamlayer.sdk.StreamLayer
import io.streamlayer.sdk.StreamLayerUI

private const val CONTROLS_AUTO_HIDE_DELAY = 5000L

class PlayerActivity : AppCompatActivity() {

    companion object {

        private const val EXTRA_EVENT_ID = "EXTRA_EVENT_ID"

        fun open(context: Context, eventId: String? = null) {
            context.startActivity(Intent(context, PlayerActivity::class.java).apply {
                eventId?.let { putExtra(EXTRA_EVENT_ID, it) }
            })
        }
    }

    private val streamsAdapter: StreamsAdapter by lazy { StreamsAdapter() }

    private val viewModel: PlayerViewModel by injectViewModel()

    private lateinit var binding: ActivityPlayerBinding

    private val controlsHandler = Handler()

    private val playerListener = object : Player.Listener {

        override fun onPlayerError(error: PlaybackException) {
            Toast.makeText(
                this@PlayerActivity,
                "Exo error: core=" + error.errorCode + " message=" + error.localizedMessage,
                Toast.LENGTH_LONG
            ).show()
        }

        override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
            with(binding) {
                playerView.keepScreenOn =
                    !(playbackState == Player.STATE_IDLE || playbackState == Player.STATE_ENDED || !playWhenReady)
                if (playWhenReady && playbackState == Player.STATE_READY || viewModel.isPlaybackPaused) videoLoader.hide()
                else videoLoader.show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        intent.getStringExtra(EXTRA_EVENT_ID)?.let {
            viewModel.requestStreamEvent(it)
            intent.removeExtra(EXTRA_EVENT_ID)
        }
        setupUI()
        bind()
        window.addFlags(
            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )
        if (!isScreenPortrait()) {
            val controller = windowController
            window.fullScreen(controller)
            setInputKeyboardEventListener { if (!it) window.fullScreen(controller) }
        }
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        if (viewModel.isControlsVisible) showControls()
        setPlaybackIcon()
    }

    private fun setupUI() {
        with(binding) {
            CastButtonFactory.setUpMediaRouteButton(this@PlayerActivity, binding.castButton)
            viewModel.player.addListener(playerListener)
            if (viewModel.isPlaybackPaused) videoLoader.hide()
            playerView.player = viewModel.player
            playerView.videoSurfaceView?.setOnTouchListener(object : DoubleTapListener() {
                override fun onDelayedTap(x: Float, y: Float) {
                    if (viewModel.isControlsVisible) hideControls() else showControls()
                }

                override fun onDoubleTap(x: Float, y: Float) {
                    if (playerView.resizeMode != AspectRatioFrameLayout.RESIZE_MODE_FIT) {
                        playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    } else if (playerView.resizeMode != AspectRatioFrameLayout.RESIZE_MODE_ZOOM) {
                        playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    }
                }
            })
            if (isScreenPortrait()) playerView.doOnPreDraw {
                StreamLayerUI.setOverlayViewHeight(this@PlayerActivity, container.height - it.height)
            }
            closeButton.setOnClickListener { finish() }
            playbackButton.setOnClickListener {
                viewModel.isPlaybackPaused = !viewModel.isPlaybackPaused
                viewModel.player.playWhenReady = !viewModel.isPlaybackPaused
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

                override fun requestAudioDucking() {
                    viewModel.notifyDuckingChanged(true)
                }

                override fun disableAudioDucking() {
                    viewModel.notifyDuckingChanged(false)
                }
            })

            StreamLayer.setStreamEventChangeListener(object : StreamLayer.StreamEventChangeListener {
                override fun onStreamChanged(id: String) {
                    viewModel.requestStreamEvent(id)
                }
            })
        }
    }

    private fun bind() {
        viewModel.streams.collectWhenStarted(this) {
            with(binding) {
                if (it.isEmpty()) dataLoader?.show() else dataLoader?.hide()
                streamsAdapter.setItems(it)
            }
        }
        viewModel.selectedStream.collectWhenStarted(this) {
            with(binding) {
                playingTitle?.text = it.title
                shareButton?.visible()
                playingTitle?.visible()
            }
        }
        viewModel.hasNetworkConnection.collectWhenStarted(this) {
            if (it) resumePlaying() else pausePlaying()
        }
        viewModel.castState.collectWhenStarted(this) {
            binding.castButton.visibleIf(it.first)
            binding.castThumbnailView.visibleIf(it.second)
            if (!it.second) {
                // reattach player to view if needed - in case when you stop stream cast
                with(binding.playerView) {
                    if (player != viewModel.player) player = viewModel.player
                }
            }
        }
        viewModel.viewEvents.collectWhenResumed(this) {
            when (it) {
                is BaseErrorEvent -> Toast.makeText(this, it.error.errorMessage, Toast.LENGTH_SHORT).show()
            }
        }
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
        with(binding) {
            playbackButton.visible()
            closeButton.visible()
            playerShadow.visible()
        }
        if (!isScreenPortrait()) StreamLayerUI.hideLaunchButton(this, false)
        viewModel.isControlsVisible = true
        controlsHandler.postDelayed({ hideControls() }, CONTROLS_AUTO_HIDE_DELAY)
    }

    private fun hideControls() {
        controlsHandler.removeCallbacksAndMessages(null)
        with(binding) {
            playbackButton.gone()
            closeButton.gone()
            playerShadow.gone()
        }
        if (!isScreenPortrait()) StreamLayerUI.hideLaunchButton(this, true)
        viewModel.isControlsVisible = false
    }

    private fun setPlaybackIcon() {
        binding.playbackButton.setImageResource(
            if (viewModel.isPlaybackPaused) R.drawable.sl_play_ic
            else R.drawable.sl_pause_ic
        )
    }

    private fun resumePlaying() {
        if (!viewModel.player.isPlaying && !viewModel.isPlaybackPaused) viewModel.player.playWhenReady = true
    }

    private fun pausePlaying() {
        if (viewModel.player.isPlaying) viewModel.player.playWhenReady = false
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
        viewModel.player.removeListener(playerListener)
        StreamLayer.setAudioDuckingListener(null)
        StreamLayer.setStreamEventChangeListener(null)
    }
}
