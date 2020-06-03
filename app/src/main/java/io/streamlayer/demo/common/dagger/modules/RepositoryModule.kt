package io.streamlayer.demo.common.dagger.modules

import dagger.Module
import dagger.Provides
import io.streamlayer.demo.repository.DemoStreamsRepository
import javax.inject.Singleton

@Module
class RepositoryModule {

    @Provides
    @Singleton
    fun providesDemoStreamsRepo(): DemoStreamsRepository {
        return DemoStreamsRepository()
    }
}