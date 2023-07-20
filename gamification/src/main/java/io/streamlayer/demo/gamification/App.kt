package io.streamlayer.demo.gamification

import android.app.Application
import android.util.Log
import io.streamlayer.sdk.StreamLayer
import io.streamlayer.demo.common.exo.ExoVideoPlayerProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class App : Application() {

    companion object {
        var instance: Application? = null
        const val SL_SDK_KEY = "SL_SDK_KEY"
        const val DEMO_EVENT_ID = "demo"
    }

    val appScope by lazy { CoroutineScope(Dispatchers.Default) }

    override fun onCreate() {
        super.onCreate()
        instance = this
        // initialize sdk with your key
        StreamLayer.initializeApp(this, SL_SDK_KEY)
        // set video player provider
        StreamLayer.setVideoPlayerProvider(ExoVideoPlayerProvider(this))
        // authorize anonymous user if needed
        appScope.launch {
            kotlin.runCatching { StreamLayer.useAnonymousAuth() }
                .onFailure { Log.e("StreamLayer", "anonymous auth failed", it) }
        }

    }
}
