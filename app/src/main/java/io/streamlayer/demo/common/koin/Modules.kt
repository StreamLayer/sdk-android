package io.streamlayer.demo.common.koin

import android.content.Context
import com.google.android.exoplayer2.database.ExoDatabaseProvider
import com.google.android.exoplayer2.upstream.cache.LeastRecentlyUsedCacheEvictor
import com.google.android.exoplayer2.upstream.cache.SimpleCache
import io.streamlayer.demo.common.dispatcher.CoroutineDispatcherProvider
import io.streamlayer.demo.common.dispatcher.CoroutineDispatcherProviderImpl
import io.streamlayer.demo.common.network.NetworkConnectionUseCase
import io.streamlayer.demo.main.brand.BrandViewModel
import io.streamlayer.demo.main.watch.WatchViewModel
import io.streamlayer.demo.player.PlayerViewModel
import io.streamlayer.demo.repository.StreamsRepository
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.module.Module
import org.koin.dsl.module

internal fun buildModules(): List<Module> = listOf(
    exoPlayerModule(),
    dispatchersModule(),
    repoModule(),
    viewModelsModule(),
    useCasesModule()
)

private fun exoPlayerModule() = module {
    single {
        val context = get<Context>()
        SimpleCache(
            context.cacheDir,
            LeastRecentlyUsedCacheEvictor(100L * 1024 * 1024),
            ExoDatabaseProvider(context)
        )
    }
}

private fun dispatchersModule() = module {
    single<CoroutineDispatcherProvider> { CoroutineDispatcherProviderImpl() }
}

private fun repoModule() = module {
    single { StreamsRepository(get()) }
}

private fun viewModelsModule() = module {
    viewModel { PlayerViewModel(get(), get(), get(), get(), get()) }
    viewModel { BrandViewModel(get(), get()) }
    viewModel { WatchViewModel(get(), get()) }
}

private fun useCasesModule() = module {
    factory { NetworkConnectionUseCase(get(), get()) }
}