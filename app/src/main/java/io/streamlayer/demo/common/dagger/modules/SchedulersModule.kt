package io.streamlayer.demo.common.dagger.modules

import dagger.Binds
import dagger.Module
import io.streamlayer.demo.common.scheduler.AndroidSchedulerProvider
import io.streamlayer.demo.common.scheduler.SchedulerProvider

@Module
abstract class SchedulersModule {

    @Binds
    abstract fun bindSchedulerProvider(androidSchedulerProvider: AndroidSchedulerProvider): SchedulerProvider

}