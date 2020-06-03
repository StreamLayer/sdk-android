package io.streamlayer.demo.common.mvvm

data class ResourceState<out T>(val status: Status, val data: T?, val error: BaseError?) {
    companion object {
        fun <T> loading(data: T? = null): ResourceState<T> {
            return ResourceState(Status.LOADING, data, null)
        }

        fun <T> success(data: T?): ResourceState<T> {
            return ResourceState(Status.SUCCESS, data, null)
        }

        fun <T> error(
            baseError: BaseError,
            data: T? = null
        ): ResourceState<T> {
            return ResourceState(Status.ERROR, data, baseError)
        }
    }
}