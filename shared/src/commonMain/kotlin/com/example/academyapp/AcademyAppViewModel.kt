package com.example.academyapp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.academyapp.api.ApiResult
import com.example.academyapp.api.AcademyApi
import com.example.academyapp.api.model.AuthOptions
import com.example.academyapp.api.KtorAcademyApi
import com.example.academyapp.api.AcademyApiEnvironments
import com.example.academyapp.api.AcademyEnvironment
import com.example.academyapp.course.AuthenticatedCourseRepository
import com.example.academyapp.course.CourseListViewModel
import com.example.academyapp.course.CourseRepository
import com.example.academyapp.course.QuizWebSessionController
import com.example.academyapp.course.QuizWebSessionCleanupResult
import com.example.academyapp.course.NoOpQuizWebSessionController
import com.example.academyapp.network.createAcademyHttpClient
import com.example.academyapp.session.AcademySessionManager
import com.example.academyapp.session.MutableAccessTokenProvider
import com.example.academyapp.session.SessionState
import com.example.academyapp.session.SessionTokenStorage
import com.example.academyapp.generated.resources.auth_action_failed
import com.example.academyapp.generated.resources.auth_accept_legal_agreement
import com.example.academyapp.generated.resources.auth_check_email
import com.example.academyapp.generated.resources.auth_invalid_email
import com.example.academyapp.generated.resources.auth_invalid_name
import com.example.academyapp.generated.resources.auth_invalid_password
import com.example.academyapp.generated.resources.auth_password_mismatch
import com.example.academyapp.generated.resources.auth_registration_unavailable
import com.example.academyapp.generated.resources.Res
import com.example.academyapp.generated.resources.error_close_quiz_session
import io.ktor.client.HttpClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class DeletionRequestState { Idle, Sending, AwaitingConfirmation, Failed }

enum class AuthEntryScreen { SignIn, PasswordReset, Registration }

