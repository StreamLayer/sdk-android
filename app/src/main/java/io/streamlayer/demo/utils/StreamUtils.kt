package io.streamlayer.demo.utils

import io.reactivex.Single
import io.reactivex.subjects.SingleSubject
import vimeoextractor.OnVimeoExtractionListener
import vimeoextractor.VimeoExtractor
import vimeoextractor.VimeoVideo

private const val DEMO_STREAM = "https://rt-uk.secure.footprint.net/1106_1600Kb.m3u8"

fun getStreamUrl(streamId: String): Single<String> {
    val id = mapToVimeo(streamId)
    return if (id.matches(Regex("[0-9]+"))) {
        getVimeoUrl(id)
    } else {
        Single.just(DEMO_STREAM)
    }
}

private fun getVimeoUrl(id: String): Single<String> {
    val result = SingleSubject.create<String>()
    VimeoExtractor.getInstance()
        .fetchVideoWithIdentifier(id, null, object : OnVimeoExtractionListener {
            override fun onSuccess(video: VimeoVideo) {
                val hdStream = video.streams["1080p"] ?: video.streams["720p"] ?: DEMO_STREAM
                result.onSuccess(hdStream)
            }

            override fun onFailure(throwable: Throwable) {
                result.onError(throwable)
            }
        })
    return result
}

/**
 * TODO replace with proper YouTube implementation
 */
private fun mapToVimeo(streamId: String): String {
    return when (streamId) {
        "LW5pdbepoIY" -> "418725583"
        else -> streamId
    }
}