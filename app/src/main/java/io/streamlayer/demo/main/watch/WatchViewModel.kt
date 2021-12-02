package io.streamlayer.demo.main.watch

import androidx.lifecycle.viewModelScope
import io.streamlayer.demo.common.dispatcher.CoroutineDispatcherProvider
import io.streamlayer.demo.common.mvvm.MviViewModel
import io.streamlayer.demo.repository.StreamsRepository
import io.streamlayer.demo.repository.Stream
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

enum class Type {
    FEATURED, ORIGINALS
}

data class Watch(
    val main: List<Stream> = emptyList(),
    val more: List<Stream> = emptyList(),
    val recommended: List<Stream> = emptyList(),
)

data class State(
    val type: Type = Type.FEATURED,
    val watches: List<Watch>? = null
)

class WatchViewModel(
    private val streamsRepository: StreamsRepository,
    coroutineDispatcherProvider: CoroutineDispatcherProvider
) : MviViewModel<State>(State(), coroutineDispatcherProvider) {

    val type = stateSlice { type }
    val watches = stateSlice { watches }.filterNotNull()

    init {
        subToStreams()
    }

    private fun subToStreams() {
        viewModelScope.launch {
            streamsRepository.getStreams().collect {
                it.consume { list ->
                    updateState {
                        val featured =
                            Watch(main = list.shuffled(), more = list.shuffled(), recommended = list.shuffled())
                        val originals =
                            Watch(main = list.shuffled(), more = list.shuffled(), recommended = list.shuffled())
                        copy(watches = listOf(featured, originals))
                    }
                }
            }
        }
    }

    fun selectType(type: Type) {
        if (currentState.type != type) updateState(true) { copy(type = type) }
    }
}