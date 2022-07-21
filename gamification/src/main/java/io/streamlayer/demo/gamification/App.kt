package io.streamlayer.demo.gamification

import android.app.Application
import io.streamlayer.sdk.StreamLayer
import io.streamlayer.demo.common.exo.ExoVideoPlayerProvider

class App : Application() {

    companion object {
        var instance: Application? = null
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        // initialize sdk with your key
        StreamLayer.initializeApp(this, BuildConfig.SL_SDK_KEY)
        // set video player provider
        StreamLayer.setVideoPlayerProvider(ExoVideoPlayerProvider(this))
    }
}
