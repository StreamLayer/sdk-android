package io.streamlayer.demo.main.brand

import androidx.lifecycle.viewModelScope
import io.streamlayer.demo.common.dispatcher.CoroutineDispatcherProvider
import io.streamlayer.demo.common.mvvm.MviViewModel
import io.streamlayer.demo.repository.Stream
import io.streamlayer.demo.repository.StreamsRepository
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

enum class Type {
    STREAM, ARTICLES
}

data class Brand(
    val now: List<Stream> = emptyList(),
    val recommended: List<Stream> = emptyList(),
)

data class State(
    val type: Type = Type.STREAM,
    val brands: List<Brand>? = null
)

class BrandViewModel(
    private val streamsRepository: StreamsRepository,
    coroutineDispatcherProvider: CoroutineDispatcherProvider
) : MviViewModel<State>(State(), coroutineDispatcherProvider) {

    val type = stateSlice { type }
    val brands = stateSlice { brands }.filterNotNull()

    init {
        subToStreams()
    }

    private fun subToStreams() {
        viewModelScope.launch {
            streamsRepository.getStreams().collect {
                it.consume { list ->
                    updateState {
                        val stream = Brand(now = list.shuffled(), recommended = list.shuffled())
                        val articles = Brand(now = list.shuffled(), recommended = list.shuffled())
                        copy(brands = listOf(stream, articles))
                    }
                }
            }
        }
    }

    fun selectType(type: Type) {
        if (currentState.type != type) updateState(true) { copy(type = type) }
    }
}