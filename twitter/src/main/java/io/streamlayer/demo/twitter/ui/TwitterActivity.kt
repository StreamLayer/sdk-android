package io.streamlayer.demo.twitter.ui

import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.view.updateLayoutParams
import io.streamlayer.common.extensions.changeFullScreen
import io.streamlayer.common.extensions.isScreenPortrait
import io.streamlayer.common.extensions.keepOnScreen
import io.streamlayer.common.extensions.setInputKeyboardEventListener
import io.streamlayer.common.extensions.windowController
import io.streamlayer.demo.common.DEMO_HLS_STREAM
import io.streamlayer.demo.common.exo.ExoPlayerHelper
import io.streamlayer.demo.twitter.R
import io.streamlayer.demo.twitter.databinding.ActivityTwitterBinding
import io.streamlayer.sdk.SLRAppHost
import io.streamlayer.sdk.StreamLayer.withStreamLayerUI
import kotlinx.coroutines.flow.Flow

class TwitterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTwitterBinding

    // exo player helper
    private val exoHelper: ExoPlayerHelper by lazy {
        ExoPlayerHelper(this, getString(R.string.app_name))
    }

    // app host delegate
    private val appHostDelegate = object : SLRAppHost.Delegate {

        override fun requestAudioDucking(level: Float) {
            exoHelper.notifyDuckingChanged(true, level)
        }

        override fun disableAudioDucking() {
            exoHelper.notifyDuckingChanged(false)
        }

        override fun setAudioVolume(value: Float) {
            exoHelper.player.volume = value
        }

        override fun getAudioVolumeListener(): Flow<Float> = exoHelper.getAudioVolumeListener()

        override fun requestStream(id: String) {
            // SDK want to request new Event/Stream by id - process it if you need this functionality
        }
    }

    private val layoutListener = View.OnLayoutChangeListener { view, _, _, _, _, _, _, _, _ ->
        view?.let {
            if (view.height > 0 && isScreenPortrait()) {
                withStreamLayerUI { overlayHeightSpace = view.height }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTwitterBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupUI()
        loadDemoStream()
        withStreamLayerUI {
            // set host app player
            delegate = appHostDelegate
            // disable sdk ui views
            isLaunchButtonEnabled = false
            isWhoIsWatchingViewEnabled = false
            isPredictionsPointsEnabled = false
            isWatchPartyReturnButtonEnabled = false
            isTooltipsEnabled = false
            isMenuProfileEnabled = false
            inAppNotificationsMode = SLRAppHost.NotificationMode.List(
                listOf(SLRAppHost.NotificationMode.Feature.TWITTER)
            )
        }
    }

    override fun onStart() {
        super.onStart()
        exoHelper.player.playWhenReady = true
    }

    override fun onStop() {
        super.onStop()
        exoHelper.player.playWhenReady = false
    }

    private fun setupUI() {
        with(binding) {
            playerView.player = exoHelper.player
            playerView.addOnLayoutChangeListener(layoutListener)
            twitterBtn.setOnClickListener {
                withStreamLayerUI { showOverlay(SLRAppHost.Overlay.Twitter) }
            }
            window.keepOnScreen()
            window.changeFullScreen(windowController, !isScreenPortrait())
            setInputKeyboardEventListener {
                // show fullscreen mode only if keyboard is closed in landscape
                if (!it && !isScreenPortrait()) window.changeFullScreen(windowController, true)
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        val isScreenPortrait = isScreenPortrait()
        binding.playerView.updateLayoutParams<ConstraintLayout.LayoutParams> {
            bottomToBottom = if (isScreenPortrait) ConstraintSet.UNSET else ConstraintSet.PARENT_ID
        }
        window.changeFullScreen(windowController, !isScreenPortrait)
    }

    override fun onDestroy() {
        withStreamLayerUI { delegate = null }
        super.onDestroy()
        binding.playerView.removeOnLayoutChangeListener(layoutListener)
        // release player
        exoHelper.release()
    }

    private fun loadDemoStream() {
        exoHelper.init(DEMO_HLS_STREAM)
    }
}
