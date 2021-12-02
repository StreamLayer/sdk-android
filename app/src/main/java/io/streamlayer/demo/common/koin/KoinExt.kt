package io.streamlayer.demo.common.koin

import android.content.ComponentCallbacks
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStoreOwner
import org.koin.android.ext.android.getDefaultScope
import org.koin.androidx.viewmodel.ViewModelOwner
import org.koin.androidx.viewmodel.koin.getViewModel
import org.koin.androidx.viewmodel.scope.getViewModel
import org.koin.core.context.GlobalContext
import org.koin.core.parameter.ParametersDefinition
import org.koin.core.qualifier.Qualifier
import kotlin.reflect.KClass

/**
 * ViewModelStoreOwner extensions to help for ViewModel
 */
inline fun <reified T : ViewModel> ViewModelStoreOwner.injectViewModel(
    qualifier: Qualifier? = null,
    noinline owner: () -> ViewModelStoreOwner = { this },
    mode: LazyThreadSafetyMode = LazyThreadSafetyMode.SYNCHRONIZED,
    noinline parameters: ParametersDefinition? = null,
): Lazy<T> {
    return lazy(mode) { getViewModel<T>(qualifier, owner(), parameters) }
}

fun <T : ViewModel> ViewModelStoreOwner.injectViewModel(
    qualifier: Qualifier? = null,
    owner: () -> ViewModelStoreOwner = { this },
    clazz: KClass<T>,
    mode: LazyThreadSafetyMode = LazyThreadSafetyMode.SYNCHRONIZED,
    parameters: ParametersDefinition? = null,
): Lazy<T> {
    return lazy(mode) { getViewModel(qualifier, owner(), clazz, parameters) }
}

inline fun <reified T : ViewModel> ViewModelStoreOwner.getViewModel(
    qualifier: Qualifier? = null,
    owner: ViewModelStoreOwner,
    noinline parameters: ParametersDefinition? = null,
): T {
    return getViewModel(qualifier, owner, T::class, parameters)
}

fun <T : ViewModel> ViewModelStoreOwner.getViewModel(
    qualifier: Qualifier? = null,
    owner: ViewModelStoreOwner,
    clazz: KClass<T>,
    parameters: ParametersDefinition? = null,
): T {
    return when (this) {
        is ComponentCallbacks -> {
            getDefaultScope().getViewModel(qualifier, null, { ViewModelOwner.from(owner) }, clazz, parameters)
        }
        else -> {
            GlobalContext.get().getViewModel(qualifier, null, { ViewModelOwner.from(owner) }, clazz, parameters)
        }
    }
}