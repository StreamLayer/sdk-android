package io.streamlayer.demo.common.dagger.modules

import android.content.Context
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.database.ExoDatabaseProvider
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory
import com.google.android.exoplayer2.upstream.cache.LeastRecentlyUsedCacheEvictor
import com.google.android.exoplayer2.upstream.cache.SimpleCache
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module(includes = [ContextModule::class])
class ExoPlayerModule {

    @Provides
    @Singleton
    fun provideCache(context: Context): SimpleCache = SimpleCache(
        context.cacheDir,
        LeastRecentlyUsedCacheEvictor(100 * 1024 * 1024),
        ExoDatabaseProvider(context)
    )
}