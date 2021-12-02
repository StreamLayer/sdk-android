package io.streamlayer.demo.common.mvvm

enum class Status {
    SUCCESS,
    ERROR,
    LOADING
}

enum class ResponseCodes(val code: Int) {
    UNDEFINED(0),
    UNAUTHORIZED(401)
}

data class BaseError(
    val errorMessage: String = "Some error occurred.\nPlease try later.",
    val errorCode: Int = ResponseCodes.UNDEFINED.code
)

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