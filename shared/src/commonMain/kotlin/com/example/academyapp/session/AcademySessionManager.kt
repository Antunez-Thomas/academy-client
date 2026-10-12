package com.example.academyapp.session

import com.example.academyapp.api.ApiResult
import com.example.academyapp.api.AcademyApi
import com.example.academyapp.api.AcademyApiError
import com.example.academyapp.api.model.LoginResponse
import com.example.academyapp.api.model.RegisterRequest
import com.example.academyapp.api.model.User
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.cancellation.CancellationException

sealed interface SessionState {
    data object Initializing : SessionState
    data object SignedOut : SessionState
    data object SigningIn : SessionState
    data class SignedIn(val user: User) : SessionState
    data class RecoverableError(val error: AcademyApiError) : SessionState
    data object Expired : SessionState
}

class AcademySessionManager(
    private val api: AcademyApi,
    private val storage: SessionTokenStorage,
    private val tokenProvider: MutableAccessTokenProvider,
) {
    private val lifecycleMutex = Mutex()
    private val refreshMutex = Mutex()
    private var tokens: SessionTokens? = null
    private var refreshInFlight: CompletableDeferred<RefreshOutcome>? = null
    private val mutableState = MutableStateFlow<SessionState>(SessionState.Initializing)

    val state: StateFlow<SessionState> = mutableState.asStateFlow()

    suspend fun initialize(): SessionState {
        val ready = lifecycleMutex.withLock {
            mutableState.value = SessionState.Initializing
            val restored = try {
                storage.load()
            } catch (cause: Throwable) {
                clearStorageAfterFailure()
                mutableState.value = SessionState.RecoverableError(AcademyApiError.SessionStorage(cause))
                return@withLock false
            }
            if (restored == null) {
                clearMemory()
                mutableState.value = SessionState.SignedOut
                return@withLock false
            }
            install(restored)
            true
        }
        if (!ready) return mutableState.value

        val result = executeAuthenticated { api.getMe() }
        return lifecycleMutex.withLock {
            when {
                mutableState.value is SessionState.Expired ||
                    mutableState.value is SessionState.SignedOut -> mutableState.value
                result is ApiResult.Success -> SessionState.SignedIn(result.value).also {
                    mutableState.value = it
                }
                result is ApiResult.Failure -> SessionState.RecoverableError(result.error).also {
                    mutableState.value = it
                }
                else -> mutableState.value
            }
        }
    }

    suspend fun login(email: String, password: String): ApiResult<LoginResponse> = lifecycleMutex.withLock {
        mutableState.value = SessionState.SigningIn
        installLoginResult(api.login(email, password))
    }

    suspend fun register(
        email: String,
        password: String,
        acceptLegalAgreement: Boolean,
        displayName: String? = null,
    ): ApiResult<LoginResponse> = lifecycleMutex.withLock {
        mutableState.value = SessionState.SigningIn
        installLoginResult(api.register(RegisterRequest(email, password, acceptLegalAgreement, displayName)))
    }

    private suspend fun installLoginResult(result: ApiResult<LoginResponse>): ApiResult<LoginResponse> {
        return when (result) {
            is ApiResult.Success -> {
                val newTokens = result.value.toSessionTokens()
                try {
                    storage.save(newTokens)
                    install(newTokens)
                    mutableState.value = SessionState.SignedIn(result.value.user)
                    result
                } catch (cause: Throwable) {
                    clearAfterStorageFailure(cause)
                }
            }
            is ApiResult.Failure -> {
                mutableState.value = SessionState.SignedOut
                result
            }
        }
    }

    suspend fun logout(): ApiResult<*> = lifecycleMutex.withLock {
        val remoteResult = api.logout()
        val storageError = try {
            storage.clear()
            null
        } catch (cause: Throwable) {
            cause
        }
        clearMemory()
        if (storageError == null) {
            mutableState.value = SessionState.SignedOut
            remoteResult
        } else {
            storageFailureResult(storageError)
        }
    }

    suspend fun <T> executeAuthenticated(request: suspend () -> ApiResult<T>): ApiResult<T> {
        val failedToken = tokenProvider.accessToken()
            ?: return ApiResult.Failure(AcademyApiError.MissingAccessToken())
        val first = request()
        if (first !is ApiResult.Failure || first.error !is AcademyApiError.Unauthorized) return first

        return when (val refresh = refreshSingleFlight(failedToken)) {
            RefreshOutcome.Success -> {
                val retried = request()
                if (retried is ApiResult.Failure && retried.error is AcademyApiError.Unauthorized) {
                    tokenProvider.accessToken()?.let { currentToken ->
                        expire(currentToken, retried.error)
                    }
                }
                retried
            }
            is RefreshOutcome.Failure -> ApiResult.Failure(refresh.error)
        }
    }

    private suspend fun refreshSingleFlight(failedToken: String): RefreshOutcome {
        var owner = false
        val deferred = refreshMutex.withLock {
            if (tokenProvider.accessToken() != failedToken) {
                return RefreshOutcome.Success
            }
            refreshInFlight ?: CompletableDeferred<RefreshOutcome>().also {
                refreshInFlight = it
                owner = true
            }
        }
        if (!owner) return deferred.await()

        try {
            val outcome = performRefresh(failedToken)
            deferred.complete(outcome)
            return outcome
        } catch (cause: CancellationException) {
            deferred.completeExceptionally(cause)
            throw cause
        } catch (cause: Throwable) {
            val error = AcademyApiError.SessionStorage(cause)
            lifecycleMutex.withLock {
                if (tokenProvider.accessToken() == failedToken) {
                    mutableState.value = SessionState.RecoverableError(error)
                }
            }
            val outcome = RefreshOutcome.Failure(error)
            deferred.complete(outcome)
            return outcome
        } finally {
            refreshMutex.withLock {
                if (refreshInFlight === deferred) refreshInFlight = null
            }
        }
    }

    private suspend fun performRefresh(failedToken: String): RefreshOutcome {
        val refreshToken = lifecycleMutex.withLock {
            if (tokenProvider.accessToken() != failedToken) return RefreshOutcome.Success
            tokens?.refreshToken
        } ?: return expire(failedToken, AcademyApiError.MissingAccessToken("A refresh token is required."))

        return when (val result = api.refresh(refreshToken)) {
            is ApiResult.Success -> lifecycleMutex.withLock {
                if (tokenProvider.accessToken() != failedToken) return@withLock RefreshOutcome.Success
                val replacement = result.value.toSessionTokens()
                try {
                    // Rotation is durable before waiting requests receive the replacement access token.
                    storage.save(replacement)
                    install(replacement)
                    mutableState.value = SessionState.SignedIn(result.value.user)
                    RefreshOutcome.Success
                } catch (cause: Throwable) {
                    expireLocked(AcademyApiError.SessionStorage(cause))
                }
            }
            is ApiResult.Failure -> when (result.error) {
                is AcademyApiError.Unauthorized,
                is AcademyApiError.Forbidden,
                is AcademyApiError.MissingAccessToken,
                -> expire(failedToken, result.error)
                else -> lifecycleMutex.withLock {
                    if (tokenProvider.accessToken() == failedToken) {
                        mutableState.value = SessionState.RecoverableError(result.error)
                    }
                    RefreshOutcome.Failure(result.error)
                }
            }
        }
    }

    private suspend fun expire(
        failedToken: String,
        error: AcademyApiError,
    ): RefreshOutcome = lifecycleMutex.withLock {
        if (tokenProvider.accessToken() != failedToken) return@withLock RefreshOutcome.Success
        expireLocked(error)
    }

    private suspend fun expireLocked(error: AcademyApiError): RefreshOutcome {
        clearStorageAfterFailure()
        mutableState.value = SessionState.Expired
        return RefreshOutcome.Failure(error)
    }

    private suspend fun clearStorageAfterFailure() {
        try {
            storage.clear()
        } catch (_: Throwable) {
            // Memory is still cleared so invalid credentials are never reused in this process.
        }
        clearMemory()
    }

    private suspend fun install(newTokens: SessionTokens) {
        tokens = newTokens
        tokenProvider.update(newTokens.accessToken)
    }

    private suspend fun clearMemory() {
        tokens = null
        tokenProvider.update(null)
    }

    private suspend fun clearAfterStorageFailure(cause: Throwable): ApiResult<LoginResponse> {
        clearStorageAfterFailure()
        return storageFailureResult(cause)
    }

    private fun <T> storageFailureResult(cause: Throwable): ApiResult<T> {
        val error = AcademyApiError.SessionStorage(cause)
        mutableState.value = SessionState.RecoverableError(error)
        return ApiResult.Failure(error)
    }

    private fun LoginResponse.toSessionTokens() = SessionTokens(
        accessToken = accessToken,
        refreshToken = refreshToken,
        accessTokenExpiresAt = accessTokenExpiresAt,
    )

    private sealed interface RefreshOutcome {
        data object Success : RefreshOutcome
        data class Failure(val error: AcademyApiError) : RefreshOutcome
    }
}
