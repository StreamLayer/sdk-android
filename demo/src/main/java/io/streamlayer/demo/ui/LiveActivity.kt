package io.streamlayer.demo.ui

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.lifecycleScope
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout
import io.branch.referral.Branch
import io.streamlayer.auth.ui.StreamLayerAuthActivity
import io.streamlayer.common.extensions.changeFullScreen
import io.streamlayer.common.extensions.gone
import io.streamlayer.common.extensions.isScreenPortrait
import io.streamlayer.common.extensions.keepOnScreen
import io.streamlayer.common.extensions.setInputKeyboardEventListener
import io.streamlayer.common.extensions.toast
import io.streamlayer.common.extensions.visible
import io.streamlayer.common.extensions.visibleIf
import io.streamlayer.common.extensions.windowController
import io.streamlayer.demo.R
import io.streamlayer.demo.common.DEMO_HLS_STREAM
import io.streamlayer.demo.common.exo.ExoPlayerHelper
import io.streamlayer.demo.common.ext.*
import io.streamlayer.demo.databinding.ActivityLiveBinding
import io.streamlayer.sdk.SLRAppHost
import io.streamlayer.sdk.SLREventSession
import io.streamlayer.sdk.SLRInviteData
import io.streamlayer.sdk.SLRTimeCodeProvider
import io.streamlayer.sdk.StreamLayer
import io.streamlayer.sdk.StreamLayer.withStreamLayerUI
import io.streamlayer.sdk.StreamLayerDemo
import io.streamlayer.sdk.invite.StreamLayerInviteFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "LiveActivity"

private const val CONTROLS_AUTO_HIDE_DELAY = 5000L

class LiveActivity : AppCompatActivity(), StreamLayerInviteFragment.Listener {

    private lateinit var binding: ActivityLiveBinding

    private var isPlaybackPaused = false // check if player was stopped by user
    private var isControlsVisible = false // check if player controls are visible
    private var hideControlsJob: Job? = null

    // branch io listener
    private val branchReferralInitListener =
        Branch.BranchReferralInitListener { linkProperties, error ->
            if (error == null) linkProperties?.let { jsonObject ->
                SLRInviteData.fromJsonObject(jsonObject)?.let { invite ->
                    // check if user authorized or auth isn't required for this invite
                    if (StreamLayer.isUserAuthorized() || !invite.isAuthRequired()){
                        StreamLayer.handleInvite(invite, this)
                    } else{
                        // show your custom dialog or user streamlayer general invite dialog
                        StreamLayerInviteFragment.newInstance(invite)
                            .show(supportFragmentManager, StreamLayerInviteFragment::class.java.name)
                    }
                }
            }
        }

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

