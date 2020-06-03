package io.streamlayer.demo.main

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.distinctUntilChanged
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.streamlayer.demo.common.mvvm.BaseError
import io.streamlayer.demo.common.mvvm.ResourceState
import io.streamlayer.demo.repository.DemoStreamsRepository
import io.streamlayer.demo.utils.getStreamUrl
import io.streamlayer.sdk.StreamLayerDemo
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MainViewModel @Inject constructor(
    demoStreamsRepository: DemoStreamsRepository
) : ViewModel() {

    private val disposables: CompositeDisposable = CompositeDisposable()

    private val _demoStreams: MutableLiveData<ResourceState<List<StreamLayerDemo.Item>>> =
        MutableLiveData()
    val demoStreams: LiveData<ResourceState<List<StreamLayerDemo.Item>>> = _demoStreams

    private val _selectedStream: MutableLiveData<StreamLayerDemo.Item> = MutableLiveData()
    val selectedStream: LiveData<StreamLayerDemo.Item> = _selectedStream.distinctUntilChanged()

    init {
        disposables.add(
            demoStreamsRepository.getDemoStreams()
                .flatMapObservable { Observable.fromIterable(it) }
                .concatMapEager { item ->
                    getStreamUrl(item.streamId).map { item.copy(streamUrl = it) }.toObservable()
                }
                .toList()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe {
                    _demoStreams.postValue(ResourceState.loading())
                }
                .doOnSuccess {
                    _selectedStream.value = it.first()
                    _demoStreams.postValue(ResourceState.success(it))
                }
                .subscribe({
                    _demoStreams.postValue(ResourceState.success(it))
                }, {
                    Log.e("MainViewModel", "getDemoStreams", it)
                    _demoStreams.postValue(ResourceState.error(it.message?.let { BaseError(it) }
                        ?: BaseError()))
                })
        )
    }


    fun selectStream(stream: StreamLayerDemo.Item) {
        _selectedStream.postValue(stream)
    }

    override fun onCleared() {
        disposables.clear()
        super.onCleared()
    }

}