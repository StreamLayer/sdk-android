package io.streamlayer.demo.twitter

import android.app.Application
import io.streamlayer.sdk.StreamLayer
import io.streamlayer.demo.common.exo.ExoVideoPlayerProvider

class App : Application() {

    companion object {
        var instance: Application? = null
        const val SL_SDK_KEY = "SL_SDK_KEY"
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        // initialize sdk with your key
        StreamLayer.initializeApp(this, SL_SDK_KEY)
        // set video player provider
        StreamLayer.setVideoPlayerProvider(ExoVideoPlayerProvider(this))
    }
}
