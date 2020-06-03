package io.streamlayer.demo.common.dagger.modules

import android.content.Context
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module(includes = [ContextModule::class])
class ExoPlayerModule {

    @Provides
    fun providesExoPlayer(context: Context): ExoPlayer {
        return SimpleExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_ONE
            playWhenReady = true
            setForegroundMode(true)
        }
    }

    @Provides
    @Singleton
    fun provideProgressiveMediaSourceFactory(): ProgressiveMediaSource.Factory {
        return ProgressiveMediaSource.Factory(
            DefaultHttpDataSourceFactory("streamlayer-demo")
        )
    }

    @Provides
    @Singleton
    fun provideHlsMediaSourceFactory(): HlsMediaSource.Factory {
        return HlsMediaSource.Factory(
            DefaultHttpDataSourceFactory("streamlayer-demo")
        )
    }

}