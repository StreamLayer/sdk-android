package io.streamlayer.demo

import android.app.Application
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasAndroidInjector
import io.streamlayer.demo.common.dagger.components.ApplicationComponent
import io.streamlayer.demo.common.dagger.components.DaggerApplicationComponent
import io.streamlayer.demo.common.dagger.modules.ContextModule
import io.streamlayer.demo.common.firebase.NotificationChannelsHelper
import io.streamlayer.sdk.StreamLayer
import javax.inject.Inject

class App : Application(), HasAndroidInjector {

    @Inject
    lateinit var dispatchingAndroidInjector: DispatchingAndroidInjector<Any>

    override fun androidInjector(): AndroidInjector<Any> = dispatchingAndroidInjector

    lateinit var component: ApplicationComponent
        private set

    override fun onCreate() {
        super.onCreate()

        DaggerApplicationComponent.builder()
            .contextModule(ContextModule(this))
            .build()
            .also { component = it }
            .inject(this)

        NotificationChannelsHelper.initChannels(this)

        StreamLayer.initializeApp(this, BuildConfig.SL_SDK_KEY)
    }
}