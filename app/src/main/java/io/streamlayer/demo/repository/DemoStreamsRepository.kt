package io.streamlayer.demo.repository

import io.streamlayer.demo.common.mvvm.BaseError
import io.streamlayer.demo.common.mvvm.ResourceState
import io.streamlayer.sdk.base.StreamLayerDemo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import vimeoextractor.OnVimeoExtractionListener
import vimeoextractor.VimeoExtractor
import vimeoextractor.VimeoVideo
import kotlin.coroutines.resumeWithException

class DemoStreamsRepository {

    companion object {
        private const val DEMO_STREAM = "https://rt-uk.secure.footprint.net/1106_1600Kb.m3u8"
    }

    suspend fun getDemoStreams(): ResourceState<List<StreamLayerDemo.Stream>> =
        withContext(Dispatchers.IO) {
            val result =
                kotlin.runCatching { StreamLayerDemo.getDemoStreams("2019-11-26") }
            result.getOrNull()?.let { list ->
                val filteredStreams = list.asFlow()
                    .map { it.copy(streamUrl = kotlin.runCatching { getStreamUrl(it.streamId) }.getOrNull() ?: "") }
                    .filter { it.streamUrl.isNotEmpty() }
                    .toList()
                ResourceState.success(filteredStreams)
            } ?: kotlin.run {
                ResourceState.error(result.exceptionOrNull()?.message?.let { BaseError(it) } ?: BaseError())
            }
        }

    private suspend fun getStreamUrl(streamId: String): String {
        return if (streamId.matches(Regex("[0-9]+"))) getVimeoUrl(streamId)
        else DEMO_STREAM
    }

    private suspend fun getVimeoUrl(id: String): String = suspendCancellableCoroutine { continuation ->
        val callback = object : OnVimeoExtractionListener {
            override fun onSuccess(video: VimeoVideo) {
                val hdStream = video.streams["1080p"] ?: video.streams["720p"] ?: DEMO_STREAM
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