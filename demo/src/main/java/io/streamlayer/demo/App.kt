package io.streamlayer.demo

import android.app.Application
import android.content.Context
import android.util.Log
import io.branch.referral.Branch
import io.streamlayer.auth.ui.StreamLayerAuthActivity
import io.streamlayer.demo.common.exo.ExoVideoPlayerProvider
import io.streamlayer.sdk.SLRAuthRequestHandler
import io.streamlayer.sdk.SLRLogListener
import io.streamlayer.sdk.SLRTheme
import io.streamlayer.sdk.StreamLayer

class App : Application() {

    companion object {
        private const val TAG = "STREAM_LAYER_SDK"
        var instance: Application? = null
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        Branch.getAutoInstance(this)
        // add log listener - it can be done before sdk init
        StreamLayer.setLogListener(object : SLRLogListener {
            override fun log(level: SLRLogListener.Level, msg: String) {
                when (level) {
                    SLRLogListener.Level.VERBOSE -> Log.v(TAG, msg)
                    SLRLogListener.Level.DEBUG -> Log.d(TAG, msg)
                    SLRLogListener.Level.INFO -> Log.i(TAG, msg)
                    SLRLogListener.Level.ERROR -> Log.e(TAG, msg)
                    else -> {}
                }
            }
        })
        // initialize sdk with your key
        StreamLayer.initializeApp(this, BuildConfig.SL_SDK_KEY)
        // set auth handler - we use stream layer phone authorization for this demo project
        StreamLayer.setAuthHandler(object : SLRAuthRequestHandler {
            override fun onAuthRequired(context: Context) {
                StreamLayerAuthActivity.open(context, true)
            }
        })
        // set phone contacts options
        StreamLayer.setPhoneContactsOptions(isUiEnabled = true, isSyncEnabled = true)
        // set custom media provider base on your exo player api
        StreamLayer.setVideoPlayerProvider(ExoVideoPlayerProvider(this))
        // set custom themes
        StreamLayer.setCustomTheme(
            SLRTheme(
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
        )
    }
}
