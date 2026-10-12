package com.example.academyapp

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.academyapp.api.model.AuthOptions
import com.example.academyapp.api.model.User
import com.example.academyapp.course.navigation.LearningRoute
import androidx.compose.runtime.saveable.Saver
import com.example.academyapp.course.CourseListViewModel
import com.example.academyapp.course.CourseRoute
import com.example.academyapp.course.QuizWebSessionController
import com.example.academyapp.design.AcademyDimensions
import com.example.academyapp.design.AcademySpacing
import com.example.academyapp.design.AcademyTheme
import com.example.academyapp.design.components.AcademyAppScaffold
import com.example.academyapp.design.components.AcademyScreenTransition
import com.example.academyapp.design.components.AcademyStateContainer
import com.example.academyapp.features.account.presentation.AccountRoute
import com.example.academyapp.features.account.presentation.AccountViewModel
import com.example.academyapp.features.home.presentation.HomeScreen
import com.example.academyapp.features.auth.presentation.AuthEntryContent
import com.example.academyapp.generated.resources.*
import com.example.academyapp.navigation.AcademyDestination
import com.example.academyapp.navigation.AcademyShellState
import com.example.academyapp.navigation.initialAuthenticatedDestination
import com.example.academyapp.session.SessionState
import org.jetbrains.compose.resources.stringResource

@Composable
fun App(
    viewModel: AcademyAppViewModel,
    courseListViewModel: CourseListViewModel,
    quizWebSessionController: QuizWebSessionController,
    accountViewModel: AccountViewModel,
) {
    val deletionState by viewModel.deletionState.collectAsStateWithLifecycle()
    val sessionState by viewModel.sessionState.collectAsStateWithLifecycle()
    val loginError by viewModel.loginError.collectAsStateWithLifecycle()
    val isLoggingOut by viewModel.isLoggingOut.collectAsStateWithLifecycle()
    val authScreen by viewModel.authScreen.collectAsStateWithLifecycle()
    val authOptions by viewModel.authOptions.collectAsStateWithLifecycle()
    val authActionError by viewModel.authActionError.collectAsStateWithLifecycle()
    val authActionMessage by viewModel.authActionMessage.collectAsStateWithLifecycle()
    val isAuthActionInProgress by viewModel.isAuthActionInProgress.collectAsStateWithLifecycle()
    val registrationJustCompleted by viewModel.registrationJustCompleted.collectAsStateWithLifecycle()

    AcademyTheme(authEntry = sessionState !is SessionState.SignedIn) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            AcademyAppContent(
                sessionState = sessionState,
                loginError = loginError,
                isLoggingOut = isLoggingOut,
                onLogin = viewModel::login,
                onRetry = viewModel::retry,
                onLogout = viewModel::logout,
                authScreen = authScreen,
                authOptions = authOptions,
                authActionError = authActionError,
                authActionMessage = authActionMessage,
                isAuthActionInProgress = isAuthActionInProgress,
                onAuthScreen = viewModel::showAuthScreen,
                onPasswordReset = viewModel::requestPasswordReset,
                onRegister = viewModel::register,
                registrationJustCompleted = registrationJustCompleted,
                signedInContent = { user ->
                    AcademyShell(
                        user = user,
                        accountViewModel = accountViewModel,
                        courseListViewModel = courseListViewModel,
                        isLoggingOut = isLoggingOut,
                        logoutError = loginError,
                        onLogout = viewModel::logout,
                        quizWebSessionController = quizWebSessionController,
                        registrationJustCompleted = registrationJustCompleted,
                        deletionState = deletionState,
                        onRequestDeletion = viewModel::requestAccountDeletion,
                    )
                },
            )
        }
    }
}

