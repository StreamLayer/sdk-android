package io.streamlayer.demo

import android.app.Application
import android.util.Log
import io.branch.referral.Branch
import io.streamlayer.demo.common.exo.ExoVideoPlayerProvider
import io.streamlayer.sdk.SLRLogLevel
import io.streamlayer.sdk.SLRLogListener
import io.streamlayer.sdk.SLRTheme
import io.streamlayer.sdk.StreamLayer

class App : Application() {

    companion object {
        private const val TAG = "STREAM_LAYER_SDK"
        internal const val DEMO_STREAM = "https://hls.next.streamlayer.io/live/index.m3u8"
        var instance: Application? = null
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        Branch.getAutoInstance(this)
        // add log listener - it can be done before sdk init
        StreamLayer.setLogListener(object : SLRLogListener {
            override fun log(level: SLRLogLevel, msg: String) {
                when (level) {
                    SLRLogLevel.VERBOSE -> Log.v(TAG, msg)
                    SLRLogLevel.DEBUG -> Log.d(TAG, msg)
                    SLRLogLevel.INFO -> Log.i(TAG, msg)
                    SLRLogLevel.ERROR -> Log.e(TAG, msg)
                }
            }
        })
        // initialize sdk with your key
        StreamLayer.initializeApp(this, BuildConfig.SL_SDK_KEY)
        // enable external auth
        StreamLayer.setExternalAuthEnabled(true)
        // set phone contacts options
        StreamLayer.setPhoneContactsOptions(isUiEnabled = true, isSyncEnabled = false)
        // set custom media provider base on your exo player api
        StreamLayer.setVideoPlayerProvider(ExoVideoPlayerProvider(this))
        // set custom themes
        StreamLayer.theme = SLRTheme(
            authTheme = R.style.AuthOverlayTheme,
            mainTheme = R.style.MainOverlayTheme,
            profileTheme = R.style.ProfileOverlayTheme,
            baseTheme = R.style.BaseOverlayTheme,
            watchPartyTheme = R.style.WatchPartyOverlayTheme,
            inviteTheme = R.style.InviteOverlayTheme,
            predictionsTheme = R.style.PredictionsOverlayTheme,
            statisticsTheme = R.style.StatisticsOverlayTheme,
            messengerTheme = R.style.MessengerOverlayTheme,
            notificationsStyle = SLRTheme.NotificationsStyle.DESIGN_NUMBER_ONE
        )
    }
}
