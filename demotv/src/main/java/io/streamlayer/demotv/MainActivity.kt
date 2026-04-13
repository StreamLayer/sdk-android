package io.streamlayer.demotv

import android.graphics.Outline
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewOutlineProvider
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.coroutineScope
import androidx.lifecycle.lifecycleScope
import io.streamlayer.common.extensions.gone
import io.streamlayer.common.extensions.toast
import io.streamlayer.common.extensions.visible
import io.streamlayer.demo.common.DEMO_HLS_STREAM
import io.streamlayer.demo.common.exo.ExoPlayerHelper
import io.streamlayer.demotv.databinding.MainActivityBinding
import io.streamlayer.sdk.SLRAppHost
import io.streamlayer.sdk.SLREventSession
import io.streamlayer.sdk.SLRTimeCodeProvider
import io.streamlayer.sdk.StreamLayer
import io.streamlayer.sdk.StreamLayer.getSLRAppHost
import io.streamlayer.sdk.StreamLayer.withStreamLayerUI
import io.streamlayer.sdk.StreamLayerAd
import io.streamlayer.sdk.StreamLayerDemo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

private const val TAG = "MainActivity"

class MainActivity : AppCompatActivity() {

    lateinit var binding: MainActivityBinding

    private val exoHelper: ExoPlayerHelper by lazy {
        ExoPlayerHelper(this, getString(R.string.app_name))
    }

    // event session helper
    private var createEventSessionJob: Job? = null
    private var eventSession: SLREventSession? = null

    private val appHostDelegate = object : SLRAppHost.Delegate {
        //StreamLayer sdk notify what overlay is shown or closed.
        override fun onActionShown(action: SLRAppHost.ActionShown) {
            when (action.source) {
                SLRAppHost.ActionShown.Source.OVERLAY -> Unit
                SLRAppHost.ActionShown.Source.WATCH_PARTY_RETURN_BUTTON -> Unit
                SLRAppHost.ActionShown.Source.FULL_BLEED -> {
                    // do your action
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

                SLRAppHost.ActionClicked.Source.BACK_BUTTON -> {
                    //do your action
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
            loadDemoStream()
            playerView.player = exoHelper.player
            playerButton.gone()
            playerButton.setOnClickListener {
                playerView.player?.play()
                playerButton.gone()
                StreamLayerAd.hide()
                StreamLayerAd.cancel()
            }
            playerView.setOnClickListener {
                playerView.player?.pause()
                playerButton.visible()
                getSLRAppHost()?.run {
                    //Do your logic
                    if (currentOverlay() == SLRAppHost.OverlayType.Ad
                        || isAnyOverlayShown
                    ) return@setOnClickListener
                    //Example how to show prefetched Ad paused
                    lifecycle.coroutineScope.launch {
                        StreamLayerAd.googlePal {
                            viewFullScreen()
                            webView {
                                host = "https://bell-ad.streamlayer.io/"
                                url =
                                    "https://securepubads.g.doubleclick.net/gampad/ads?ad_rule=0&an=tsn&correlator=1774963552&description_url=https://www.tsn.ca/&env=instream&gdfp_req=1&hl=en&idtype=adid&is_lat=true&iu=/5479/tsn_tsndigital_cotv_samsungtv/playerpage/na_nba-on-tsn&cust_params=envr%3Ddev%26contentrating%3Dagvote%2Cqfrg%26genre%3Dbasketball%26adtarget%3D%26subtype%3Dtsn%2Ctsn2%2Ctsn2_bdu%2Ctsn_bdu%26pagetitle%3Dnba-on-tsn-mavericks-vs-rockets%26mediatype%3Dsports%26contenttype%3Dgame%26revshare%3Dna%26lang%3Den%26adrule%3D&msid=ca.tsn.tsngo&output=ldjh&pp=app_profile&rdid=${loadAndroidId()}&sz=315x400&tfcd=0&unviewed_position_start=1&url=ca.tsn.tsngo&vid=8243165&sid=a3098bb7-ae67-4d8c-b4be-3309cf64ca0d&impl=s&video_doc_id=8243165&scor=879066875&npa=0&wta=1&plcmt=1"
                            }
                        }
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
            adDelay(0) // Default 3000 ms
            binding.playerView.let { playerView ->
                playerView.post {
                    setPlayerViewSize(playerView.width, playerView.height)
                }
            }
        }
    }

    //Generate UUID of device
    private fun loadAndroidId(): String? = try {
        val androidId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
        UUID.nameUUIDFromBytes(androidId.toByteArray()).toString().also {
        }
    } catch (e: Exception) {
        null
    }

    // load demo streams and select first
    private fun loadDemoStream() {
        lifecycleScope.launch {
            // don't change date - it's for testing purposes
            val result =
                withContext(Dispatchers.IO) { kotlin.runCatching { StreamLayerDemo.getDemoStreams("2022-01-01") } }
            result.getOrNull()?.let { list ->
                Log.i(TAG, "Demo streams $list")
                list.firstOrNull()?.let { demoStream ->
                    Log.i(TAG, "Demo stream $demoStream")
                    exoHelper.init(demoStream.stream.ifEmpty { DEMO_HLS_STREAM })
                    createEventSession(demoStream.eventId.toString())
                }
            } ?: kotlin.run {
                result.exceptionOrNull()?.let { Log.e(TAG, "can not load stream", it) }
                toast("Can not load stream")
            }
        }
    }

    // create a new event session
    private fun createEventSession(id: String) {
        if (eventSession?.getExternalEventId() == id) return
        createEventSessionJob?.cancel()
        createEventSessionJob = lifecycleScope.launch {
            try {
                eventSession?.release()
                eventSession = StreamLayer.createEventSession(id, object : SLRTimeCodeProvider {
                    override fun getEpochTimeCodeInMillis() = exoHelper.getEpochTimeCodeInMillis()
                    override fun getTotalDurationInMillis(): Long = exoHelper.totalDuration()
                })
            } catch (t: Throwable) {
                Log.e(TAG, "createEventSession failed:", t)
            }
        }
    }

    override fun onDestroy() {
        createEventSessionJob?.cancel()
        eventSession?.release()
        super.onDestroy()
    }
}