package io.streamlayer.demotv
import android.app.Application
import io.streamlayer.sdk.SLRTheme
import io.streamlayer.sdk.StreamLayer
import io.streamlayer.sdk.exoplayer.StreamLayerExoPlayer
//import io.streamlayer.sdk.media3.StreamLayerMedia3Player
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class App : Application() {

    override fun onCreate() {
        super.onCreate()
        StreamLayer.initializeApp(this, BuildConfig.SL_SDK_KEY)
        StreamLayer.setGamificationOptions(
            StreamLayer.GameOptions(
                isGlobalLeaderboardEnabled = false,
                isInvitesEnabled = false,
                isOnboardingEnabled = false,
                showGamificationNotificationOnboarding = false
            )
        )
        StreamLayer.setCustomTheme(
            SLRTheme(
                mainTheme = R.style.TVMainOverlayTheme,
                baseTheme = R.style.TVMainOverlayTheme
            )
        )
        StreamLayer.setInvitesEnabled(false)
        StreamLayerExoPlayer.initSdk(this)
//        StreamLayerMedia3Player.initSdk(this) // in case media3
        // If you need anonymous authorization
        if (!StreamLayer.isUserAuthorized()) {
            CoroutineScope(Dispatchers.IO).launch {
                runCatching {
                    StreamLayer.useAnonymousAuth()
                }
            }
        }
    }
}