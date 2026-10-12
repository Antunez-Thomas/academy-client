package com.example.academyapp.course

import androidx.navigation3.runtime.NavBackStack
import com.example.academyapp.course.navigation.LearningRoute
import com.example.academyapp.course.navigation.saveLearningNavigation
import com.example.academyapp.course.navigation.restoreLearningNavigation
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.academyapp.api.ApiResult
import com.example.academyapp.api.model.Course
import com.example.academyapp.api.model.CourseSectionHeading
import com.example.academyapp.api.model.DashboardEvent
import com.example.academyapp.api.model.DashboardLiveClass
import com.example.academyapp.api.model.DashboardSectionStatus
import com.example.academyapp.api.model.DashboardSupportAction
import com.example.academyapp.api.model.CourseProgress
import com.example.academyapp.api.model.Lesson
import com.example.academyapp.api.model.PlaybackResponse
import com.example.academyapp.api.model.ProgressRequest
import com.example.academyapp.api.model.ProgressResponse
import com.example.academyapp.api.model.Quiz
import com.example.academyapp.api.model.QuizWebLaunch
import com.example.academyapp.api.model.QuizWebLaunchRequest
import com.example.academyapp.session.AcademySessionManager
import com.example.academyapp.session.SessionState
import com.example.academyapp.localizedUserMessageForState
import com.example.academyapp.UserFacingStringResolver
import com.example.academyapp.defaultUserFacingStringResolver
import com.example.academyapp.generated.resources.Res
import com.example.academyapp.generated.resources.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.random.Random
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

sealed interface CourseListUiState {
    data object SignedOut : CourseListUiState
    data object Loading : CourseListUiState
    data object Empty : CourseListUiState
    data class Content(val courses: List<Course>) : CourseListUiState
    data class Error(val message: String) : CourseListUiState
}

sealed interface DashboardSectionUiState<out T> {
    data object Loading : DashboardSectionUiState<Nothing>
    data object Empty : DashboardSectionUiState<Nothing>
    data object Unavailable : DashboardSectionUiState<Nothing>
    data class Content<T>(val items: List<T>) : DashboardSectionUiState<T>
    data class Error(val message: String) : DashboardSectionUiState<Nothing>
}

sealed interface CurriculumUiState {
    data object Hidden : CurriculumUiState
    data class Loading(val course: Course) : CurriculumUiState
    data class Empty(val course: Course) : CurriculumUiState
    data class Content(
        val course: Course,
        val lessons: List<Lesson>,
        val quizzes: List<Quiz>,
        val sectionHeadings: List<CourseSectionHeading> = emptyList(),
    ) : CurriculumUiState

    data class Error(
        val course: Course,
        val message: String,
    ) : CurriculumUiState
}

sealed interface QuizWebUiState {
    data object Hidden : QuizWebUiState
    data object Loading : QuizWebUiState
    data class Ready(val launch: QuizWebLaunch) : QuizWebUiState
    data class Error(val message: String) : QuizWebUiState
    data object Closing : QuizWebUiState
    data class CleanupError(val message: String) : QuizWebUiState
}

sealed interface LessonUiState {
    data object Hidden : LessonUiState
    data class Loading(
        val course: Course,
        val selectedLesson: Lesson,
    ) : LessonUiState

    data class Content(
        val course: Course,
        val lesson: Lesson,
    ) : LessonUiState

    data class Error(
        val course: Course,
        val selectedLesson: Lesson,
        val message: String,
    ) : LessonUiState
}

sealed interface PlaybackUiState {
    data object Hidden : PlaybackUiState
    data object NoVideo : PlaybackUiState
    data object Loading : PlaybackUiState

    class Playable internal constructor(val playbackUrl: String) : PlaybackUiState {
        override fun toString(): String = "Playable"
    }

    data class Error(val message: String) : PlaybackUiState
}

sealed interface LessonCompletionUiState {
    data object Hidden : LessonCompletionUiState
    data object Available : LessonCompletionUiState
    data object Saving : LessonCompletionUiState
    data object Saved : LessonCompletionUiState
    data object AlreadyCompleted : LessonCompletionUiState
    data class Error(val message: String) : LessonCompletionUiState
}

fun interface ProgressEventIdGenerator {
    fun nextId(): String
}

fun interface QuizLaunchRequestIdGenerator {
    fun nextId(): String
}

