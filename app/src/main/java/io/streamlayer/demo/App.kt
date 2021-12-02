package io.streamlayer.demo

import android.app.Application
import io.branch.referral.Branch
import io.streamlayer.demo.common.firebase.NotificationChannelsHelper
import io.streamlayer.demo.common.koin.buildModules
import io.streamlayer.demo.player.ExoVideoPlayerProvider
import io.streamlayer.demo.utils.SdkFileLogger
import io.streamlayer.sdk.StreamLayer
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.Koin
import org.koin.core.context.startKoin

class App : Application() {

    private lateinit var koin: Koin

    override fun onCreate() {
        super.onCreate()
        koin = startKoin {
            androidLogger()
            androidContext(this@App)
            modules(buildModules())
        }.koin
        NotificationChannelsHelper.initChannels(this)
        Branch.getAutoInstance(this)
        StreamLayer.initializeApp(this, BuildConfig.SL_SDK_KEY)
        StreamLayer.setLogListener(SdkFileLogger(this))
        if (!BuildConfig.DEBUG) StreamLayer.setLogcatLoggingEnabled(false)
        StreamLayer.setVideoPlayerProvider(ExoVideoPlayerProvider(this))
    }
}