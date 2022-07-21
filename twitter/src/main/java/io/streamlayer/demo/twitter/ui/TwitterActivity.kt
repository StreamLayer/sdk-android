package io.streamlayer.demo.twitter.ui

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.doOnPreDraw
import io.streamlayer.demo.common.ext.changeFullScreen
import io.streamlayer.demo.common.ext.isScreenPortrait
import io.streamlayer.demo.common.ext.keepOnScreen
import io.streamlayer.demo.common.ext.windowController
import io.streamlayer.demo.twitter.databinding.ActivityTwitterBinding
import io.streamlayer.sdk.StreamLayer.withStreamLayerUI

class TwitterActivity : AppCompatActivity() {

    private val viewModel: TwitterViewModel by viewModels()

    private lateinit var binding: ActivityTwitterBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTwitterBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupUI()
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        // disable sdk ui views
        withStreamLayerUI {
            isWhoIsWatchingViewEnabled = false
            isPredictionsPointsEnabled = false
            isWatchPartyReturnButtonEnabled = false
            isTooltipsEnabled = false
            isMenuProfileEnabled = false
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
            window.keepOnScreen()
            val isScreenPortrait = isScreenPortrait()
            if (isScreenPortrait) {
                // check if screen orientation is portrait and set overlay height when it will be available
                playerView.doOnPreDraw {
                    val newHeight = container.height - it.height
                    withStreamLayerUI { if (overlayHeight != newHeight) overlayHeight = newHeight }
                }
            } else {
                // setup fullscreen mode for better ux in landscape
                window.changeFullScreen(windowController, true)
            }
        }
    }
}
