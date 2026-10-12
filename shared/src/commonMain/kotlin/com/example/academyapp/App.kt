package com.example.academyapp

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.example.academyapp.course.CourseDestination
import com.example.academyapp.course.CourseListViewModel
import com.example.academyapp.course.CourseRoute
import com.example.academyapp.course.QuizWebSessionController
import com.example.academyapp.design.AcademyDimensions
import com.example.academyapp.design.AcademySpacing
import com.example.academyapp.design.AcademyTheme
import com.example.academyapp.design.components.AcademyAdaptivePane
import com.example.academyapp.design.components.AcademyAppScaffold
import com.example.academyapp.design.components.AcademyScreenTransition
import com.example.academyapp.design.components.AcademySectionHeader
import com.example.academyapp.design.components.AcademyStateContainer
import com.example.academyapp.features.account.presentation.AccountRoute
import com.example.academyapp.features.account.presentation.AccountViewModel
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
    val childDestination by courseListViewModel.destination.collectAsStateWithLifecycle()
    // Save a primitive value: custom Kotlin state objects are not Bundle-saveable by default.
    var savedRootDestination by rememberSaveable {
        mutableStateOf(initialAuthenticatedDestination(registrationJustCompleted).name)
    }
    val isQuizVisible = childDestination is CourseDestination.QuizWeb
    val hasLearningChild = childDestination != CourseDestination.Dashboard
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
            shellState.canCloseLearningChild,
        onBack = {
            when (childDestination) {
                is CourseDestination.QuizWeb -> courseListViewModel.returnFromQuiz()
                is CourseDestination.LessonDetail -> courseListViewModel.showCurriculum()
                is CourseDestination.Curriculum -> courseListViewModel.showCourses()
                CourseDestination.Courses -> courseListViewModel.showDashboard()
                CourseDestination.Dashboard -> Unit
            }
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
                    displayName = firstName(user.displayName),
                    coursesState = courseListViewModel.listState.collectAsStateWithLifecycle().value,
                    onViewCourses = { savedRootDestination = AcademyDestination.MyLearning.name },
                    onCourseSelected = { course ->
                        savedRootDestination = AcademyDestination.MyLearning.name
                        courseListViewModel.openCourse(course)
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

private fun firstName(displayName: String): String =
    displayName.trim().takeWhile { !it.isWhitespace() }.ifBlank { displayName }

@Composable
private fun HomeScreen(
    displayName: String,
    coursesState: com.example.academyapp.course.CourseListUiState,
    onViewCourses: () -> Unit,
    onCourseSelected: (com.example.academyapp.api.model.Course) -> Unit,
) {
    val loadedCourses = (coursesState as? com.example.academyapp.course.CourseListUiState.Content)?.courses
    val inProgressCourse = loadedCourses.orEmpty()
        .filter { it.progressPercent in 1..99 }
        .singleOrNull()
    AcademyAdaptivePane(
        primary = { twoPane ->
            Column(
                modifier = if (twoPane) Modifier.fillMaxSize().verticalScroll(rememberScrollState()) else Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(AcademySpacing.Large),
            ) {
                Text(stringResource(Res.string.nav_home), style = MaterialTheme.typography.headlineLarge)
                Text(
                    stringResource(Res.string.home_welcome, displayName),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                AnimatedVisibility(
                    visible = inProgressCourse != null,
                    enter = fadeIn(tween(240)) + slideInVertically(tween(240)) { it / 12 },
                    exit = fadeOut(tween(120)) + slideOutVertically(tween(120)) { -it / 16 },
                ) {
                    inProgressCourse?.let { course ->
                        com.example.academyapp.design.components.AcademyHeroCourseCard(
                            course,
                            onClick = { onCourseSelected(course) },
                        )
                    }
                }
            }
        },
        secondary = {
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(AcademySpacing.Large),
            ) {
                AnimatedContent(
                    targetState = loadedCourses != null,
                    transitionSpec = {
                        (fadeIn(tween(240)) + slideInVertically(tween(240)) { it / 12 }) togetherWith
                            (fadeOut(tween(120)) + slideOutVertically(tween(120)) { -it / 16 })
                    },
                    label = "home-course-content-transition",
                ) { hasLoadedCourses ->
                    if (hasLoadedCourses) {
                        Column(verticalArrangement = Arrangement.spacedBy(AcademySpacing.Large)) {
                            AcademySectionHeader(stringResource(Res.string.home_my_learning_title))
                            loadedCourses.orEmpty().take(3).forEach { course ->
                                com.example.academyapp.design.components.AcademyCourseCard(
                                    course,
                                    { onCourseSelected(course) },
                                )
                            }
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(AcademySpacing.Large)) {
                            AcademySectionHeader(
                                stringResource(Res.string.home_my_learning_title),
                                stringResource(Res.string.home_my_learning_description),
                            )
                            Button(
                                onClick = onViewCourses,
                                modifier = Modifier.fillMaxWidth(),
                                shape = com.example.academyapp.design.AcademyShapes.Pill,
                            ) {
                                Text(stringResource(Res.string.home_view_courses))
                            }
                        }
                    }
                }
            }
        },
    )
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

@Preview(widthDp = 390, heightDp = 844, name = "Inicio · compact")
@Composable
fun AcademyHomeCompactPreview() {
    AcademyTheme {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(Modifier.fillMaxSize().padding(AcademySpacing.Medium), verticalArrangement = Arrangement.spacedBy(AcademySpacing.Large)) {
                HomeScreen("Ana García", com.example.academyapp.course.CourseListUiState.Content(listOf(
                    com.example.academyapp.api.model.Course(1, "Neuroeducación y aprendizaje", "Aprende estrategias para el aula.", null, 42, listOf("Neuroeducación")),
                    com.example.academyapp.api.model.Course(2, "Desarrollo socioemocional", "", null, 0, emptyList()),
                )), {}, {})
            }
        }
    }
}

@Preview(widthDp = 914, heightDp = 411, name = "Inicio · phone landscape")
@Composable
fun AcademyHomeLandscapePreview() {
    AcademyTheme {
        Surface(Modifier.fillMaxSize().padding(AcademySpacing.Medium), color = MaterialTheme.colorScheme.background) {
            HomeScreen(
                "Ana García",
                com.example.academyapp.course.CourseListUiState.Content(
                    listOf(
                        com.example.academyapp.api.model.Course(1, "Neuroeducación y aprendizaje", "Aprende estrategias para el aula.", null, 42, listOf("Neuroeducación")),
                        com.example.academyapp.api.model.Course(2, "Desarrollo socioemocional", "", null, 0, emptyList()),
                    ),
                ),
                {},
                {},
            )
        }
    }
}

@Preview(widthDp = 390, heightDp = 844, name = "Perfil · compact")
@Composable
fun AcademyProfileCompactPreview() {
    com.example.academyapp.features.account.presentation.AccountCompactPreview()
}
