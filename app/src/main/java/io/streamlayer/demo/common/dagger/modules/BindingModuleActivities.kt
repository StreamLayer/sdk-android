package io.streamlayer.demo.common.dagger.modules

import dagger.Module
import dagger.android.ContributesAndroidInjector
import io.streamlayer.demo.common.mvvm.BaseActivity
import io.streamlayer.demo.player.PlayerActivity

@Module
abstract class BindingModuleActivities {

    @ContributesAndroidInjector
    abstract fun contributeBaseActivity(): BaseActivity

    @ContributesAndroidInjector
    abstract fun contributeMainActivity(): PlayerActivity
}