package io.streamlayer.demo.player

import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.doOnPreDraw
import androidx.lifecycle.Observer
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout
import io.streamlayer.demo.R
import io.streamlayer.demo.common.kotlin.gone
import io.streamlayer.demo.common.kotlin.visible
import io.streamlayer.demo.common.mvvm.BaseActivity
import io.streamlayer.demo.utils.DoubleClickListener
import io.streamlayer.demo.utils.isScreenPortrait
import io.streamlayer.sdk.StreamLayer
import io.streamlayer.sdk.StreamLayerUI
import kotlinx.android.synthetic.main.activity_player.*
import kotlin.math.min

private const val CONTROLS_AUTO_HIDE_DELAY = 5000L

class PlayerActivity : BaseActivity() {

    private val viewModel: PlayerViewModel by viewModels { viewModelFactory }

    private val controlsHandler = Handler()

    private val playerListener = object : Player.Listener {

        override fun onPlayerError(error: ExoPlaybackException) {
            val exceptionMessage = when (error.type) {
                ExoPlaybackException.TYPE_SOURCE -> error.sourceException.localizedMessage
                ExoPlaybackException.TYPE_RENDERER -> error.rendererException.localizedMessage
                ExoPlaybackException.TYPE_UNEXPECTED -> error.unexpectedException.localizedMessage
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
        if (!isScreenPortrait(this)) setFullScreen()
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
        if (isScreenPortrait(this)) {
            if (playerView.height != 0) StreamLayerUI.setOverlayViewHeight(this, container.height - playerView.height)
            else playerView.doOnPreDraw { playerView ->
                StreamLayerUI.setOverlayViewHeight(this, container.height - playerView.height)
            }
        }
        close_button.setOnClickListener { finish() }
        playback_button.setOnClickListener {
            viewModel.isPlaybackPaused = !viewModel.isPlaybackPaused
            viewModel.exoPlayer.playWhenReady = !viewModel.isPlaybackPaused
            setPlaybackIcon()
            showControls()
        }

        StreamLayer.setAudioDuckingListener(object : StreamLayer.AudioDuckingListener {

            override fun requestAudioDucking() {
                viewModel.exoPlayer.audioComponent?.let { audio ->
                    // decrease volume to 10% if louder, otherwise keep the current volume
                    if (viewModel.volumeBeforeDucking == null) {
                        viewModel.volumeBeforeDucking = audio.volume
                        audio.volume = min(audio.volume, 0.1f)
                    }
                }
            }

            override fun disableAudioDucking() {
                viewModel.exoPlayer.audioComponent?.let { audio ->
                    viewModel.volumeBeforeDucking?.let { volume ->
                        audio.volume = volume
                        viewModel.volumeBeforeDucking = null
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
            it.error?.let { Toast.makeText(this, it.errorMessage, Toast.LENGTH_SHORT).show() }
        })
        viewModel.networkConnectionLiveData.observe(this, {
            if (it) {
                resumePlaying()
                viewModel.refresh()
            } else pausePlaying()
        })
    }

    private fun showControls() {
        controlsHandler.removeCallbacksAndMessages(null)
        playback_button?.visible()
        close_button?.visible()
        player_shadow?.visible()
        if (!isScreenPortrait(this)) StreamLayerUI.hideLaunchButton(this)
        viewModel.isControlsVisible = true
        controlsHandler.postDelayed({ hideControls() }, CONTROLS_AUTO_HIDE_DELAY)
    }

    private fun hideControls() {
        controlsHandler.removeCallbacksAndMessages(null)
        playback_button?.gone()
        close_button?.gone()
        player_shadow?.gone()
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

    private fun AppCompatActivity.setFullScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.apply {
                clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
                setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
                addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
                decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE
                        // Set the content to appear under the system bars so that the
                        // content doesn't resize when the system bars hide and show.
                        or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        // Hide the nav bar and status bar
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_FULLSCREEN)
                statusBarColor = Color.TRANSPARENT
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) setDecorFitsSystemWindows(true)
            }
        } else {
            window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        }
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

    /**
     * If your activity launch mode is set to [Intent.FLAG_ACTIVITY_SINGLE_TOP], listen for [onNewIntent] as well
     * because the [onResume] will not be triggered if the activity is already running.
     */
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        if (intent == null) return
        this.intent = intent
        if (!StreamLayer.handleDeepLink(intent, this)) {
            // do host logic if needed
        }
    }

    override fun onStop() {
        super.onStop()
        pausePlaying()
    }

    override fun onDestroy() {
        super.onDestroy()
        controlsHandler.removeCallbacksAndMessages(null)
        viewModel.exoPlayer.removeListener(playerListener)
        StreamLayer.setAudioDuckingListener(null)
        StreamLayer.setStreamEventChangeListener(null)
    }
}