class AcademyAppViewModel internal constructor(
    private val session: AcademySessionManager,
    private val api: AcademyApi,
    private val resources: HttpClient? = null,
    private val quizWebSessionController: QuizWebSessionController = NoOpQuizWebSessionController,
    private val userFacingStrings: UserFacingStringResolver = defaultUserFacingStringResolver,
    coroutineScope: CoroutineScope? = null,
) : ViewModel() {
    private val scope = coroutineScope ?: viewModelScope
    private val mutableLoginError = MutableStateFlow<String?>(null)
    private val mutableIsLoggingOut = MutableStateFlow(false)
    private val mutableAuthScreen = MutableStateFlow(AuthEntryScreen.SignIn)
    private val mutableAuthOptions = MutableStateFlow(AuthOptions(registrationAvailable = false))
    private val mutableAuthActionError = MutableStateFlow<String?>(null)
    private val mutableAuthActionMessage = MutableStateFlow<String?>(null)
    private val mutableIsAuthActionInProgress = MutableStateFlow(false)
    private val mutableRegistrationJustCompleted = MutableStateFlow(false)

    private val mutableDeletionState = MutableStateFlow(DeletionRequestState.Idle)
    val deletionState: StateFlow<DeletionRequestState> = mutableDeletionState.asStateFlow()

    val sessionState: StateFlow<SessionState> = session.state
    val loginError: StateFlow<String?> = mutableLoginError.asStateFlow()
    val isLoggingOut: StateFlow<Boolean> = mutableIsLoggingOut.asStateFlow()
    val authScreen: StateFlow<AuthEntryScreen> = mutableAuthScreen.asStateFlow()
    val authOptions: StateFlow<AuthOptions> = mutableAuthOptions.asStateFlow()
    val authActionError: StateFlow<String?> = mutableAuthActionError.asStateFlow()
    val authActionMessage: StateFlow<String?> = mutableAuthActionMessage.asStateFlow()
    val isAuthActionInProgress: StateFlow<Boolean> = mutableIsAuthActionInProgress.asStateFlow()
    val registrationJustCompleted: StateFlow<Boolean> = mutableRegistrationJustCompleted.asStateFlow()

    init {
        scope.launch { session.initialize() }
        scope.launch {
            val result = api.getAuthOptions()
            mutableAuthOptions.value = if (result is ApiResult.Success) result.value else AuthOptions(false)
        }
    }

    fun login(email: String, password: String) {
        if (sessionState.value == SessionState.SigningIn) return
        mutableDeletionState.value = DeletionRequestState.Idle
        mutableRegistrationJustCompleted.value = false
        mutableAuthActionError.value = null
        mutableAuthActionMessage.value = null
        mutableLoginError.value = null
        scope.launch {
            when (val result = session.login(email.trim(), password)) {
                is ApiResult.Success -> Unit
                is ApiResult.Failure -> mutableLoginError.value =
                    result.error.localizedUserMessageForState(userFacingStrings)
            }
        }
    }

    fun showAuthScreen(screen: AuthEntryScreen) {
        mutableAuthScreen.value = if (screen == AuthEntryScreen.Registration && !authOptions.value.registrationAvailable) {
            AuthEntryScreen.SignIn
        } else screen
        mutableAuthActionError.value = null
        mutableAuthActionMessage.value = null
        mutableLoginError.value = null
    }

    fun requestPasswordReset(email: String) {
        if (mutableIsAuthActionInProgress.value) return
        val normalized = email.trim()
        if (!validEmail(normalized)) {
            scope.launch { mutableAuthActionError.value = userFacingStrings.resolve(Res.string.auth_invalid_email) }
            return
        }
        mutableAuthActionError.value = null
        mutableAuthActionMessage.value = null
        mutableIsAuthActionInProgress.value = true
        scope.launch {
            try {
                when (api.requestPasswordReset(normalized)) {
                    is ApiResult.Success -> mutableAuthActionMessage.value = userFacingStrings.resolve(Res.string.auth_check_email)
                    is ApiResult.Failure -> mutableAuthActionError.value = userFacingStrings.resolve(Res.string.auth_action_failed)
                }
            } finally {
                mutableIsAuthActionInProgress.value = false
            }
        }
    }

    fun register(
        email: String,
        password: String,
        acceptLegalAgreement: Boolean,
        displayName: String,
    ) {
        if (mutableIsAuthActionInProgress.value || sessionState.value == SessionState.SigningIn) return
        val normalized = email.trim()
        val validationMessage = when {
            !authOptions.value.registrationAvailable -> Res.string.auth_registration_unavailable
            authOptions.value.legalAgreementRequired && !acceptLegalAgreement -> Res.string.auth_accept_legal_agreement
            displayName.isBlank() -> Res.string.auth_invalid_name
            !validEmail(normalized) -> Res.string.auth_invalid_email
            password.length < 8 -> Res.string.auth_invalid_password
            else -> null
        }
        if (validationMessage != null) {
            scope.launch { mutableAuthActionError.value = userFacingStrings.resolve(validationMessage) }
            return
        }
        mutableLoginError.value = null
        mutableAuthActionMessage.value = null
        mutableIsAuthActionInProgress.value = true
        scope.launch {
            try {
                when (val result = session.register(normalized, password, acceptLegalAgreement, displayName.trim())) {
                    is ApiResult.Success -> {
                        mutableRegistrationJustCompleted.value = true
                        mutableAuthScreen.value = AuthEntryScreen.SignIn
                    }
                    is ApiResult.Failure -> mutableAuthActionError.value = userFacingStrings.resolve(
                        if (!authOptions.value.registrationAvailable) Res.string.auth_registration_unavailable else Res.string.auth_action_failed,
                    )
                }
            } finally {
                mutableIsAuthActionInProgress.value = false
            }
        }
    }

    fun requestAccountDeletion() {
        if (mutableDeletionState.value == DeletionRequestState.Sending || sessionState.value !is SessionState.SignedIn) return
        mutableDeletionState.value = DeletionRequestState.Sending
        scope.launch {
            try {
                mutableDeletionState.value = when (val result = session.executeAuthenticated { api.requestAccountDeletion() }) {
                    is ApiResult.Success -> if (result.value.state == "awaiting_confirmation" && result.value.confirmationRequired) {
                        DeletionRequestState.AwaitingConfirmation
                    } else DeletionRequestState.Failed
                    is ApiResult.Failure -> DeletionRequestState.Failed
                }
            } finally {
                if (mutableDeletionState.value == DeletionRequestState.Sending) mutableDeletionState.value = DeletionRequestState.Failed
            }
        }
    }

    fun retry() {
        mutableLoginError.value = null
        scope.launch { session.initialize() }
    }

    fun logout() {
        if (mutableIsLoggingOut.value) return
        mutableLoginError.value = null
        mutableRegistrationJustCompleted.value = false
        mutableIsLoggingOut.value = true
        scope.launch {
            try {
                when (quizWebSessionController.revokeAndClear()) {
                    QuizWebSessionCleanupResult.Cleared,
                    QuizWebSessionCleanupResult.NoActiveSession,
                    -> session.logout()
                    QuizWebSessionCleanupResult.Failed -> {
                        mutableLoginError.value = userFacingStrings.resolve(Res.string.error_close_quiz_session)
                    }
                }
            } finally {
                mutableIsLoggingOut.value = false
            }
        }
    }

    private fun validEmail(value: String): Boolean =
        value.length <= 254 && Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$").matches(value)

    override fun onCleared() {
        resources?.close()
        super.onCleared()
    }
}

class AcademyAppComponent internal constructor(
    private val session: AcademySessionManager,
    private val api: AcademyApi,
    private val courseRepository: CourseRepository,
    private val client: HttpClient,
    val quizWebSessionController: QuizWebSessionController,
    private val userFacingStrings: UserFacingStringResolver = defaultUserFacingStringResolver,
) {
    fun createSessionViewModel(): AcademyAppViewModel = AcademyAppViewModel(
        session = session,
        api = api,
        resources = client,
        quizWebSessionController = quizWebSessionController,
        userFacingStrings = userFacingStrings,
    )

    fun createAccountViewModel() = com.example.academyapp.features.account.presentation.AccountViewModel(
        com.example.academyapp.features.account.data.AuthenticatedAccountRepository(api, session),
    )

    fun createCourseListViewModel(): CourseListViewModel = CourseListViewModel(
        session = session,
        repository = courseRepository,
        quizWebSessionController = quizWebSessionController,
    )
}

fun createAcademyAppComponent(
    storage: SessionTokenStorage,
    quizWebSessionController: QuizWebSessionController,
    environment: AcademyEnvironment = AcademyEnvironment.LOCAL,
): AcademyAppComponent {
    val tokenProvider = MutableAccessTokenProvider()
    val client = createAcademyHttpClient()
    val api = KtorAcademyApi(
        configuration = AcademyApiEnvironments.configurationFor(environment),
        client = client,
        accessTokenProvider = tokenProvider,
    )
    val session = AcademySessionManager(api, storage, tokenProvider)
    val courseRepository = AuthenticatedCourseRepository(api, session)
    return AcademyAppComponent(session, api, courseRepository, client, quizWebSessionController)
}
