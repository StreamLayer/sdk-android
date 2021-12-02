package io.streamlayer.demo.repository

import io.streamlayer.demo.common.dispatcher.CoroutineDispatcherProvider
import io.streamlayer.demo.common.mvvm.BaseError
import io.streamlayer.demo.common.mvvm.ResourceState
import io.streamlayer.demo.common.mvvm.Status
import io.streamlayer.sdk.base.StreamLayerDemo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import vimeoextractor.OnVimeoExtractionListener
import vimeoextractor.VimeoExtractor
import vimeoextractor.VimeoVideo
import kotlin.coroutines.resumeWithException

data class Stream(
    val eventId: Long,
    val eventImageUrl: String?,
    val live: Boolean,
    val title: String,
    val subtitle: String,
    val time: String,
    val streamUrl: String = ""
)

class StreamsRepository(coroutineDispatcherProvider: CoroutineDispatcherProvider) {

    companion object {
        private const val DEMO_STREAM = "https://rt-uk.secure.footprint.net/1106_1600Kb.m3u8"
        private val LEAGUES_TABLE = mapOf(
            "10" to "ESPN+ • SLP",
            "3" to "ESPN+ • EPL",
            "466" to "ESPN+ • NFL",
            "375" to "ESPN+ • NBA",
            "423" to "ESPN+ • MLB"
        )
    }

    private val state: MutableStateFlow<ResourceState<List<Stream>>> = MutableStateFlow(ResourceState.loading())

    private val scope = CoroutineScope(coroutineDispatcherProvider.io)
    private var job: Job? = null

    init {
        refresh()
    }

    fun getStreams(): Flow<ResourceState<List<Stream>>> =
        state.filter { it.status != Status.LOADING }.distinctUntilChanged()

    fun refresh() {
        if (job?.isActive == true || state.value.status == Status.SUCCESS) return // npt make sense to refresh loaded items
        job?.cancel()
        job = scope.launch {
            val result =
                kotlin.runCatching { StreamLayerDemo.getDemoStreams("2019-11-26") }
            result.getOrNull()?.let { list ->
                val filteredStreams = list.asFlow()
                    .map { it.toDomain(kotlin.runCatching { getStreamUrl(it.eventId, it.streamId) }.getOrNull() ?: "") }
                    .filter { it.streamUrl.isNotEmpty() }
                    .toList()
                if (isActive) state.emit(ResourceState.success(filteredStreams))
            } ?: kotlin.run {
                if (isActive) state.emit(ResourceState.error(result.exceptionOrNull()?.message?.let { BaseError(it) }
                    ?: BaseError()))
            }
        }
    }

    fun isPDTStream(url: String) = DEMO_STREAM == url

    private fun StreamLayerDemo.Stream.toDomain(url: String): Stream = Stream(
        eventId = eventId,
        eventImageUrl = eventImageUrl,
        live = live,
        title = "$homeTeam vs $awayTeam",
        subtitle = LEAGUES_TABLE[leagueId].orEmpty(),
        streamUrl = url,
        time = time
    )

    private suspend fun getStreamUrl(eventId: Long, streamId: String): String {
        return when {
            streamId.matches(Regex("[0-9]+")) -> getVimeoUrl(streamId)
            else -> DEMO_STREAM
        }
    }

    private suspend fun getVimeoUrl(id: String): String = suspendCancellableCoroutine { continuation ->
        val callback = object : OnVimeoExtractionListener {
            override fun onSuccess(video: VimeoVideo) {
                val hdStream = video.streams["1080p"] ?: video.streams["720p"]
                ?: video.streams["480p"] ?: video.streams["360p"] ?: DEMO_STREAM
                continuation.resume(hdStream, null)
            }

            override fun onFailure(throwable: Throwable) {
                continuation.resumeWithException(throwable)
            }
        }
        VimeoExtractor.getInstance().fetchVideoWithIdentifier(id, null, callback)
        continuation.invokeOnCancellation { }
    }
}