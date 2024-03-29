package io.streamlayer.demo.gamification.ui

import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.lifecycleScope
import io.streamlayer.common.extensions.changeFullScreen
import io.streamlayer.common.extensions.isScreenPortrait
import io.streamlayer.common.extensions.keepOnScreen
import io.streamlayer.common.extensions.setInputKeyboardEventListener
import io.streamlayer.common.extensions.windowController
import io.streamlayer.demo.common.DEMO_HLS_STREAM
import io.streamlayer.demo.common.exo.ExoPlayerHelper
import io.streamlayer.demo.gamification.App
import io.streamlayer.demo.gamification.R
import io.streamlayer.demo.gamification.databinding.ActivityGamificationBinding
import io.streamlayer.sdk.SLRAppHost
import io.streamlayer.sdk.SLREventSession
import io.streamlayer.sdk.StreamLayer
import io.streamlayer.sdk.StreamLayer.withStreamLayerUI
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

private const val TAG = "GamificationActivity"

class GamificationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGamificationBinding

    // event session helper
    private var createEventSessionJob: Job? = null
    private var eventSession: SLREventSession? = null

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
        binding = ActivityGamificationBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupUI()
        loadDemoStream()
        withStreamLayerUI {
            // add app host delegate
            delegate = appHostDelegate
            // disable unused sdk ui views
            isLaunchButtonEnabled = false
            isWhoIsWatchingViewEnabled = false
            isWatchPartyReturnButtonEnabled = false
            isTooltipsEnabled = false
            isMenuProfileEnabled = false
            inAppNotificationsMode = SLRAppHost.NotificationMode.List(
                listOf(
                    SLRAppHost.NotificationMode.Feature.GAMES,
                    SLRAppHost.NotificationMode.Feature.HIGHLIGHTS
                )
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
            highlightsBtn.setOnClickListener {
                withStreamLayerUI { showOverlay(SLRAppHost.Overlay.Highlights) }
            }
            gamesBtn.setOnClickListener {
                withStreamLayerUI { showOverlay(SLRAppHost.Overlay.Games) }
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
        // remove app host delegate
        withStreamLayerUI { delegate = null }
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
