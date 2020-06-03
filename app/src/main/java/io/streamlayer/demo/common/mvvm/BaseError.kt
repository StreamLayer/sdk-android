package io.streamlayer.demo.common.mvvm

data class BaseError(
    val errorMessage: String = "Some error occurred.\nPlease try later.",
    val errorCode: Int = ResponseCodes.UNDEFINED.code
)