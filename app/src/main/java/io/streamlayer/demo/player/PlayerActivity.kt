package io.streamlayer.demo.player

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.viewModels
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.Observer
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout
import io.streamlayer.demo.R
import io.streamlayer.demo.common.mvvm.BaseActivity
import io.streamlayer.demo.common.mvvm.Status
import io.streamlayer.demo.common.recyclerview.DashedDividerDecoration
import io.streamlayer.demo.utils.DoubleClickListener
import io.streamlayer.demo.utils.visible
import io.streamlayer.sdk.StreamLayer
import io.streamlayer.sdk.StreamLayerComponents
import kotlinx.android.synthetic.main.activity_player.*
import kotlin.math.min

class PlayerActivity : BaseActivity() {

    private val streamsAdapter: StreamsAdapter by lazy { StreamsAdapter() }

    private val viewModel: PlayerViewModel by viewModels { viewModelFactory }

    private val playerListener = object : Player.EventListener {

        override fun onPlayerError(error: ExoPlaybackException) {
            val exceptionMessage = when (error.type) {
                ExoPlaybackException.TYPE_SOURCE -> error.sourceException.localizedMessage
                ExoPlaybackException.TYPE_RENDERER -> error.rendererException.localizedMessage
                ExoPlaybackException.TYPE_UNEXPECTED -> error.unexpectedException.localizedMessage
                ExoPlaybackException.TYPE_OUT_OF_MEMORY -> error.outOfMemoryError.localizedMessage
                ExoPlaybackException.TYPE_TIMEOUT -> error.timeoutException.localizedMessage
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

    private fun setupUI() {
        viewModel.exoPlayer.addListener(playerListener)
        if (viewModel.isPlaybackPaused) videoLoader.hide()
        playerView.player = viewModel.exoPlayer
        playerView.videoSurfaceView?.setOnClickListener(DoubleClickListener(
            {
                if (close_button.isPlayerControlVisible()) {
                    close_button.visibility = View.GONE
                    if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                        StreamLayerComponents.showStreamLayerButton(this)
                    }
                } else {
                    if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                        StreamLayerComponents.hideStreamLayerButton(this)
                    }
                    close_button.let {
                        it.visibility = View.VISIBLE
                        it.alpha = 1f
                        it.animate()
                            .setStartDelay(4900)
                            .setDuration(200)
                            .alpha(0f)
                            .withEndAction {
                                if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                                    StreamLayerComponents.showStreamLayerButton(this)
                                }
                            }
                            .start()
                    }
                }

                if (viewModel.isPlaybackPaused) {
                    if (play_button.isPlayerControlVisible()) {
                        play_button.let {
                            it.visibility = View.GONE
                            it.alpha = 0f
                        }
                    } else  {
                        play_button.let {
                            it.visibility = View.VISIBLE
                            it.alpha = 1f
                            it.animate()
                                .setStartDelay(4900)
                                .setDuration(200)
                                .alpha(0f)
                                .start()
                        }
                    }
                } else {
                    if (pause_button.isPlayerControlVisible()) {
                        pause_button.let {
                            it.visibility = View.GONE
                            it.alpha = 0f
                        }
                    } else {
                        pause_button.let {
                            it.visibility = View.VISIBLE
                            it.alpha = 1f
                            it.animate()
                                .setStartDelay(4900)
                                .setDuration(200)
                                .alpha(0f)
                                .start()
                        }
                    }
                }
            },
            {
                if (playerView.resizeMode != AspectRatioFrameLayout.RESIZE_MODE_FIT) {
                    playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                } else if (playerView.resizeMode != AspectRatioFrameLayout.RESIZE_MODE_ZOOM) {
                    playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                }
            }
        ))
        close_button.setOnClickListener { finish() }
        play_button.setOnClickListener {
            viewModel.isPlaybackPaused = false
            viewModel.exoPlayer.prepare()
            viewModel.exoPlayer.playWhenReady = true
            it.visibility = View.GONE
            pause_button.visibility = View.VISIBLE
            pause_button.alpha = 1f
            close_button.clearAnimation()
            close_button.visibility = View.VISIBLE
            close_button.alpha = 1f
            pause_button.animate()
                .setStartDelay(4900)
                .setDuration(200)
                .alpha(0f)
                .withEndAction {
                    close_button.alpha = 0f
                }
                .start()
        }

        pause_button.setOnClickListener {
            viewModel.isPlaybackPaused = true
            viewModel.exoPlayer.playWhenReady = false
            it.visibility = View.GONE
            play_button.visibility = View.VISIBLE
            play_button.alpha = 1f
            close_button.clearAnimation()
            close_button.visibility = View.VISIBLE
            close_button.alpha = 1f
            play_button.animate()
                .setStartDelay(4900)
                .setDuration(200)
                .alpha(0f)
                .withEndAction {
                    close_button.alpha = 0f
                }
                .start()
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

        StreamLayer.setStreamEventChangeListener { eventId -> viewModel.requestStreamEvent(eventId) }
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

    private fun resumePlaying() {
        if (!viewModel.exoPlayer.isPlaying && !viewModel.isPlaybackPaused) viewModel.exoPlayer.play()
    }

    private fun pausePlaying() {
        if (viewModel.exoPlayer.isPlaying) viewModel.exoPlayer.pause()
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
        if (!StreamLayer.handleDeepLinkIntent(intent, this)) {
            // do host logic if needed
        }
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

    override fun onDestroy() {
        super.onDestroy()
        viewModel.exoPlayer.removeListener(playerListener)
        StreamLayer.setAudioDuckingListener(null)
        StreamLayer.setStreamEventChangeListener(null)
    }

    private fun View.isPlayerControlVisible(): Boolean{
        return visibility == View.VISIBLE && alpha > 0f
    }
}