@OptIn(ExperimentalTime::class)
class CourseListViewModel internal constructor(
    private val session: AcademySessionManager,
    private val repository: CourseRepository,
    coroutineScope: CoroutineScope? = null,
    private val eventIdGenerator: ProgressEventIdGenerator = ProgressEventIdGenerator { newProgressEventId() },
    private val quizRequestIdGenerator: QuizLaunchRequestIdGenerator =
        QuizLaunchRequestIdGenerator { newQuizLaunchRequestId() },
    private val quizWebSessionController: QuizWebSessionController = NoOpQuizWebSessionController,
    private val userFacingStrings: UserFacingStringResolver = defaultUserFacingStringResolver,
) : ViewModel() {
    val supportSiteHost: String? = repository.supportSiteHost
    private val scope = coroutineScope ?: viewModelScope
    private val mutableListState = MutableStateFlow<CourseListUiState>(CourseListUiState.SignedOut)
    private val mutableLiveClassesState = MutableStateFlow<DashboardSectionUiState<DashboardLiveClass>>(DashboardSectionUiState.Loading)
    private val mutableEventsState = MutableStateFlow<DashboardSectionUiState<DashboardEvent>>(DashboardSectionUiState.Loading)
    private val mutableSupportActionsState = MutableStateFlow<DashboardSectionUiState<DashboardSupportAction>>(DashboardSectionUiState.Loading)
    // The Nav3 stack is the only authoritative child-route history.
    val backStack = NavBackStack<LearningRoute>(LearningRoute.Dashboard)
    private var navigationTouched = false
    private var restorationJob: Job? = null
    private var pendingRestoredNavigation: List<LearningRoute>? = null
    private val mutableRestoring = MutableStateFlow(false)
    // Back dismisses the presentation gate, independently of mandatory quiz cleanup.
    private val mutableRecoveryDismissed = MutableStateFlow(false)
    val recoveryDismissed: StateFlow<Boolean> = mutableRecoveryDismissed.asStateFlow()
    private val mutableRestorationError = MutableStateFlow<String?>(null)
    val restorationError: StateFlow<String?> = mutableRestorationError.asStateFlow()
    private val mutableInterruptedQuizCleanup = MutableStateFlow(false)
    val isRestoringNavigation: StateFlow<Boolean> = mutableRestoring.asStateFlow()
    val interruptedQuizCleanupPending: StateFlow<Boolean> = mutableInterruptedQuizCleanup.asStateFlow()
    private val mutableCurriculumState = MutableStateFlow<CurriculumUiState>(CurriculumUiState.Hidden)
    private val mutableLessonState = MutableStateFlow<LessonUiState>(LessonUiState.Hidden)
    private val mutablePlaybackState = MutableStateFlow<PlaybackUiState>(PlaybackUiState.Hidden)
    private val mutableCompletionState =
        MutableStateFlow<LessonCompletionUiState>(LessonCompletionUiState.Hidden)
    private val mutableQuizWebState = MutableStateFlow<QuizWebUiState>(QuizWebUiState.Hidden)
    private var listJob: Job? = null
    private var curriculumJob: Job? = null
    private var lessonJob: Job? = null
    private var playbackJob: Job? = null
    private var completionJob: Job? = null
    private var quizLaunchJob: Job? = null
    private var quizCleanupJob: Job? = null
    private var pendingCompletion: PendingCompletion? = null
    private var pendingQuizLaunch: QuizWebLaunchRequest? = null
    private var recentlyOpenedCourseId: Long? = null

    val listState: StateFlow<CourseListUiState> = mutableListState.asStateFlow()
    val liveClassesState: StateFlow<DashboardSectionUiState<DashboardLiveClass>> = mutableLiveClassesState.asStateFlow()
    val eventsState: StateFlow<DashboardSectionUiState<DashboardEvent>> = mutableEventsState.asStateFlow()
    val supportActionsState: StateFlow<DashboardSectionUiState<DashboardSupportAction>> = mutableSupportActionsState.asStateFlow()
    val curriculumState: StateFlow<CurriculumUiState> = mutableCurriculumState.asStateFlow()
    val lessonState: StateFlow<LessonUiState> = mutableLessonState.asStateFlow()
    val playbackState: StateFlow<PlaybackUiState> = mutablePlaybackState.asStateFlow()
    val completionState: StateFlow<LessonCompletionUiState> = mutableCompletionState.asStateFlow()
    val quizWebState: StateFlow<QuizWebUiState> = mutableQuizWebState.asStateFlow()

    init {
        scope.launch {
            session.state.collectLatest { state ->
                if (state is SessionState.SignedIn) {
                    if (mutableListState.value == CourseListUiState.SignedOut) {
                        loadCourses()
                        if (quizWebSessionController.hasPendingCleanup()) {
                            pendingRestoredNavigation = backStack.toList()
                            mutableInterruptedQuizCleanup.value = true
                            rehydrateNavigation()
                        }
                    }
                } else {
                    clearForSignedOutSession()
                }
            }
        }
    }

    fun retryCourses() {
        loadCourses()
    }

    fun retryLiveClasses() = loadCourses()

    fun retryEvents() = loadCourses()

    fun retrySupportActions() = loadCourses()

    fun openCourse(course: Course) {
        if (session.state.value !is SessionState.SignedIn) return
        if (backStack.last() is LearningRoute.QuizWeb || mutableInterruptedQuizCleanup.value || quizWebSessionController.hasPendingCleanup()) return
        promoteRecentlyOpenedCourse(course.id)
        restorationJob?.cancel()
        pendingRestoredNavigation = null
        mutableRestoring.value = false
        mutableRestorationError.value = null
        mutableRecoveryDismissed.value = false
        lessonJob?.cancel()
        clearPlayback()
        clearCompletion()
        mutableLessonState.value = LessonUiState.Hidden
        navigationTouched = true
        while (backStack.last() != LearningRoute.Dashboard && backStack.last() != LearningRoute.Courses) {
            backStack.removeLastOrNull()
        }
        backStack.add(LearningRoute.Curriculum(course.id))
        loadCurriculum(course)
    }

    private fun promoteRecentlyOpenedCourse(courseId: Long) {
        recentlyOpenedCourseId = courseId
        val content = mutableListState.value as? CourseListUiState.Content ?: return
        val index = content.courses.indexOfFirst { it.id == courseId }
        if (index <= 0) return
        val ordered = content.courses.toMutableList()
        val recentlyOpened = ordered.removeAt(index)
        ordered.add(0, recentlyOpened)
        mutableListState.value = content.copy(courses = ordered)
    }

    fun showAllCourses() {
        if (session.state.value !is SessionState.SignedIn) return
        if (backStack.last() != LearningRoute.Dashboard) return
        navigationTouched = true
        backStack.add(LearningRoute.Courses)
    }

    fun showDashboard() {
        if (session.state.value !is SessionState.SignedIn) return
        if (backStack.last() == LearningRoute.Courses) backStack.removeLastOrNull()
    }

    fun retryCurriculum() {
        val course = activeCurriculumCourse() ?: return
        loadCurriculum(course)
    }

    fun openLesson(lesson: Lesson) {
        if (session.state.value !is SessionState.SignedIn) return
        val course = activeCurriculumCourse() ?: return
        backStack.add(LearningRoute.LessonDetail(course.id, lesson.id))
        loadLesson(course, lesson)
    }

    fun retryLesson() {
        val route = backStack.last() as? LearningRoute.LessonDetail ?: return
        val course = curriculumCourse() ?: return
        val lesson = (mutableCurriculumState.value as? CurriculumUiState.Content)?.lessons
            ?.firstOrNull { it.id == route.lessonId } ?: return
        loadLesson(course, lesson)
    }

    fun retryPlayback() {
        if (backStack.last() !is LearningRoute.LessonDetail) return
        val course = curriculumCourse() ?: return
        val lesson = (mutableLessonState.value as? LessonUiState.Content)?.lesson ?: return
        if (lesson.video == null) return
        authorizePlayback(course, lesson)
    }

    fun markLessonComplete() {
        if (session.state.value !is SessionState.SignedIn || completionJob?.isActive == true) return
        val content = mutableLessonState.value as? LessonUiState.Content ?: return
        if (content.lesson.completed || mutableCompletionState.value != LessonCompletionUiState.Available) return
        val request = ProgressRequest(
            courseId = content.course.id,
            completed = true,
            positionSeconds = content.lesson.positionSeconds,
            clientEventId = eventIdGenerator.nextId(),
        )
        PendingCompletion(content.lesson.id, request).also {
            pendingCompletion = it
            submitCompletion(it)
        }
    }

    fun retryLessonCompletion() {
        if (
            session.state.value !is SessionState.SignedIn ||
            completionJob?.isActive == true ||
            mutableCompletionState.value !is LessonCompletionUiState.Error
        ) {
            return
        }
        pendingCompletion?.let(::submitCompletion)
    }

    fun openQuiz(quiz: Quiz) {
        if (session.state.value !is SessionState.SignedIn || quizLaunchJob?.isActive == true) return
        val course = activeCurriculumCourse() ?: return
        if (quiz.courseId != course.id || mutableInterruptedQuizCleanup.value || quizWebSessionController.hasPendingCleanup()) return
        backStack.add(LearningRoute.QuizWeb(course.id, quiz.id))
        QuizWebLaunchRequest(
            courseId = course.id,
            quizId = quiz.id,
            clientRequestId = quizRequestIdGenerator.nextId(),
        ).also {
            pendingQuizLaunch = it
            launchQuiz(it)
        }
    }

    fun retryQuizLaunch() {
        if (session.state.value !is SessionState.SignedIn || quizLaunchJob?.isActive == true) return
        pendingQuizLaunch?.let(::launchQuiz)
    }

    fun returnFromQuiz() {
        if (quizCleanupJob?.isActive == true) return
        val route = backStack.last() as? LearningRoute.QuizWeb ?: return
        val course = curriculumCourse() ?: return
        mutableQuizWebState.value = QuizWebUiState.Closing
        quizCleanupJob = scope.launch {
            when (quizWebSessionController.revokeAndClear()) {
                QuizWebSessionCleanupResult.Failed -> {
                    mutableQuizWebState.value = QuizWebUiState.CleanupError(
                        userFacingStrings.resolve(Res.string.error_close_quiz_session),
                    )
                    return@launch
                }
                QuizWebSessionCleanupResult.Cleared,
                QuizWebSessionCleanupResult.NoActiveSession,
                -> Unit
            }
            clearQuizLaunch()
            if (backStack.last() != route || session.state.value !is SessionState.SignedIn) return@launch
            backStack.removeLastOrNull()
            loadCurriculum(course)
            curriculumJob?.join()
            when (val result = repository.getCourseProgress(course.id)) {
                is ApiResult.Success -> if (result.value.isValidFor(course.id)) {
                    applyCourseProgress(result.value)
                }
                is ApiResult.Failure -> Unit
            }
        }
    }

    fun retryQuizCleanup() = returnFromQuiz()

    fun showCurriculum() {
        if (backStack.last() !is LearningRoute.LessonDetail) return
        lessonJob?.cancel()
        clearPlayback()
        clearCompletion()
        clearQuizLaunch()
        backStack.removeLastOrNull()
        mutableLessonState.value = LessonUiState.Hidden
    }

    fun showCourses() {
        if (backStack.last() !is LearningRoute.Curriculum) return
        curriculumJob?.cancel()
        lessonJob?.cancel()
        clearPlayback()
        clearCompletion()
        clearQuizLaunch()
        backStack.removeLastOrNull()
        mutableCurriculumState.value = CurriculumUiState.Hidden
        mutableLessonState.value = LessonUiState.Hidden
    }

    fun navigateBack() {
        if (!mutableRecoveryDismissed.value &&
            (mutableRestoring.value || mutableRestorationError.value != null || mutableInterruptedQuizCleanup.value)
        ) {
            // Back abandons unresolved children, including while their requests are suspended.
            // Cleanup must still finish/retry before a fresh quiz can launch.
            val cleanupPending = mutableInterruptedQuizCleanup.value
            if (!cleanupPending) {
                restorationJob?.cancel()
                restorationJob = null
            }
            mutableRecoveryDismissed.value = true
            mutableRestoring.value = false
            curriculumJob?.cancel()
            lessonJob?.cancel()
            mutableRestorationError.value = null
            while (backStack.last() != LearningRoute.Dashboard && backStack.last() != LearningRoute.Courses) {
                backStack.removeLastOrNull()
            }
            pendingRestoredNavigation = if (cleanupPending) backStack.toList() else null
            mutableCurriculumState.value = CurriculumUiState.Hidden
            mutableLessonState.value = LessonUiState.Hidden
            clearPlayback()
            clearCompletion()
            return
        }
        when (backStack.last()) {
            LearningRoute.Dashboard -> Unit
            LearningRoute.Courses -> showDashboard()
            is LearningRoute.Curriculum -> showCourses()
            is LearningRoute.LessonDetail -> showCurriculum()
            is LearningRoute.QuizWeb -> returnFromQuiz()
        }
        if (mutableRecoveryDismissed.value && mutableInterruptedQuizCleanup.value) {
            pendingRestoredNavigation = backStack.toList()
        }
    }

    private fun activeCurriculumCourse(): Course? =
        (backStack.last() as? LearningRoute.Curriculum)?.let { route ->
            curriculumCourse()?.takeIf { it.id == route.courseId }
        }

    private fun curriculumCourse(): Course? = when (val state = mutableCurriculumState.value) {
        CurriculumUiState.Hidden -> null
        is CurriculumUiState.Loading -> state.course
        is CurriculumUiState.Empty -> state.course
        is CurriculumUiState.Content -> state.course
        is CurriculumUiState.Error -> state.course
    }

    internal fun saveNavigation(accountId: Long): String =
        saveLearningNavigation(accountId, backStack.toList(),
            mutableInterruptedQuizCleanup.value || quizWebSessionController.hasPendingCleanup())

    internal fun restoreNavigation(accountId: Long, saved: String) {
        // Rotation retains this VM and an active quiz. Only a fresh VM consumes saved history.
        if (navigationTouched || session.state.value !is SessionState.SignedIn) return
        val user = (session.state.value as? SessionState.SignedIn)?.user ?: return
        if (user.id != accountId) return
        val restored = restoreLearningNavigation(accountId, saved) ?: return
        navigationTouched = true
        mutableRecoveryDismissed.value = false
        backStack.clear()
        backStack.addAll(restored.routes)
        pendingRestoredNavigation = restored.routes
        mutableInterruptedQuizCleanup.value = restored.interruptedQuiz || quizWebSessionController.hasPendingCleanup()
        rehydrateNavigation()
    }

    fun retryInterruptedQuizCleanup() = rehydrateNavigation()

    fun retryNavigationRestoration() = rehydrateNavigation()

    private fun rehydrateNavigation() {
        if (restorationJob?.isActive == true) return
        if (pendingRestoredNavigation == null) return
        val userId = (session.state.value as? SessionState.SignedIn)?.user?.id ?: return
        mutableRestoring.value = !mutableRecoveryDismissed.value
        mutableRestorationError.value = null
        restorationJob = scope.launch {
            try {
                if (mutableInterruptedQuizCleanup.value) {
                    mutableQuizWebState.value = QuizWebUiState.Closing
                    when (quizWebSessionController.revokeAndClear()) {
                        QuizWebSessionCleanupResult.Failed -> {
                            mutableQuizWebState.value = QuizWebUiState.CleanupError(
                                userFacingStrings.resolve(Res.string.error_close_quiz_session),
                            )
                            return@launch
                        }
                        QuizWebSessionCleanupResult.Cleared,
                        QuizWebSessionCleanupResult.NoActiveSession -> {
                            mutableInterruptedQuizCleanup.value = false
                            clearQuizLaunch()
                        }
                    }
                }
                if ((session.state.value as? SessionState.SignedIn)?.user?.id != userId) return@launch
                // Restore may have supplied a saved route while startup cleanup was suspended.
                val routes = pendingRestoredNavigation ?: return@launch
                val curriculumRoute = routes.filterIsInstance<LearningRoute.Curriculum>().lastOrNull()
                if (curriculumRoute == null) {
                    pendingRestoredNavigation = null
                    return@launch
                }
                val result = repository.getCourses()
                if (backStack.toList() != routes) {
                    if (pendingRestoredNavigation == routes) pendingRestoredNavigation = null
                    return@launch
                }
                val courses = when (result) {
                    is ApiResult.Success -> result.value
                    is ApiResult.Failure -> {
                        mutableRestorationError.value = result.error.localizedUserMessageForState(userFacingStrings)
                        return@launch
                    }
                }
                val course = courses.firstOrNull { it.id == curriculumRoute.courseId }
                if (course == null) {
                    returnToCoursePicker()
                    return@launch
                }
                loadCurriculum(course)
                curriculumJob?.join()
                if (backStack.toList() != routes) {
                    if (pendingRestoredNavigation == routes) pendingRestoredNavigation = null
                    return@launch
                }
                val content = mutableCurriculumState.value as? CurriculumUiState.Content
                val empty = mutableCurriculumState.value as? CurriculumUiState.Empty
                val error = mutableCurriculumState.value as? CurriculumUiState.Error
                if (error != null) {
                    mutableRestorationError.value = error.message
                    return@launch
                }
                if ((content?.course?.id ?: empty?.course?.id) != course.id) {
                    mutableRestorationError.value = userFacingStrings.resolve(Res.string.error_invalid_response)
                    return@launch
                }
                val lessonRoute = routes.lastOrNull() as? LearningRoute.LessonDetail
                if (lessonRoute != null) {
                    val lesson = content?.lessons?.firstOrNull { it.id == lessonRoute.lessonId }
                    if (lesson == null) backStack.removeLastOrNull() else loadLesson(content.course, lesson)
                }
                pendingRestoredNavigation = null
            } finally {
                mutableRestoring.value = false
            }
        }
    }

    private fun returnToCoursePicker() {
        pendingRestoredNavigation = null
        mutableRestorationError.value = null
        backStack.clear()
        backStack.addAll(listOf(LearningRoute.Dashboard, LearningRoute.Courses))
        mutableCurriculumState.value = CurriculumUiState.Hidden
        mutableLessonState.value = LessonUiState.Hidden
        clearPlayback()
        clearCompletion()
        retryCourses()
    }

    private fun loadCourses() {
        if (session.state.value !is SessionState.SignedIn) {
            clearForSignedOutSession()
            return
        }
        listJob?.cancel()
        listJob = scope.launch {
            mutableListState.value = CourseListUiState.Loading
            mutableLiveClassesState.value = DashboardSectionUiState.Loading
            mutableEventsState.value = DashboardSectionUiState.Loading
            mutableSupportActionsState.value = DashboardSectionUiState.Loading
            when (val result = repository.getDashboard()) {
                is ApiResult.Success -> {
                    val dashboard = result.value
                    mutableListState.value = if (dashboard.courses.isEmpty()) {
                        CourseListUiState.Empty
                    } else {
                        CourseListUiState.Content(dashboard.courses.promote(recentlyOpenedCourseId))
                    }
                    mutableLiveClassesState.value = dashboard.upcomingLiveClasses.items.toSectionState(dashboard.upcomingLiveClasses.status)
                    mutableEventsState.value = dashboard.events.items.toSectionState(dashboard.events.status)
                    mutableSupportActionsState.value = dashboard.supportActions.items.toSectionState(dashboard.supportActions.status)
                }
                is ApiResult.Failure -> {
                    val message = result.error.localizedUserMessageForState(userFacingStrings)
                    mutableListState.value = CourseListUiState.Error(message)
                    mutableLiveClassesState.value = DashboardSectionUiState.Error(message)
                    mutableEventsState.value = DashboardSectionUiState.Error(message)
                    mutableSupportActionsState.value = DashboardSectionUiState.Error(message)
                }
            }
        }
    }

    private fun <T> List<T>.toSectionState(status: DashboardSectionStatus): DashboardSectionUiState<T> =
        when {
            status == DashboardSectionStatus.UNAVAILABLE -> DashboardSectionUiState.Unavailable
            isEmpty() -> DashboardSectionUiState.Empty
            else -> DashboardSectionUiState.Content(this)
        }

    private fun loadCurriculum(course: Course) {
        curriculumJob?.cancel()
        curriculumJob = scope.launch {
            mutableCurriculumState.value = CurriculumUiState.Loading(course)
            when (val result = repository.getCourseContent(course.id)) {
                is ApiResult.Success -> {
                    val content = result.value
                    mutableCurriculumState.value = if (content.steps.isEmpty() && content.quizzes.isEmpty()) {
                        CurriculumUiState.Empty(content.course)
                    } else {
                        CurriculumUiState.Content(content.course, content.steps, content.quizzes, content.sectionHeadings)
                    }
                }
                is ApiResult.Failure -> {
                    mutableCurriculumState.value = CurriculumUiState.Error(
                        course = course,
                        message = result.error.localizedUserMessageForState(userFacingStrings),
                    )
                }
            }
        }
    }

    private fun loadLesson(course: Course, selectedLesson: Lesson) {
        lessonJob?.cancel()
        clearPlayback()
        clearCompletion()
        lessonJob = scope.launch {
            mutableLessonState.value = LessonUiState.Loading(course, selectedLesson)
            when (val result = repository.getLesson(course.id, selectedLesson.id)) {
                is ApiResult.Success -> {
                    val lesson = result.value
                    mutableLessonState.value = LessonUiState.Content(course, lesson)
                    mutableCompletionState.value = if (lesson.completed) {
                        LessonCompletionUiState.AlreadyCompleted
                    } else {
                        LessonCompletionUiState.Available
                    }
                    if (lesson.video == null) {
                        mutablePlaybackState.value = PlaybackUiState.NoVideo
                    } else {
                        authorizePlayback(course, lesson)
                    }
                }
                is ApiResult.Failure -> {
                    mutableLessonState.value = LessonUiState.Error(
                        course = course,
                        selectedLesson = selectedLesson,
                        message = result.error.localizedUserMessageForState(userFacingStrings),
                    )
                }
            }
        }
    }

    private fun submitCompletion(pending: PendingCompletion) {
        completionJob = scope.launch {
            mutableCompletionState.value = LessonCompletionUiState.Saving
            when (val result = repository.saveLessonProgress(pending.lessonId, pending.request)) {
                is ApiResult.Success -> {
                    val response = result.value
                    if (!response.matches(pending)) {
                        mutableCompletionState.value = LessonCompletionUiState.Error(
                            userFacingStrings.resolve(Res.string.error_mark_lesson_complete),
                        )
                        return@launch
                    }
                    applyLessonProgress(response)
                    when (val progress = repository.getCourseProgress(pending.request.courseId)) {
                        is ApiResult.Success -> {
                            if (!progress.value.isValidFor(pending.request.courseId)) {
                                mutableCompletionState.value =
                                    LessonCompletionUiState.Error(
                                        userFacingStrings.resolve(Res.string.error_refresh_course_progress),
                                    )
                                return@launch
                            }
                            applyCourseProgress(progress.value)
                            pendingCompletion = null
                            mutableCompletionState.value = LessonCompletionUiState.Saved
                        }
                        is ApiResult.Failure -> {
                            mutableCompletionState.value = LessonCompletionUiState.Error(
                                userFacingStrings.resolve(Res.string.error_refresh_course_progress),
                            )
                        }
                    }
                }
                is ApiResult.Failure -> {
                    mutableCompletionState.value = LessonCompletionUiState.Error(
                        userFacingStrings.resolve(Res.string.error_mark_lesson_complete),
                    )
                }
            }
        }
    }

    private fun applyLessonProgress(response: ProgressResponse) {
        val content = mutableLessonState.value as? LessonUiState.Content ?: return
        val updatedLesson = content.lesson.copy(
            completed = response.completed,
            positionSeconds = response.positionSeconds,
        )
        mutableLessonState.value = content.copy(lesson = updatedLesson)
        mutableCurriculumState.value = (mutableCurriculumState.value as? CurriculumUiState.Content)
            ?.let { curriculum ->
                curriculum.copy(
                    lessons = curriculum.lessons.map { lesson ->
                        if (lesson.id == updatedLesson.id) updatedLesson else lesson
                    },
                )
            }
            ?: mutableCurriculumState.value
    }

    private fun applyCourseProgress(progress: CourseProgress) {
        fun Course.updated(): Course = copy(progressPercent = progress.progressPercent)

        mutableListState.value = (mutableListState.value as? CourseListUiState.Content)
            ?.let { state ->
                state.copy(courses = state.courses.map { if (it.id == progress.courseId) it.updated() else it })
            }
            ?: mutableListState.value
        mutableCurriculumState.value = (mutableCurriculumState.value as? CurriculumUiState.Content)
            ?.let { state -> state.copy(course = state.course.updated()) }
            ?: mutableCurriculumState.value
        mutableLessonState.value = (mutableLessonState.value as? LessonUiState.Content)
            ?.let { state -> state.copy(course = state.course.updated()) }
            ?: mutableLessonState.value
    }

    private fun launchQuiz(request: QuizWebLaunchRequest) {
        quizLaunchJob?.cancel()
        quizLaunchJob = scope.launch {
            mutableQuizWebState.value = QuizWebUiState.Loading
            when (val result = repository.createQuizWebLaunch(request)) {
                is ApiResult.Success -> {
                    mutableQuizWebState.value = if (
                        result.value.courseId == request.courseId && result.value.quizId == request.quizId
                    ) {
                        QuizWebUiState.Ready(result.value)
                    } else {
                        QuizWebUiState.Error(userFacingStrings.resolve(Res.string.error_open_quiz))
                    }
                }
                is ApiResult.Failure -> {
                    mutableQuizWebState.value = QuizWebUiState.Error(
                        result.error.localizedUserMessageForState(userFacingStrings),
                    )
                }
            }
        }
    }

    private fun clearQuizLaunch() {
        quizLaunchJob?.cancel()
        quizLaunchJob = null
        pendingQuizLaunch = null
        mutableQuizWebState.value = QuizWebUiState.Hidden
    }

    private fun authorizePlayback(course: Course, lesson: Lesson) {
        playbackJob?.cancel()
        playbackJob = scope.launch {
            mutablePlaybackState.value = PlaybackUiState.Loading
            when (val result = repository.authorizePlayback(course.id, lesson.id)) {
                is ApiResult.Success -> {
                    val playbackUrl = result.value.validatedEmbedUrlFor(
                        lesson = lesson,
                        currentEpochSeconds = Clock.System.now().epochSeconds,
                    )
                    mutablePlaybackState.value = if (playbackUrl == null) {
                        PlaybackUiState.Error(
                            userFacingStrings.resolve(Res.string.error_video_authorization),
                        )
                    } else {
                        PlaybackUiState.Playable(playbackUrl)
                    }
                }
                is ApiResult.Failure -> {
                    mutablePlaybackState.value = PlaybackUiState.Error(
                        userFacingStrings.resolve(Res.string.error_video_authorization),
                    )
                }
            }
        }
    }

    private fun clearPlayback() {
        playbackJob?.cancel()
        playbackJob = null
        mutablePlaybackState.value = PlaybackUiState.Hidden
    }

    private fun clearCompletion() {
        completionJob?.cancel()
        completionJob = null
        pendingCompletion = null
        mutableCompletionState.value = LessonCompletionUiState.Hidden
    }

    private fun clearForSignedOutSession() {
        listJob?.cancel()
        curriculumJob?.cancel()
        lessonJob?.cancel()
        clearPlayback()
        clearCompletion()
        clearQuizLaunch()
        recentlyOpenedCourseId = null
        mutableLiveClassesState.value = DashboardSectionUiState.Loading
        mutableEventsState.value = DashboardSectionUiState.Loading
        mutableSupportActionsState.value = DashboardSectionUiState.Loading
        quizCleanupJob?.cancel()
        quizCleanupJob = null
        mutableListState.value = CourseListUiState.SignedOut
        restorationJob?.cancel()
        restorationJob = null
        pendingRestoredNavigation = null
        mutableRestoring.value = false
        mutableRecoveryDismissed.value = false
        mutableRestorationError.value = null
        mutableInterruptedQuizCleanup.value = false
        navigationTouched = false
        backStack.clear()
        backStack.add(LearningRoute.Dashboard)
        mutableCurriculumState.value = CurriculumUiState.Hidden
        mutableLessonState.value = LessonUiState.Hidden
    }

    private companion object {
    }
}

