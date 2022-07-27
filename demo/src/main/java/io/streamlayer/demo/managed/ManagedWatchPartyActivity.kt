package io.streamlayer.demo.managed

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.doOnPreDraw
import com.google.android.exoplayer2.PlaybackException
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout
import io.streamlayer.demo.R
import io.streamlayer.demo.common.ext.*
import io.streamlayer.demo.common.ext.DoubleTapListener
import io.streamlayer.demo.common.ext.isScreenPortrait
import io.streamlayer.demo.databinding.ActivityManagedWatchPartyBinding
import io.streamlayer.sdk.SLRAppHost
import io.streamlayer.sdk.StreamLayer.withStreamLayerUI

private const val CONTROLS_AUTO_HIDE_DELAY = 5000L

class ManagedWatchPartyActivity : AppCompatActivity() {

    companion object {

        fun open(context: Context) {
            context.startActivity(Intent(context, ManagedWatchPartyActivity::class.java))
        }
    }

    private val viewModel: ManagedWatchPartyViewModel by viewModels()

    private lateinit var binding: ActivityManagedWatchPartyBinding

    private val controlsHandler = Handler()

    private val playerListener = object : Player.Listener {

        override fun onPlayerError(error: PlaybackException) {
            Toast.makeText(
                this@ManagedWatchPartyActivity,
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
        binding = ActivityManagedWatchPartyBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupUI()
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        if (viewModel.isControlsVisible) showControls()
        setPlaybackIcon()
        // disable all unused sdk components for this mode
        withStreamLayerUI {
            isLaunchButtonEnabled = false
            isWhoIsWatchingViewEnabled = false
            inAppNotificationsMode = SLRAppHost.NotificationMode.Silent
            isPredictionsPointsEnabled = false
            isWatchPartyReturnButtonEnabled = false
            isTooltipsEnabled = false
        }
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
            if (isScreenPortrait()) playerView.doOnPreDraw {
                val playerHeight = binding.playerView.height
                val newHeight = it.height - playerHeight
                withStreamLayerUI { if (overlayHeight != newHeight) overlayHeight = newHeight }
            }
            closeButton.setOnClickListener { finish() }
            playbackButton.setOnClickListener {
                viewModel.isPlaybackPaused = !viewModel.isPlaybackPaused
                viewModel.player.playWhenReady = !viewModel.isPlaybackPaused
                setPlaybackIcon()
                showControls()
            }
        }
        window.keepOnScreen()
    }

    private fun showControls() {
        controlsHandler.removeCallbacksAndMessages(null)
        with(binding) {
            playbackButton.visible()
            closeButton.visible()
            playerShadow.visible()
        }
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

    override fun onStart() {
        super.onStart()
        resumePlaying()
    }

    override fun onStop() {
        super.onStop()
        pausePlaying()
    }

    override fun onDestroy() {
        super.onDestroy()
        controlsHandler.removeCallbacksAndMessages(null)
        viewModel.player.removeListener(playerListener)
    }
}
