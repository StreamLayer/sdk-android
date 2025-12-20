package io.streamlayer.demotv

import android.graphics.Outline
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewOutlineProvider
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.coroutineScope
import io.streamlayer.common.extensions.gone
import io.streamlayer.common.extensions.visible
import io.streamlayer.common.extensions.visibleIf
import io.streamlayer.demo.common.exo.ExoPlayerHelper
import io.streamlayer.demotv.databinding.MainActivityBinding
import io.streamlayer.sdk.SLRAppHost
import io.streamlayer.sdk.StreamLayer.withStreamLayerUI
import io.streamlayer.sdk.StreamLayerAd
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    lateinit var binding: MainActivityBinding

    private val exoHelper: ExoPlayerHelper by lazy {
        ExoPlayerHelper(this, getString(R.string.app_name))
    }

    private val appHostDelegate = object : SLRAppHost.Delegate {
        //StreamLayer sdk notify what overlay is shown or closed.
        override fun onActionShown(action: SLRAppHost.ActionShown) {
            when (action.source) {
                SLRAppHost.ActionShown.Source.OVERLAY -> Unit
                SLRAppHost.ActionShown.Source.WATCH_PARTY_RETURN_BUTTON -> Unit
                SLRAppHost.ActionShown.Source.FULL_BLEED -> {
                    binding.playerButton.visibleIf(!action.isShown)
                }
            }
        }

        override fun onActionClicked(action: SLRAppHost.ActionClicked): Boolean {
            //Example how to handle click events from StreamLayer sdk
            return when (action.source) {
                SLRAppHost.ActionClicked.Source.WATCH_PARTIES_CREATE_NEW_BUTTON -> false
                SLRAppHost.ActionClicked.Source.WATCH_PARTY_LEAVE_BUTTON -> false
                SLRAppHost.ActionClicked.Source.RESUME_PLAY_BUTTON -> {
                    //Stream Layer sdk requested player start to play
                    // start play video player
                    binding.playerButton.gone()
                    true
                }
            }
        }

        //StreamLayer sdk request video player volume
        override fun requestAudioDucking(level: Float) {
            exoHelper.notifyDuckingChanged(true, level)
        }

        //StreamLayer sdk request video player resume volume level
        override fun disableAudioDucking() {
            exoHelper.notifyDuckingChanged(false)
        }

        //StreamLayer sdk request set video player volume level
        override fun setAudioVolume(value: Float) {
            exoHelper.player.volume = value
        }

        //Return flow of current volume level of video player
        override fun getAudioVolumeListener(): Flow<Float> = exoHelper.getAudioVolumeListener()

        override fun onScreenSizeChanged(size: SLRAppHost.SLRScreenSize) {
            // update you player view
            binding.playerView.updateLayoutParams<ConstraintLayout.LayoutParams> {
                topMargin = size.topMargin
                bottomMargin = size.bottomMargin
                marginEnd = size.endMargin
                marginStart = size.startMargin
                width = size.playerMinWidth
                height = size.playerHeight
                verticalBias = size.verticalBias
                horizontalBias = 0f
            }
            binding.playerView.outlineProvider = object : ViewOutlineProvider() {
                override fun getOutline(view: View, outline: Outline) {
                    outline.setRoundRect(0, 0, view.width, view.height, size.playerCornerRadius)
                }
            }
            binding.playerView.clipToOutline = true
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = MainActivityBinding.inflate(LayoutInflater.from(this)).apply {
            setContentView(root)
            playerButton.setOnClickListener {
                playerButton.gone()
                StreamLayerAd.cancel()
            }
            playerView.setOnClickListener {
                playerButton.visible()
                //Example how to start Ad paused
                lifecycle.coroutineScope.launch {
                    StreamLayerAd.googlePal {
                        viewFullScreen()
                        contentVastUrl("vast_url")
                        resumeButtonImage()
                    }.onSuccess {
                        // hide your views if needed
                    }.onFailure {
                        // do you logic
                    }
                }
            }
        }
        // Setup StreamLayer Ui
        withStreamLayerUI {
            overlayLandscapeMode = SLRAppHost.OverlayLandscapeMode.LBAR
            lbarMode = SLRAppHost.LBarMode.SIDE_BAR
            isLaunchButtonEnabled = false
            delegate = appHostDelegate
            isWhoIsWatchingViewEnabled = false
            isGamesPointsEnabled = false
            setRootViewGroup(binding.root)
        }
    }
}