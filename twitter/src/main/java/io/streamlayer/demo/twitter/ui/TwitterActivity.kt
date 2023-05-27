package io.streamlayer.demo.twitter.ui

import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.lifecycleScope
import io.streamlayer.demo.common.DEMO_HLS_STREAM
import io.streamlayer.demo.common.exo.ExoPlayerHelper
import io.streamlayer.demo.common.ext.changeFullScreen
import io.streamlayer.demo.common.ext.isScreenPortrait
import io.streamlayer.demo.common.ext.keepOnScreen
import io.streamlayer.demo.common.ext.setInputKeyboardEventListener
import io.streamlayer.demo.common.ext.windowController
import io.streamlayer.demo.twitter.App
import io.streamlayer.demo.twitter.R
import io.streamlayer.demo.twitter.databinding.ActivityTwitterBinding
import io.streamlayer.sdk.SLRAppHost
import io.streamlayer.sdk.SLREventSession
import io.streamlayer.sdk.StreamLayer
import io.streamlayer.sdk.StreamLayer.withStreamLayerUI
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

private const val TAG = "GamificationActivity"

class TwitterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTwitterBinding

    // event session helper
    private var createEventSessionJob: Job? = null
    private var eventSession: SLREventSession? = null

    // exo player helper
    private val exoHelper: ExoPlayerHelper by lazy {
        ExoPlayerHelper(this, getString(R.string.app_name))
    }

    // app host player
    private val appHostPlayer = object : SLRAppHost.Player {

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
        // set host app player
        withStreamLayerUI { player = appHostPlayer }
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        // disable sdk ui views
        withStreamLayerUI {
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
        withStreamLayerUI { player = null }
        super.onDestroy()
        binding.playerView.removeOnLayoutChangeListener(layoutListener)
        // release player
        exoHelper.release()
        // release event session
        createEventSessionJob?.cancel()
        eventSession?.release()
    }

    private fun loadDemoStream() {
        exoHelper.init(DEMO_HLS_STREAM)
        createEventSession(App.DEMO_EVENT_ID)
    }

    // create a new event session
    private fun createEventSession(id: String) {
        if (eventSession?.getExternalEventId() == id) return
        createEventSessionJob?.cancel()
        createEventSessionJob = lifecycleScope.launch {
            try {
                eventSession?.release()
                eventSession = StreamLayer.createEventSession(id, null)
            } catch (t: Throwable) {
                Log.e(TAG, "createEventSession failed:", t)
            }
        }
    }

}