        override fun requestStream(id: String) {
            // SDK want to request new Event/Stream by id - process it if you need this functionality
        }
    }

    // listen player view layout changes
    private val layoutListener = View.OnLayoutChangeListener { view, _, _, _, _, _, _, _, _ ->
        view?.let {
            if (view.height > 0 && isScreenPortrait()) {
                withStreamLayerUI { overlayHeightSpace = view.height }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLiveBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupUI()
        loadDemoStream()
        withStreamLayerUI {
            // add host app delegate
            delegate = appHostDelegate
            isMenuProfileEnabled = false
        }
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        if (isControlsVisible) showControls()
        setPlaybackIcon()
    }

    private fun setupUI() {
        with(binding) {
            profileIV.setOnClickListener { StreamLayerAuthActivity.open(this@LiveActivity, false) }
            playerView.player = exoHelper.player
            playerView.videoSurfaceView?.setOnTouchListener(object : DoubleTapListener() {
                override fun onDelayedTap(x: Float, y: Float) {
                    if (isControlsVisible) hideControls() else showControls()
                }

                override fun onDoubleTap(x: Float, y: Float) {
                    if (playerView.resizeMode != AspectRatioFrameLayout.RESIZE_MODE_FIT) {
                        playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    } else if (playerView.resizeMode != AspectRatioFrameLayout.RESIZE_MODE_ZOOM) {
                        playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    }
                }
            })
            playbackButton.setOnClickListener {
                isPlaybackPaused = !isPlaybackPaused
                exoHelper.player.playWhenReady = !isPlaybackPaused
                setPlaybackIcon()
                showControls()
            }
            playerView.addOnLayoutChangeListener(layoutListener)
            window.keepOnScreen()
            window.changeFullScreen(windowController, !isScreenPortrait())
            setInputKeyboardEventListener {
                // show fullscreen mode only if keyboard is closed in landscape
                if (!it && !isScreenPortrait()) window.changeFullScreen(windowController, true)
            }
        }
    }


    private fun showControls() {
        hideControlsJob?.cancel()
        with(binding) {
            playbackButton.visible()
            playerShadow.visible()
        }
        // hide launch button in landscape
        if (!isScreenPortrait()) withStreamLayerUI { isLaunchButtonEnabled = false }
        isControlsVisible = true
        hideControlsJob = lifecycleScope.launch {
            delay(CONTROLS_AUTO_HIDE_DELAY)
            hideControls()
        }
    }

    private fun hideControls() {
        hideControlsJob?.cancel()
        with(binding) {
            playbackButton.gone()
            playerShadow.gone()
        }
        if (!isScreenPortrait()) withStreamLayerUI { isLaunchButtonEnabled = true }
        isControlsVisible = false
    }

    private fun setPlaybackIcon() {
        binding.playbackButton.setImageResource(
            if (isPlaybackPaused) R.drawable.sl_play_ic else R.drawable.sl_pause_ic
        )
    }

    private fun resumePlaying() {
        if (!exoHelper.player.isPlaying && !isPlaybackPaused) exoHelper.player.playWhenReady = true
    }

    private fun pausePlaying() {
        if (exoHelper.player.isPlaying) exoHelper.player.playWhenReady = false
    }

    override fun onResume() {
        super.onResume()
        // handle possible deeplink
        if (!StreamLayer.handleDeepLink(intent, this)) {
            // do host app logic if needed
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        if (intent == null) return
        this.intent = intent
        Branch.sessionBuilder(this)
            .withCallback(branchReferralInitListener)
            .reInit()
        // handle possible deeplink
        if (!StreamLayer.handleDeepLink(intent, this)) {
            // do host app logic if needed
        }
    }

    override fun onStart() {
        super.onStart()
        resumePlaying()
        Branch.sessionBuilder(this)
            .withCallback(branchReferralInitListener)
            .withData(this.intent?.data).init()
    }

    override fun onStop() {
        super.onStop()
        pausePlaying()
    }

    override fun onMultiWindowModeChanged(isInMultiWindowMode: Boolean, newConfig: Configuration) {
        super.onMultiWindowModeChanged(isInMultiWindowMode, newConfig)
        if (isInMultiWindowMode) hideControls()
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        if (isInPictureInPictureMode) hideControls()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        configureLayout()
    }

    private fun configureLayout() {
        val isScreenPortrait = isScreenPortrait()
        with(binding) {
            toolbar.visibleIf(isScreenPortrait)
            playerView.updateLayoutParams<ConstraintLayout.LayoutParams> {
                topToBottom = if (isScreenPortrait) R.id.toolbar else ConstraintSet.UNSET
                topToTop = if (isScreenPortrait) ConstraintSet.UNSET else ConstraintSet.PARENT_ID
                bottomToBottom =
                    if (isScreenPortrait) ConstraintSet.UNSET else ConstraintSet.PARENT_ID
                dimensionRatio = if (isScreenPortrait) "H,16:9" else ""
            }
        }
        window.changeFullScreen(windowController, !isScreenPortrait)
    }


    override fun onInviteStart(inviteData: SLRInviteData) {
        StreamLayerAuthActivity.open(this, closeWhenAuthorized = false, inviteData = inviteData)
    }

    override fun onDestroy() {
        // remove host app player
        withStreamLayerUI { delegate = null }
        super.onDestroy()
        // remove player view layout listener
        binding.playerView.removeOnLayoutChangeListener(layoutListener)
        // release player
        exoHelper.release()
        // release event session
        createEventSessionJob?.cancel()
        eventSession?.release()
        // cancel hide controls
        hideControlsJob?.cancel()
    }

    // load demo streams and select first
    private fun loadDemoStream() {
        lifecycleScope.launch {
            // don't change date - it's for testing purposes
            val result =
                withContext(Dispatchers.IO) { kotlin.runCatching { StreamLayerDemo.getDemoStreams("2022-01-01") } }
            result.getOrNull()?.let { list ->
                list.firstOrNull()?.let {
                    exoHelper.init(it.stream.ifEmpty { DEMO_HLS_STREAM })
                    createEventSession(it.eventId.toString())
                    isPlaybackPaused = false
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
                })
            } catch (t: Throwable) {
                Log.e(TAG, "createEventSession failed:", t)
            }
        }
    }
}
