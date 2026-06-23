package com.example.mediabrowser.data.remote

sealed class MediaApiException(message: String, cause: Throwable? = null) : Exception(message, cause) {

    class NoConnectivity(cause: Throwable? = null) :
        MediaApiException("No internet connection.", cause)

    class Timeout(cause: Throwable? = null) :
        MediaApiException("The request timed out.", cause)

    class ServerError(val code: Int, cause: Throwable? = null) :
        MediaApiException("Server returned an error (code $code).", cause)

    class Unknown(cause: Throwable? = null) :
        MediaApiException("An unexpected error occurred.", cause)
}
