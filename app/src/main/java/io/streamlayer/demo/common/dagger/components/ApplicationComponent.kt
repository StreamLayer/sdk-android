package io.streamlayer.demo.common.dagger.components

import android.content.Context
import dagger.Component
import dagger.android.AndroidInjectionModule
import dagger.android.AndroidInjector
import io.streamlayer.demo.App
import io.streamlayer.demo.common.dagger.modules.*
import javax.inject.Singleton

@Singleton
@Component(
    modules = [
        (AndroidInjectionModule::class),
        (BindingModuleActivities::class),
        (RepositoryModule::class),
        (SchedulersModule::class),
        (ViewModelModule::class),
        (ExoPlayerModule::class)
    ]
)
interface ApplicationComponent : AndroidInjector<App> {

    val context: Context

}