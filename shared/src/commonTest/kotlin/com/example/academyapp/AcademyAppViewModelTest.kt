package com.example.academyapp

import com.example.academyapp.api.ApiResult
import com.example.academyapp.api.AcademyApi
import com.example.academyapp.api.AcademyApiError
import com.example.academyapp.api.model.*
import com.example.academyapp.course.NoOpQuizWebSessionController
import com.example.academyapp.course.QuizWebSessionController
import com.example.academyapp.course.QuizWebSessionCleanupResult
import com.example.academyapp.session.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class AcademyAppViewModelTest {
    private val user = User(42, "learner@example.test", "Learner")
    private val loginResponse = LoginResponse(
        user = user,
        accessToken = "access",
        refreshToken = "refresh",
        accessTokenExpiresAt = "2026-09-23T12:00:00Z",
    )

    @Test
    fun deletionRequestRequiresASessionAndKeepsAccessUntilEmailConfirmation() = runTest {
        val api = FakeApi().apply { meResult = ApiResult.Success(user) }
        val storage = MemoryStorage(SessionTokens("access", "refresh", "2026-09-23T12:00:00Z"))
        val viewModel = viewModel(api, storage)
        advanceUntilIdle()
        viewModel.requestAccountDeletion()
        viewModel.requestAccountDeletion()
        advanceUntilIdle()
        assertEquals(1, api.deletionCalls)
        assertEquals(DeletionRequestState.AwaitingConfirmation, viewModel.deletionState.value)
        assertIs<SessionState.SignedIn>(viewModel.sessionState.value)
        assertEquals("refresh", storage.tokens?.refreshToken)
        val signedOut = viewModel(FakeApi(), MemoryStorage())
        advanceUntilIdle()
        signedOut.requestAccountDeletion()
        advanceUntilIdle()
        assertEquals(DeletionRequestState.Idle, signedOut.deletionState.value)
    }

    @Test
    fun deletionFailureAllowsRetryWithoutClaimingDeletion() = runTest {
        val api = FakeApi().apply {
            meResult = ApiResult.Success(user)
            deletionResult = ApiResult.Failure(AcademyApiError.Server(503, null))
        }
        val viewModel = viewModel(api, MemoryStorage(SessionTokens("access", "refresh", "2026-09-23T12:00:00Z")))
        advanceUntilIdle()
        viewModel.requestAccountDeletion()
        advanceUntilIdle()
        assertEquals(DeletionRequestState.Failed, viewModel.deletionState.value)
        api.deletionResult = ApiResult.Success(DeletionRequestResponse("awaiting_confirmation", true))
        viewModel.requestAccountDeletion()
        advanceUntilIdle()
        assertEquals(DeletionRequestState.AwaitingConfirmation, viewModel.deletionState.value)
    }

    @Test
    fun deletionRejectsAnUnexpectedCompletionResponse() = runTest {
        val api = FakeApi().apply {
            meResult = ApiResult.Success(user)
            deletionResult = ApiResult.Success(DeletionRequestResponse("completed", false))
        }
        val viewModel = viewModel(api, MemoryStorage(SessionTokens("access", "refresh", "2026-09-23T12:00:00Z")))
        advanceUntilIdle()
        viewModel.requestAccountDeletion()
        advanceUntilIdle()
        assertEquals(DeletionRequestState.Failed, viewModel.deletionState.value)
        assertIs<SessionState.SignedIn>(viewModel.sessionState.value)
    }

    @Test
    fun initializesTheSessionAtStartup() = runTest {
        val viewModel = viewModel(FakeApi(), MemoryStorage())
        advanceUntilIdle()
        assertEquals(SessionState.SignedOut, viewModel.sessionState.value)
    }

    @Test
    fun loginPublishesSignedInStateAndPersistsTokens() = runTest {
        val storage = MemoryStorage()
        val api = FakeApi().apply { loginResult = ApiResult.Success(loginResponse) }
        val viewModel = viewModel(api, storage)
        advanceUntilIdle()

        viewModel.login("  learner@example.test  ", "password")
        advanceUntilIdle()

        assertEquals(SessionState.SignedIn(user), viewModel.sessionState.value)
        assertEquals("refresh", storage.tokens?.refreshToken)
        assertNull(viewModel.loginError.value)
    }

    @Test
    fun registrationPersistsTheNormalSessionAndSelectsMyLearningFlow() = runTest {
        val storage = MemoryStorage()
        val api = FakeApi().apply {
            registrationAvailable = true
            legalAgreementRequired = true
            registerResult = ApiResult.Success(loginResponse)
        }
        val viewModel = viewModel(api, storage)
        advanceUntilIdle()

        viewModel.showAuthScreen(AuthEntryScreen.Registration)
        viewModel.register(" learner@example.test ", "long-password", false, "Learner Example")
        advanceUntilIdle()
        assertEquals("Debes aceptar los términos y la política de privacidad para crear una cuenta.", viewModel.authActionError.value)
        assertEquals(0, api.registerCalls)

        viewModel.register(" learner@example.test ", "long-password", true, "Learner Example")
        advanceUntilIdle()

        assertEquals(SessionState.SignedIn(user), viewModel.sessionState.value)
        assertEquals("refresh", storage.tokens?.refreshToken)
        assertEquals(true, viewModel.registrationJustCompleted.value)
        assertEquals(AuthEntryScreen.SignIn, viewModel.authScreen.value)
        assertTrue(api.registeredRequest?.acceptLegalAgreement == true)
        assertEquals("Learner Example", api.registeredRequest?.displayName)
    }

    @Test
    fun registrationRequiresFullNameAndAvailableWordPressSetting() = runTest {
        val api = FakeApi().apply { registrationAvailable = true }
        val viewModel = viewModel(api, MemoryStorage())
        advanceUntilIdle()

        viewModel.register("learner@example.test", "long-password", false, " ")
        advanceUntilIdle()

        assertEquals(SessionState.SignedOut, viewModel.sessionState.value)
        assertEquals("Ingresa tu nombre completo.", viewModel.authActionError.value)
        assertEquals(0, api.registerCalls)

        val unavailableViewModel = viewModel(FakeApi(), MemoryStorage())
        advanceUntilIdle()
        unavailableViewModel.register("learner@example.test", "long-password", false, "Learner Example")
        advanceUntilIdle()
        assertEquals("La creación de cuentas no está disponible por el momento.", unavailableViewModel.authActionError.value)
    }

    @Test
    fun passwordResetShowsSameNeutralConfirmationAndDoesNotSignIn() = runTest {
        val api = FakeApi().apply { resetResult = ApiResult.Success(PasswordResetResponse(true, "accepted")) }
        val viewModel = viewModel(api, MemoryStorage())
        advanceUntilIdle()

        viewModel.showAuthScreen(AuthEntryScreen.PasswordReset)
        viewModel.requestPasswordReset("learner@example.test")
        advanceUntilIdle()

        assertEquals(SessionState.SignedOut, viewModel.sessionState.value)
        assertEquals("Si existe una cuenta asociada, recibirás instrucciones para restablecer tu contraseña.", viewModel.authActionMessage.value)
        assertEquals(1, api.resetCalls)
    }

    @Test
    fun failedLoginPublishesSafeServerMessage() = runTest {
        val api = FakeApi().apply {
            loginResult = ApiResult.Failure(
                AcademyApiError.Unauthorized(
                    ApiError(
                        code = "invalid_credentials",
                        message = "Invalid credentials.",
                        data = ApiErrorData(status = 401),
                    ),
                ),
            )
        }
        val viewModel = viewModel(api, MemoryStorage())
        advanceUntilIdle()

        viewModel.login("learner@example.test", "wrong")
        advanceUntilIdle()

        assertEquals(SessionState.SignedOut, viewModel.sessionState.value)
        assertEquals("El correo electrónico o la contraseña son incorrectos.", viewModel.loginError.value)
    }

    @Test
    fun unexpectedLoginFailureDoesNotClaimTheServiceIsUnreachableOrExposeItsCause() = runTest {
        val api = FakeApi().apply {
            loginResult = ApiResult.Failure(
                AcademyApiError.UnexpectedFailure(IllegalStateException("sensitive implementation detail")),
            )
        }
        val viewModel = viewModel(api, MemoryStorage())
        advanceUntilIdle()

        viewModel.login("learner@example.test", "wrong")
        advanceUntilIdle()

        assertEquals("Academy no pudo completar la solicitud. Inténtalo de nuevo.", viewModel.loginError.value)
    }

    @Test
    fun logoutClearsTheSignedInSession() = runTest {
        val storage = MemoryStorage(SessionTokens("access", "refresh", "2026-09-23T12:00:00Z"))
        val api = FakeApi().apply { meResult = ApiResult.Success(user) }
        val viewModel = viewModel(api, storage)
        advanceUntilIdle()
        assertIs<SessionState.SignedIn>(viewModel.sessionState.value)

        viewModel.logout()
        advanceUntilIdle()

        assertEquals(SessionState.SignedOut, viewModel.sessionState.value)
        assertNull(storage.tokens)
    }

    @Test
    fun logoutRevokesTheWebViewSessionBeforeClearingTheAppSession() = runTest {
        val storage = MemoryStorage(SessionTokens("access", "refresh", "2026-09-23T12:00:00Z"))
        val api = FakeApi().apply { meResult = ApiResult.Success(user) }
        val controller = RecordingQuizWebSessionController()
        val manager = AcademySessionManager(api, storage, MutableAccessTokenProvider())
        val viewModel = AcademyAppViewModel(
            manager,
            api,
            quizWebSessionController = controller,
            userFacingStrings = testUserFacingStrings,
            coroutineScope = this,
        )
        advanceUntilIdle()

        viewModel.logout()
        advanceUntilIdle()

        assertEquals(1, controller.revokeCalls)
        assertEquals(SessionState.SignedOut, viewModel.sessionState.value)
    }

    @Test
    fun logoutFailureKeepsTheAppSessionSoWebCleanupCanBeRetried() = runTest {
        val storage = MemoryStorage(SessionTokens("access", "refresh", "2026-09-23T12:00:00Z"))
        val api = FakeApi().apply { meResult = ApiResult.Success(user) }
        val controller = RecordingQuizWebSessionController(QuizWebSessionCleanupResult.Failed)
        val manager = AcademySessionManager(api, storage, MutableAccessTokenProvider())
        val viewModel = AcademyAppViewModel(
            manager,
            api,
            quizWebSessionController = controller,
            userFacingStrings = testUserFacingStrings,
            coroutineScope = this,
        )
        advanceUntilIdle()

        viewModel.logout()
        advanceUntilIdle()

        assertEquals(1, controller.revokeCalls)
        assertIs<SessionState.SignedIn>(viewModel.sessionState.value)
        assertEquals("access", storage.tokens?.accessToken)
        assertEquals(
            "No pudimos cerrar la sesión de la evaluación. Revisa tu conexión e intenta cerrar sesión nuevamente.",
            viewModel.loginError.value,
        )
    }

    private fun TestScope.viewModel(
        api: AcademyApi,
        storage: SessionTokenStorage,
        controller: QuizWebSessionController = NoOpQuizWebSessionController,
    ): AcademyAppViewModel {
        val manager = AcademySessionManager(api, storage, MutableAccessTokenProvider())
        return AcademyAppViewModel(
            manager,
            api,
            quizWebSessionController = controller,
            userFacingStrings = testUserFacingStrings,
            coroutineScope = this,
        )
    }

    private class RecordingQuizWebSessionController(
        private val result: QuizWebSessionCleanupResult = QuizWebSessionCleanupResult.Cleared,
    ) : QuizWebSessionController {
        var revokeCalls = 0
        override fun activate(logoutUrl: String) = Unit
        override suspend fun revokeAndClear(): QuizWebSessionCleanupResult {
            revokeCalls += 1
            return result
        }
    }

    private class MemoryStorage(var tokens: SessionTokens? = null) : SessionTokenStorage {
        override suspend fun load(): SessionTokens? = tokens
        override suspend fun save(tokens: SessionTokens) {
            this.tokens = tokens
        }
        override suspend fun clear() {
            tokens = null
        }
    }

    private class FakeApi : AcademyApi {
        var deletionCalls = 0
        var deletionResult: ApiResult<DeletionRequestResponse> = ApiResult.Success(DeletionRequestResponse("awaiting_confirmation", true))
        override suspend fun requestAccountDeletion(): ApiResult<DeletionRequestResponse> {
            deletionCalls += 1
            return deletionResult
        }
        var registrationAvailable = false
        var legalAgreementRequired = false
        var registerCalls = 0
        var resetCalls = 0
        var registeredRequest: RegisterRequest? = null
        var registerResult: ApiResult<LoginResponse> =
            ApiResult.Failure(AcademyApiError.NoConnection(IllegalStateException("unused")))
        var resetResult: ApiResult<PasswordResetResponse> =
            ApiResult.Failure(AcademyApiError.NoConnection(IllegalStateException("unused")))
        var loginResult: ApiResult<LoginResponse> =
            ApiResult.Failure(AcademyApiError.NoConnection(IllegalStateException("unused")))
        var meResult: ApiResult<User> =
            ApiResult.Failure(AcademyApiError.NoConnection(IllegalStateException("unused")))

        override suspend fun getAuthOptions() = ApiResult.Success(
            AuthOptions(registrationAvailable, legalAgreementRequired),
        )
        override suspend fun login(email: String, password: String) = loginResult
        override suspend fun register(request: RegisterRequest): ApiResult<LoginResponse> {
            registerCalls += 1
            registeredRequest = request
            return registerResult
        }
        override suspend fun requestPasswordReset(email: String): ApiResult<PasswordResetResponse> {
            resetCalls += 1
            return resetResult
        }
        override suspend fun refresh(refreshToken: String): ApiResult<LoginResponse> = unsupported()
        override suspend fun logout(): ApiResult<LogoutResponse> = ApiResult.Success(LogoutResponse(true))
        override suspend fun getMe() = meResult
        override suspend fun getCourses(): ApiResult<List<Course>> = unsupported()
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
            ApiResult.Failure(AcademyApiError.NoConnection(IllegalStateException("Unexpected call")))
    }
}
