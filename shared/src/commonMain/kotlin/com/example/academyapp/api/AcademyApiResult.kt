package com.example.academyapp.api

import com.example.academyapp.api.model.ApiError

sealed interface ApiResult<out T> {
    data class Success<T>(val value: T) : ApiResult<T>
    data class Failure(val error: AcademyApiError) : ApiResult<Nothing>
}

sealed interface AcademyApiError {
    data class MissingAccessToken(val message: String = "An access token is required.") : AcademyApiError
    data class SessionStorage(val cause: Throwable) : AcademyApiError
    data class Timeout(val cause: Throwable) : AcademyApiError
    data class NoConnection(val cause: Throwable) : AcademyApiError
    data class InvalidResponse(val cause: Throwable) : AcademyApiError
    data class UnexpectedFailure(val cause: Throwable) : AcademyApiError
    data class BadRequest(val details: ApiError?) : AcademyApiError
    data class Unauthorized(val details: ApiError?) : AcademyApiError
    data class Forbidden(val details: ApiError?) : AcademyApiError
    data class NotFound(val details: ApiError?) : AcademyApiError
    data class Gone(val details: ApiError?) : AcademyApiError
    data class Conflict(val details: ApiError?) : AcademyApiError
    data class RateLimited(val details: ApiError?) : AcademyApiError
    data class Server(val status: Int, val details: ApiError?) : AcademyApiError
    data class UnexpectedHttpStatus(val status: Int, val details: ApiError?) : AcademyApiError
}