@Composable
private fun AcademyAppContent(
    sessionState: SessionState,
    loginError: String?,
    isLoggingOut: Boolean,
    onLogin: (String, String) -> Unit,
    onRetry: () -> Unit,
    onLogout: () -> Unit,
    authScreen: AuthEntryScreen = AuthEntryScreen.SignIn,
    authOptions: AuthOptions = AuthOptions(false),
    authActionError: String? = null,
    authActionMessage: String? = null,
    isAuthActionInProgress: Boolean = false,
    onAuthScreen: (AuthEntryScreen) -> Unit = {},
    onPasswordReset: (String) -> Unit = {},
    onRegister: (String, String, Boolean, String) -> Unit = { _, _, _, _ -> },
    registrationJustCompleted: Boolean = false,
    signedInContent: @Composable (User) -> Unit,
) {
    PlatformStatusBarAppearance(lightIcons = sessionState !is SessionState.SignedIn)
    val sessionGateModifier = if (sessionState is SessionState.SignedIn) {
        Modifier.fillMaxSize()
    } else {
        Modifier.fillMaxSize()
            .background(com.example.academyapp.design.AcademyColors.BrandBlue)
            .statusBarsPadding()
            .imePadding()
            .padding(horizontal = 8.dp)
    }
    if (sessionState is SessionState.SignedIn) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            signedInContent(sessionState.user)
        }
    } else {
        BoxWithConstraints(
            modifier = sessionGateModifier,
        ) {
            when (sessionState) {
                SessionState.Initializing -> SessionRestoringScreen()
                SessionState.SigningIn -> AuthEntryContent(
                    isSigningIn = true, screen = authScreen, loginError = loginError,
                    authOptions = authOptions, actionError = authActionError, actionMessage = authActionMessage,
                    isActionInProgress = isAuthActionInProgress, onLogin = onLogin, onScreen = onAuthScreen,
                    onReset = onPasswordReset, onRegister = onRegister,
                )
                SessionState.SignedOut -> AuthEntryContent(
                    isSigningIn = false, screen = authScreen, loginError = loginError,
                    authOptions = authOptions, actionError = authActionError, actionMessage = authActionMessage,
                    isActionInProgress = isAuthActionInProgress, onLogin = onLogin, onScreen = onAuthScreen,
                    onReset = onPasswordReset, onRegister = onRegister,
                )
                SessionState.Expired -> AuthEntryContent(
                    isSigningIn = false, screen = authScreen, loginError = loginError,
                    authOptions = authOptions, actionError = authActionError, actionMessage = authActionMessage,
                    isActionInProgress = isAuthActionInProgress, onLogin = onLogin, onScreen = onAuthScreen,
                    onReset = onPasswordReset, onRegister = onRegister,
                    noticeMessage = stringResource(Res.string.session_expired),
                )
                is SessionState.RecoverableError -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    RecoverableErrorScreen(
                        message = sessionState.error.localizedUserMessage(),
                        isLoggingOut = isLoggingOut,
                        onRetry = onRetry,
                        onLogout = onLogout,
                    )
                }
                is SessionState.SignedIn -> Unit // Handled by the non-scrollable shell above.
            }
        }
    }
}

@Composable
private fun SessionRestoringScreen() {
    val restoringDescription = stringResource(Res.string.session_restoring)
    AcademyStateContainer {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(AcademySpacing.Large),
        ) {
            Text(
                text = stringResource(Res.string.app_name),
                color = androidx.compose.ui.graphics.Color.White,
                style = MaterialTheme.typography.headlineMedium,
            )
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp).semantics {
                    contentDescription = restoringDescription
                },
                color = androidx.compose.ui.graphics.Color.White,
                strokeWidth = 4.dp,
            )
        }
    }
}

