package io.streamlayer.demo.live

import android.app.PictureInPictureParams
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.view.updateLayoutParams
import com.google.android.exoplayer2.PlaybackException
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout
import io.streamlayer.demo.R
import io.streamlayer.demo.common.ext.*
import io.streamlayer.demo.common.ext.changeFullScreen
import io.streamlayer.demo.common.ext.isMultiWindowOrPiPModeEnabled
import io.streamlayer.demo.common.ext.isScreenPortrait
import io.streamlayer.demo.common.ext.windowController
import io.streamlayer.demo.databinding.ActivityLiveBinding
import io.streamlayer.sdk.SLRAudioDuckingListener
import io.streamlayer.sdk.SLREventChangeListener
import io.streamlayer.sdk.StreamLayer
import io.streamlayer.sdk.StreamLayer.withStreamLayerUI

private const val CONTROLS_AUTO_HIDE_DELAY = 5000L

class LiveActivity : AppCompatActivity() {

    companion object {

        fun open(context: Context) {
            context.startActivity(Intent(context, LiveActivity::class.java))
        }
    }

    private val viewModel: LiveViewModel by viewModels()

    private lateinit var binding: ActivityLiveBinding

    private val controlsHandler = Handler()

    private val playerListener = object : Player.Listener {

        override fun onPlayerError(error: PlaybackException) {
            Toast.makeText(
                this@LiveActivity,
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

    private val audioDuckingListener = object : SLRAudioDuckingListener {

        override fun requestAudioDucking(level: Float) {
            viewModel.notifyDuckingChanged(true, level)
        }

        override fun disableAudioDucking() {
            viewModel.notifyDuckingChanged(false)
        }
    }

    private val containerLayoutBoundsListener =
        View.OnLayoutChangeListener { view, _, _, _, _, _, _, _, _ ->
            view?.let {
                val playerHeight = binding.playerView.height
                if (it.height != 0 && playerHeight != 0 && isScreenPortrait() && !isMultiWindowOrPiPModeEnabled()) {
                    val newHeight = it.height - playerHeight
                    withStreamLayerUI { if (overlayHeight != newHeight) overlayHeight = newHeight }
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLiveBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupUI()
        bind()
        window.addFlags(
            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                    or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )
        window.changeFullScreen(windowController, !isScreenPortrait())
        setInputKeyboardEventListener {
            // show fullscreen mode only if keyboard is closed in landscape
            if (!it && !isScreenPortrait()) window.changeFullScreen(windowController, true)
        }
        binding.container.addOnLayoutChangeListener(containerLayoutBoundsListener)
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        if (viewModel.isControlsVisible) showControls()
        setPlaybackIcon()
    }

    private fun setupUI() {
        with(binding) {
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
            closeButton.setOnClickListener { finish() }
            playbackButton.setOnClickListener {
                viewModel.isPlaybackPaused = !viewModel.isPlaybackPaused
                viewModel.player.playWhenReady = !viewModel.isPlaybackPaused
                setPlaybackIcon()
                showControls()
            }
            pipButton.setOnClickListener {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                    packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)
                ) {
                    enterPictureInPictureMode(PictureInPictureParams.Builder().build())
                } else Toast.makeText(
                    this@LiveActivity,
                    "Picture in picture mode is not supported",
                    Toast.LENGTH_LONG
                ).show()
            }

            StreamLayer.addAudioDuckingListener(audioDuckingListener)

            StreamLayer.setEventChangeListener(object : SLREventChangeListener {

                override fun onChanged(id: String) {
                    // SDK requested stream changes - id is id of your stream
                }

            })
        }
    }

    private fun bind() {
        viewModel.viewEvents.collectWhenResumed(this) {
            when (it) {
                is BaseErrorEvent -> Toast.makeText(this, it.error, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showControls() {
        controlsHandler.removeCallbacksAndMessages(null)
        with(binding) {
            playbackButton.visible()
            closeButton.visible()
            playerShadow.visible()
            pipButton.visible()
        }
        if (!isScreenPortrait()) withStreamLayerUI { isLaunchButtonEnabled = false }
        viewModel.isControlsVisible = true
        controlsHandler.postDelayed({ hideControls() }, CONTROLS_AUTO_HIDE_DELAY)
    }

    private fun hideControls() {
        controlsHandler.removeCallbacksAndMessages(null)
        with(binding) {
            playbackButton.gone()
            closeButton.gone()
            playerShadow.gone()
            pipButton.gone()
        }
        if (!isScreenPortrait()) withStreamLayerUI { isLaunchButtonEnabled = true }
        viewModel.isControlsVisible = false
    }

    private fun setPlaybackIcon() {
        binding.playbackButton.setImageResource(
            if (viewModel.isPlaybackPaused) R.drawable.sl_play_ic else R.drawable.sl_pause_ic
        )
    }

    private fun resumePlaying() {
        if (!viewModel.player.isPlaying && !viewModel.isPlaybackPaused) viewModel.player.playWhenReady =
            true
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
            // do host app logic if needed
        }
        // verify and create new event session if needed
        viewModel.verifyEventSession()
    }

    override fun onStart() {
        super.onStart()
        resumePlaying()
    }

    override fun onStop() {
        super.onStop()
        pausePlaying()
    }

    override fun onMultiWindowModeChanged(isInMultiWindowMode: Boolean, newConfig: Configuration?) {
        super.onMultiWindowModeChanged(isInMultiWindowMode, newConfig)
        if (isInMultiWindowMode) hideControls()
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration?
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        if (isInPictureInPictureMode) hideControls()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        val isScreenPortrait = isScreenPortrait()
        binding.playerView.updateLayoutParams<ConstraintLayout.LayoutParams> {
            bottomToBottom = if (isScreenPortrait) ConstraintSet.UNSET else ConstraintSet.PARENT_ID
        }
        window.changeFullScreen(windowController, !isScreenPortrait)
    }

    /**
     * If your activity launch mode is set to [Intent.FLAG_ACTIVITY_SINGLE_TOP], listen for [onNewIntent] as well
     * because the [onResume] will not be triggered if the activity is already running.
     */
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        if (intent == null) return
        if (!StreamLayer.handleDeepLink(intent, this)) {
            // do host app logic if needed
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.container.removeOnLayoutChangeListener(containerLayoutBoundsListener)
        controlsHandler.removeCallbacksAndMessages(null)
        viewModel.player.removeListener(playerListener)
        StreamLayer.removeAudioDuckingListener(audioDuckingListener)
        StreamLayer.setEventChangeListener(null)
    }
}
