package io.streamlayer.demo.common.dagger.modules

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap
import io.streamlayer.demo.common.dagger.qualifiers.ViewModelKey
import io.streamlayer.demo.common.mvvm.ViewModelFactory
import io.streamlayer.demo.main.MainViewModel

@Suppress("unused")
@Module
abstract class ViewModelModule {

    @Binds
    abstract fun bindViewModelFactory(factory: ViewModelFactory): ViewModelProvider.Factory

    @Binds
    @IntoMap
    @ViewModelKey(MainViewModel::class)
    abstract fun bindCurrentMainViewModel(mainViewModel: MainViewModel): ViewModel
}