private fun List<Course>.promote(courseId: Long?): List<Course> {
    val index = indexOfFirst { it.id == courseId }
    if (index <= 0) return this
    return toMutableList().also { courses -> courses.add(0, courses.removeAt(index)) }
}

private data class PendingCompletion(
    val lessonId: Long,
    val request: ProgressRequest,
)

private fun ProgressResponse.matches(pending: PendingCompletion): Boolean =
    accepted &&
        lessonId == pending.lessonId &&
        courseId == pending.request.courseId &&
        completed == pending.request.completed &&
        positionSeconds == pending.request.positionSeconds &&
        clientEventId == pending.request.clientEventId

private fun CourseProgress.isValidFor(expectedCourseId: Long): Boolean =
    courseId == expectedCourseId && progressPercent in 0..100

internal fun newProgressEventId(random: Random = Random.Default): String = buildString {
    append("progress:")
    repeat(16) {
        val value = random.nextInt(256)
        append(HEX_DIGITS[value ushr 4])
        append(HEX_DIGITS[value and 0x0f])
    }
}

internal fun newQuizLaunchRequestId(random: Random = Random.Default): String = buildString {
    append("quiz-launch:")
    repeat(16) {
        val value = random.nextInt(256)
        append(HEX_DIGITS[value ushr 4])
        append(HEX_DIGITS[value and 0x0f])
    }
}

private const val HEX_DIGITS = "0123456789abcdef"

internal fun PlaybackResponse.validatedEmbedUrlFor(
    lesson: Lesson,
    currentEpochSeconds: Long,
): String? {
    val video = lesson.video ?: return null
    val url = runCatching { io.ktor.http.Url(playbackUrl) }.getOrNull() ?: return null
    if (
        lessonId != lesson.id ||
        libraryId != video.libraryId ||
        videoId != video.videoId ||
        tokenScheme != "embed_sha256_hex" ||
        token.isBlank() ||
        expires <= currentEpochSeconds ||
        url.parameters.getAll("token") != listOf(token) ||
        url.parameters.getAll("expires") != listOf(expires.toString())
    ) {
        return null
    }
    return playbackUrl.takeIf { isExpectedBunnyEmbedUrl(it, libraryId, videoId) }
}
