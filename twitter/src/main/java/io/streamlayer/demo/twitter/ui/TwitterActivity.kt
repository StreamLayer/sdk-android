package io.streamlayer.demo.twitter.ui

import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import io.streamlayer.demo.common.ext.changeFullScreen
import io.streamlayer.demo.common.ext.isScreenPortrait
import io.streamlayer.demo.common.ext.keepOnScreen
import io.streamlayer.demo.common.ext.setInputKeyboardEventListener
import io.streamlayer.demo.common.ext.windowController
import io.streamlayer.demo.twitter.databinding.ActivityTwitterBinding
import io.streamlayer.sdk.SLRAppHost
import io.streamlayer.sdk.StreamLayer.withStreamLayerUI

class TwitterActivity : AppCompatActivity() {

    private val viewModel: TwitterViewModel by viewModels()

    private lateinit var binding: ActivityTwitterBinding

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
        window.keepOnScreen()
        window.changeFullScreen(windowController, !isScreenPortrait())
        setInputKeyboardEventListener {
            // show fullscreen mode only if keyboard is closed in landscape
            if (!it && !isScreenPortrait()) window.changeFullScreen(windowController, true)
        }
        withStreamLayerUI { player = viewModel.appHostPlayer }
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
        viewModel.player.playWhenReady = true
    }

    override fun onStop() {
        super.onStop()
        viewModel.player.playWhenReady = false
    }

    private fun setupUI() {
        with(binding) {
            playerView.player = viewModel.player
            playerView.addOnLayoutChangeListener(layoutListener)
            twitterBtn.setOnClickListener {
                withStreamLayerUI { showOverlay(SLRAppHost.Overlay.Twitter) }
            }
        }
    }

    override fun onDestroy() {
        withStreamLayerUI { player = null }
        super.onDestroy()
        binding.playerView.removeOnLayoutChangeListener(layoutListener)
    }
}
