package io.streamlayer.demo.common.ext

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.*
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.EmptyCoroutineContext

/***
 * Common interface of view event
 */
interface ViewEvent

/**
 * Pure function that returns a slice K of the state object S.
 */
typealias Selector<S, K> = S.() -> K

/***
 * Base view events
 */

data class ShowLoading(val isLoading: Boolean) : ViewEvent
data class BaseErrorEvent(val error: String) : ViewEvent

abstract class MviViewModel<State : Any>(
    defaultState: State,
    private val dispatcherProvider: CoroutineDispatcher
) : ViewModel() {

    private val mutex = Mutex()
    protected val _state = MutableStateFlow(defaultState)
    protected val _viewEvents = Channel<ViewEvent>(Channel.UNLIMITED)
    protected var stateLogger: ((state: State) -> Unit)? = null
    val viewEvents = _viewEvents.receiveAsFlow()
    val currentState: State
        get() = _state.value

    // if immediate true it will run on Dispatchers.Main.immediate
    @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
    fun updateState(immediate: Boolean = false, reduce: State.() -> State) {
        viewModelScope.launch(if (immediate) EmptyCoroutineContext else dispatcherProvider) {
            mutex.withLock {
                _state.value = reduce(_state.value).apply { stateLogger?.invoke(this) }
            }
        }
    }

    //If state is simple one and you do not need use state slice, use asStateFlow() for the _state
    protected fun <Slice> stateSlice(selector: Selector<State, Slice>): Flow<Slice> =
        _state.map { it.selector() }.distinctUntilChanged()
}

fun <T> Flow<T>.collectWhenStarted(lifecycleOwner: LifecycleOwner, block: suspend (T) -> Unit) {
    with(lifecycleOwner) {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) { collect { block(it) } }
        }
    }
}

fun <T> Flow<T>.collectLatestWhenStarted(
    lifecycleOwner: LifecycleOwner,
    block: suspend (T) -> Unit
) {
    with(lifecycleOwner) {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) { collectLatest { block(it) } }
        }
    }
}

fun <T> Flow<T>.collectWhenResumed(lifecycleOwner: LifecycleOwner, block: suspend (T) -> Unit) {
    with(lifecycleOwner) {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) { collect { block(it) } }
        }
    }
}
