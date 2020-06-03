package io.streamlayer.demo.common.dagger.modules

import dagger.Module
import dagger.android.ContributesAndroidInjector
import io.streamlayer.demo.common.firebase.MyFirebaseMessagingService
import io.streamlayer.demo.common.mvvm.BaseActivity
import io.streamlayer.demo.main.MainActivity

@Module
abstract class BindingModuleActivities {

    @ContributesAndroidInjector
    abstract fun contributeBaseActivity(): BaseActivity

    @ContributesAndroidInjector
    abstract fun contributeMainActivity(): MainActivity

    @ContributesAndroidInjector
    abstract fun contributeMyFirebaseService(): MyFirebaseMessagingService
}