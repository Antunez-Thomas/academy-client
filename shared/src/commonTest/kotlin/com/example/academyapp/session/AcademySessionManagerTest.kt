package com.example.academyapp.session

import com.example.academyapp.api.ApiResult
import com.example.academyapp.api.AcademyApi
import com.example.academyapp.api.AcademyApiError
import com.example.academyapp.api.model.Course
import com.example.academyapp.api.model.CourseContent
import com.example.academyapp.api.model.CourseProgress
import com.example.academyapp.api.model.Entitlement
import com.example.academyapp.api.model.Lesson
import com.example.academyapp.api.model.LoginResponse
import com.example.academyapp.api.model.LogoutResponse
import com.example.academyapp.api.model.PlaybackResponse
import com.example.academyapp.api.model.ProgressRequest
import com.example.academyapp.api.model.ProgressResponse
import com.example.academyapp.api.model.QuizWebLaunch
import com.example.academyapp.api.model.QuizWebLaunchRequest
import com.example.academyapp.api.model.User
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AcademySessionManagerTest {
    private val user = User(42, "learner@example.test", "Learner")
    private val oldTokens = SessionTokens("old-access", "old-refresh", "2026-01-01T00:00:00Z")
    private val replacement = LoginResponse(
        user = user,
        accessToken = "new-access",
        refreshToken = "new-refresh",
        accessTokenExpiresAt = "2026-01-01T01:00:00Z",
    )

    @Test
    fun loginPersistsTokensAndPublishesSignedInState() = runTest {
        val storage = MemoryTokenStorage()
        val provider = MutableAccessTokenProvider()
        val api = FakeAcademyApi().apply {
            loginHandler = { _, _ -> ApiResult.Success(replacement) }
        }
        val manager = AcademySessionManager(api, storage, provider)

        val result = manager.login("learner@example.test", "password")

        assertIs<ApiResult.Success<LoginResponse>>(result)
        assertEquals("new-refresh", storage.tokens?.refreshToken)
        assertEquals("new-access", provider.accessToken())
        assertEquals(SessionState.SignedIn(user), manager.state.value)
    }

    @Test
    fun concurrentUnauthorizedCallsShareOneRefreshAndRetry() = runTest {
        val storage = MemoryTokenStorage(oldTokens)
        val provider = MutableAccessTokenProvider()
        var refreshCount = 0
        val api = FakeAcademyApi().apply {
            meHandler = { ApiResult.Success(user) }
            coursesHandler = {
                if (provider.accessToken() == "old-access") unauthorized()
                else ApiResult.Success(emptyList())
            }
            refreshHandler = {
                refreshCount += 1
                delay(50)
                ApiResult.Success(replacement)
            }
        }
        val manager = AcademySessionManager(api, storage, provider)
        assertIs<SessionState.SignedIn>(manager.initialize())

        val results = List(12) {
            async { manager.executeAuthenticated { api.getCourses() } }
        }.awaitAll()

        assertTrue(results.all { it is ApiResult.Success })
        assertEquals(1, refreshCount)
        assertEquals("new-refresh", storage.tokens?.refreshToken)
        assertEquals("new-access", provider.accessToken())
    }

    @Test
    fun rejectedRefreshExpiresAndDeletesTheSession() = runTest {
        val storage = MemoryTokenStorage(oldTokens)
        val provider = MutableAccessTokenProvider()
        val api = FakeAcademyApi().apply {
            meHandler = { ApiResult.Success(user) }
            coursesHandler = { unauthorized() }
            refreshHandler = { unauthorized() }
        }
        val manager = AcademySessionManager(api, storage, provider)
        manager.initialize()

        val result = manager.executeAuthenticated { api.getCourses() }

        assertIs<AcademyApiError.Unauthorized>(assertIs<ApiResult.Failure>(result).error)
        assertEquals(SessionState.Expired, manager.state.value)
        assertNull(storage.tokens)
        assertNull(provider.accessToken())
    }

    @Test
    fun unauthorizedRetryAfterRefreshExpiresTheReplacementSession() = runTest {
        val storage = MemoryTokenStorage(oldTokens)
        val provider = MutableAccessTokenProvider()
        val api = FakeAcademyApi().apply {
            meHandler = { ApiResult.Success(user) }
            coursesHandler = { unauthorized() }
            refreshHandler = { ApiResult.Success(replacement) }
        }
        val manager = AcademySessionManager(api, storage, provider)
        manager.initialize()

        val result = manager.executeAuthenticated { api.getCourses() }

        assertIs<AcademyApiError.Unauthorized>(assertIs<ApiResult.Failure>(result).error)
        assertEquals(SessionState.Expired, manager.state.value)
        assertNull(storage.tokens)
        assertNull(provider.accessToken())
    }

    @Test
    fun transientRefreshFailureKeepsStoredTokensForRetryLater() = runTest {
        val storage = MemoryTokenStorage(oldTokens)
        val provider = MutableAccessTokenProvider()
        val offline = AcademyApiError.NoConnection(IllegalStateException("offline"))
        val api = FakeAcademyApi().apply {
            meHandler = { ApiResult.Success(user) }
            coursesHandler = { unauthorized() }
            refreshHandler = { ApiResult.Failure(offline) }
        }
        val manager = AcademySessionManager(api, storage, provider)
        manager.initialize()

        val result = manager.executeAuthenticated { api.getCourses() }

        assertEquals(offline, assertIs<ApiResult.Failure>(result).error)
        assertEquals(SessionState.RecoverableError(offline), manager.state.value)
        assertEquals(oldTokens, storage.tokens)
        assertEquals("old-access", provider.accessToken())
    }

    @Test
    fun logoutClearsLocalSessionEvenWhenServerIsUnavailable() = runTest {
        val storage = MemoryTokenStorage(oldTokens)
        val provider = MutableAccessTokenProvider()
        val offline = AcademyApiError.NoConnection(IllegalStateException("offline"))
        val api = FakeAcademyApi().apply {
            meHandler = { ApiResult.Success(user) }
            logoutHandler = { ApiResult.Failure(offline) }
        }
        val manager = AcademySessionManager(api, storage, provider)
        manager.initialize()

        val result = manager.logout()

        assertEquals(offline, assertIs<ApiResult.Failure>(result).error)
        assertEquals(SessionState.SignedOut, manager.state.value)
        assertNull(storage.tokens)
        assertNull(provider.accessToken())
    }

    @Test
    fun logoutDuringRefreshCannotRestoreTheSession() = runTest {
        val storage = MemoryTokenStorage(oldTokens)
        val provider = MutableAccessTokenProvider()
        val refreshStarted = CompletableDeferred<Unit>()
        val releaseRefresh = CompletableDeferred<Unit>()
        val api = FakeAcademyApi().apply {
            meHandler = { ApiResult.Success(user) }
            coursesHandler = {
                if (provider.accessToken() == "new-access") ApiResult.Success(emptyList())
                else unauthorized()
            }
            refreshHandler = {
                refreshStarted.complete(Unit)
                releaseRefresh.await()
                ApiResult.Success(replacement)
            }
        }
        val manager = AcademySessionManager(api, storage, provider)
        manager.initialize()

        val request = async {
            manager.executeAuthenticated { api.getCourses() }
        }
        refreshStarted.await()
        manager.logout()
        releaseRefresh.complete(Unit)
        request.await()

        assertEquals(SessionState.SignedOut, manager.state.value)
        assertNull(storage.tokens)
        assertNull(provider.accessToken())
    }

    @Test
    fun initializeWithoutStoredTokensIsSignedOut() = runTest {
        val manager = AcademySessionManager(
            FakeAcademyApi(),
            MemoryTokenStorage(),
            MutableAccessTokenProvider(),
        )

        assertEquals(SessionState.SignedOut, manager.initialize())
    }

    private fun unauthorized(): ApiResult.Failure =
        ApiResult.Failure(AcademyApiError.Unauthorized(null))

    private class MemoryTokenStorage(
        var tokens: SessionTokens? = null,
    ) : SessionTokenStorage {
        override suspend fun load(): SessionTokens? = tokens
        override suspend fun save(tokens: SessionTokens) {
            this.tokens = tokens
        }
        override suspend fun clear() {
            tokens = null
        }
    }

    private class FakeAcademyApi : AcademyApi {
        var loginHandler: suspend (String, String) -> ApiResult<LoginResponse> = { _, _ -> unsupported() }
        var refreshHandler: suspend (String) -> ApiResult<LoginResponse> = { unsupported() }
        var logoutHandler: suspend () -> ApiResult<LogoutResponse> = { ApiResult.Success(LogoutResponse(true)) }
        var meHandler: suspend () -> ApiResult<User> = { unsupported() }
        var coursesHandler: suspend () -> ApiResult<List<Course>> = { unsupported() }

        override suspend fun login(email: String, password: String) = loginHandler(email, password)
        override suspend fun refresh(refreshToken: String) = refreshHandler(refreshToken)
        override suspend fun logout() = logoutHandler()
        override suspend fun getMe() = meHandler()
        override suspend fun getCourses() = coursesHandler()
        override suspend fun getCourse(id: Long): ApiResult<Course> = unsupported()
        override suspend fun getCourseContent(id: Long): ApiResult<CourseContent> = unsupported()
        override suspend fun getLesson(courseId: Long, lessonId: Long): ApiResult<Lesson> = unsupported()
        override suspend fun saveLessonProgress(
            lessonId: Long,
            request: ProgressRequest,
        ): ApiResult<ProgressResponse> = unsupported()
        override suspend fun getCourseProgress(courseId: Long): ApiResult<CourseProgress> = unsupported()
        override suspend fun authorizePlayback(
            courseId: Long,
            lessonId: Long,
        ): ApiResult<PlaybackResponse> = unsupported()
        override suspend fun createQuizWebLaunch(request: QuizWebLaunchRequest): ApiResult<QuizWebLaunch> = unsupported()
        override suspend fun getEntitlements(): ApiResult<List<Entitlement>> = unsupported()

        private fun <T> unsupported(): ApiResult<T> =
            ApiResult.Failure(AcademyApiError.NoConnection(IllegalStateException("Unexpected fake call")))
    }
}