@Composable
private fun AcademyShell(
    user: User,
    accountViewModel: AccountViewModel,
    courseListViewModel: CourseListViewModel,
    isLoggingOut: Boolean,
    logoutError: String?,
    onLogout: () -> Unit,
    quizWebSessionController: QuizWebSessionController,
    registrationJustCompleted: Boolean = false,
    deletionState: DeletionRequestState = DeletionRequestState.Idle,
    onRequestDeletion: () -> Unit = {},
) {
    // Bind saved history to the signed-in account. The VM remains the sole live stack owner.
    rememberSaveable(user.id, saver = Saver<CourseListViewModel, String>(
        save = { it.saveNavigation(user.id) },
        restore = { saved -> courseListViewModel.also { it.restoreNavigation(user.id, saved) } },
    )) { courseListViewModel }
    val childDestination = courseListViewModel.backStack.last()
    val learningRestorationError by courseListViewModel.restorationError.collectAsStateWithLifecycle()
    val learningRecoveryDismissed by courseListViewModel.recoveryDismissed.collectAsStateWithLifecycle()
    val isRestoringLearning by courseListViewModel.isRestoringNavigation.collectAsStateWithLifecycle()
    val interruptedQuizCleanup by courseListViewModel.interruptedQuizCleanupPending.collectAsStateWithLifecycle()
    // Save a primitive value: custom Kotlin state objects are not Bundle-saveable by default.
    var savedRootDestination by rememberSaveable {
        mutableStateOf(initialAuthenticatedDestination(registrationJustCompleted).name)
    }
    val isQuizVisible = childDestination is LearningRoute.QuizWeb
    val hasLearningChild = childDestination != LearningRoute.Dashboard
    val shellState = AcademyShellState.restore(
        savedRoot = savedRootDestination,
        hasLearningChild = hasLearningChild,
        retainedQuizChildActive = isQuizVisible,
    )
    LaunchedEffect(isQuizVisible) {
        if (isQuizVisible) savedRootDestination = AcademyDestination.MyLearning.name
    }
    LaunchedEffect(registrationJustCompleted) {
        if (registrationJustCompleted) {
            savedRootDestination = initialAuthenticatedDestination(registrationJustCompleted).name
        }
    }
    PlatformBackHandler(
        enabled = shellState.navigation.selectedDestination == AcademyDestination.MyLearning &&
            shellState.canCloseLearningChild && !learningRecoveryDismissed && (isRestoringLearning || interruptedQuizCleanup || learningRestorationError != null),
        // NavDisplay handles normal Back; this fallback covers its temporary recovery gate.
        onBack = {
            courseListViewModel.navigateBack()
        },
    )

    AcademyAppScaffold(
        destination = shellState.navigation.selectedDestination,
        showNavigation = shellState.showPrimaryNavigation,
        onDestinationSelected = {
            if (shellState.navigation.selectedDestination == AcademyDestination.Profile) {
                accountViewModel.requestLeave { savedRootDestination = shellState.navigation.select(it).saveableRoot() }
            } else savedRootDestination = shellState.navigation.select(it).saveableRoot()
        },
    ) {
        AcademyScreenTransition(
            targetState = shellState.navigation.selectedDestination,
            stateOrder = { destination ->
                when (destination) {
                    AcademyDestination.Home -> 0
                    AcademyDestination.MyLearning -> 1
                    AcademyDestination.Profile -> 2
                }
            },
            modifier = Modifier.fillMaxSize(),
        ) { destination ->
            when (destination) {
                AcademyDestination.Home -> HomeScreen(
                    summary = courseListViewModel.listState.collectAsStateWithLifecycle().value
                        .toHomeSummary(user.displayName),
                    onViewCourses = { savedRootDestination = AcademyDestination.MyLearning.name },
                    onCourseSelected = { courseId ->
                        openHomeCourse(
                            courseId = courseId,
                            state = courseListViewModel.listState.value,
                            onNavigate = { savedRootDestination = AcademyDestination.MyLearning.name },
                            onOpenCourse = courseListViewModel::openCourse,
                        )
                    },
                )
                AcademyDestination.MyLearning -> CourseRoute(
                    viewModel = courseListViewModel,
                    quizWebSessionController = quizWebSessionController,
                )
                AcademyDestination.Profile -> AccountRoute(
                    accountId = user.id,
                    viewModel = accountViewModel,
                    onBack = { savedRootDestination = AcademyDestination.Home.name },
                    isLoggingOut = isLoggingOut,
                    logoutError = logoutError,
                    onLogout = { accountViewModel.requestLeave(onLogout) },
                    deletionState = deletionState,
                    onRequestDeletion = onRequestDeletion,
                )
            }
        }
    }
}

@Composable
private fun RecoverableErrorScreen(
    message: String,
    isLoggingOut: Boolean,
    onRetry: () -> Unit,
    onLogout: () -> Unit,
) {
    Card(modifier = Modifier.widthIn(max = AcademyDimensions.ReadingMaxWidth).fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(AcademySpacing.Large),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(AcademySpacing.Medium),
        ) {
            Text(stringResource(Res.string.session_recovery_title), style = MaterialTheme.typography.headlineSmall)
            Text(message, textAlign = TextAlign.Center)
            OutlinedButton(onClick = onLogout, enabled = !isLoggingOut, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(if (isLoggingOut) Res.string.signing_out else Res.string.sign_out))
            }
            Button(onClick = onRetry, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(Res.string.try_again))
            }
        }
    }
}

@Preview
@Composable
fun AcademyAppPreview() {
    AcademyTheme(authEntry = true) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            AcademyAppContent(
                sessionState = SessionState.SignedOut,
                loginError = null,
                isLoggingOut = false,
                onLogin = { _, _ -> },
                onRetry = {},
                onLogout = {},
                authOptions = AuthOptions(true, true, "https://example.test/terms", "https://example.test/privacy"),
                signedInContent = {},
            )
        }
    }
}

@Preview(widthDp = 390, heightDp = 844, name = "Perfil · compact")
@Composable
fun AcademyProfileCompactPreview() {
    com.example.academyapp.features.account.presentation.AccountCompactPreview()
}
