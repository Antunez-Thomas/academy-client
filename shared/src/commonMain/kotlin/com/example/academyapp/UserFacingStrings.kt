package com.example.academyapp

import androidx.compose.runtime.Composable
import com.example.academyapp.api.AcademyApiError
import com.example.academyapp.generated.resources.Res
import com.example.academyapp.generated.resources.error_bad_request
import com.example.academyapp.generated.resources.error_conflict
import com.example.academyapp.generated.resources.error_expired_request
import com.example.academyapp.generated.resources.error_forbidden
import com.example.academyapp.generated.resources.error_invalid_response
import com.example.academyapp.generated.resources.error_missing_access_token
import com.example.academyapp.generated.resources.error_no_connection
import com.example.academyapp.generated.resources.error_not_found
import com.example.academyapp.generated.resources.error_rate_limited
import com.example.academyapp.generated.resources.error_secure_storage
import com.example.academyapp.generated.resources.error_server_unavailable
import com.example.academyapp.generated.resources.error_timeout
import com.example.academyapp.generated.resources.error_unauthorized
import com.example.academyapp.generated.resources.error_unexpected_failure
import com.example.academyapp.generated.resources.error_unexpected_server
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource

fun interface UserFacingStringResolver {
    suspend fun resolve(resource: StringResource): String
}

internal val defaultUserFacingStringResolver = UserFacingStringResolver { resource ->
    getString(resource)
}

internal fun AcademyApiError.messageResource(): StringResource = when (this) {
    is AcademyApiError.MissingAccessToken -> Res.string.error_missing_access_token
    is AcademyApiError.SessionStorage -> Res.string.error_secure_storage
    is AcademyApiError.Timeout -> Res.string.error_timeout
    is AcademyApiError.NoConnection -> Res.string.error_no_connection
    is AcademyApiError.InvalidResponse -> Res.string.error_invalid_response
    is AcademyApiError.UnexpectedFailure -> Res.string.error_unexpected_failure
    is AcademyApiError.BadRequest -> Res.string.error_bad_request
    is AcademyApiError.Unauthorized -> Res.string.error_unauthorized
    is AcademyApiError.Forbidden -> Res.string.error_forbidden
    is AcademyApiError.NotFound -> Res.string.error_not_found
    is AcademyApiError.Gone -> Res.string.error_expired_request
    is AcademyApiError.Conflict -> Res.string.error_conflict
    is AcademyApiError.RateLimited -> Res.string.error_rate_limited
    is AcademyApiError.Server -> Res.string.error_server_unavailable
    is AcademyApiError.UnexpectedHttpStatus -> Res.string.error_unexpected_server
}

@Composable
internal fun AcademyApiError.localizedUserMessage(): String =
    stringResource(messageResource())

internal suspend fun AcademyApiError.localizedUserMessageForState(
    resolver: UserFacingStringResolver = defaultUserFacingStringResolver,
): String = resolver.resolve(messageResource())
