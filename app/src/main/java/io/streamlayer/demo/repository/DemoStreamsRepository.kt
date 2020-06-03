package io.streamlayer.demo.repository

import io.reactivex.Single
import io.streamlayer.sdk.StreamLayerDemo

class DemoStreamsRepository {

    fun getDemoStreams(): Single<List<StreamLayerDemo.Item>> = Single.create {
        StreamLayerDemo.getDemoStreams(it::onSuccess, it::onError)
    }

}