package com.example.academyapp.course

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.size
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material3.Surface
import com.example.academyapp.api.model.Course
import com.example.academyapp.api.model.CourseSectionHeading
import com.example.academyapp.api.model.DashboardEvent
import com.example.academyapp.api.model.DashboardEventStatus
import com.example.academyapp.api.model.DashboardLiveClass
import com.example.academyapp.api.model.DashboardSupportAction
import com.example.academyapp.api.model.Lesson
import com.example.academyapp.api.model.LessonType
import com.example.academyapp.api.model.Quiz
import com.example.academyapp.design.AcademyDimensions
import com.example.academyapp.design.AcademyColors
import com.example.academyapp.design.AcademyNavigationIcons
import com.example.academyapp.design.AcademyShapes
import com.example.academyapp.design.AcademySpacing
import com.example.academyapp.design.AcademyTheme
import com.example.academyapp.design.components.AcademyAdaptivePane
import com.example.academyapp.design.components.AcademyEmptyState
import com.example.academyapp.design.components.AcademyErrorState
import com.example.academyapp.design.components.AcademyLoadingState
import com.example.academyapp.design.components.AcademyProgressIndicator
import com.example.academyapp.design.components.AcademyStateContainer
import com.example.academyapp.design.components.AcademyCourseCard
import com.example.academyapp.design.components.AcademyLessonRow
import com.example.academyapp.design.components.AcademyLessonSectionHeader
import com.example.academyapp.design.components.AcademyTopicRow
import com.example.academyapp.design.components.AcademyScreenTransition
import com.example.academyapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource

@Composable
fun CourseRoute(
    viewModel: CourseListViewModel,
    quizWebSessionController: QuizWebSessionController,
) {
    val listState by viewModel.listState.collectAsStateWithLifecycle()
    val destination by viewModel.destination.collectAsStateWithLifecycle()
    val curriculumState by viewModel.curriculumState.collectAsStateWithLifecycle()
    val lessonState by viewModel.lessonState.collectAsStateWithLifecycle()
    val playbackState by viewModel.playbackState.collectAsStateWithLifecycle()
    val completionState by viewModel.completionState.collectAsStateWithLifecycle()
    val quizWebState by viewModel.quizWebState.collectAsStateWithLifecycle()
    val liveClassesState by viewModel.liveClassesState.collectAsStateWithLifecycle()
    val eventsState by viewModel.eventsState.collectAsStateWithLifecycle()
    val supportActionsState by viewModel.supportActionsState.collectAsStateWithLifecycle()

    AcademyScreenTransition(
        targetState = destination,
        stateOrder = { route ->
            when (route) {
                CourseDestination.Dashboard -> 0
                CourseDestination.Courses -> 1
                is CourseDestination.Curriculum -> 2
                is CourseDestination.LessonDetail -> 3
                is CourseDestination.QuizWeb -> 4
            }
        },
        modifier = Modifier.fillMaxSize(),
    ) { route ->
        when (route) {
            CourseDestination.Dashboard -> CourseDashboardScreen(
                state = listState,
                supportSiteHost = viewModel.supportSiteHost,
                liveClassesState = liveClassesState,
                eventsState = eventsState,
                supportActionsState = supportActionsState,
                onRetry = viewModel::retryCourses,
                onRetryLiveClasses = viewModel::retryLiveClasses,
                onRetryEvents = viewModel::retryEvents,
                onRetrySupportActions = viewModel::retrySupportActions,
                onCourseSelected = viewModel::openCourse,
                onViewAllCourses = viewModel::showAllCourses,
            )
            CourseDestination.Courses -> CourseListScreen(
                state = listState,
                onBack = viewModel::showDashboard,
                onRetry = viewModel::retryCourses,
                onCourseSelected = viewModel::openCourse,
            )
            is CourseDestination.Curriculum -> CurriculumScreen(
                state = curriculumState,
                onBack = viewModel::showCourses,
                onRetry = viewModel::retryCurriculum,
                onLessonSelected = viewModel::openLesson,
                onQuizSelected = viewModel::openQuiz,
            )
            is CourseDestination.LessonDetail -> LessonScreen(
                state = lessonState,
                playbackState = playbackState,
                completionState = completionState,
                onBack = viewModel::showCurriculum,
                onRetry = viewModel::retryLesson,
                onRetryPlayback = viewModel::retryPlayback,
                onMarkComplete = viewModel::markLessonComplete,
                onRetryCompletion = viewModel::retryLessonCompletion,
            )
            is CourseDestination.QuizWeb -> QuizWebScreen(
                destination = route,
                state = quizWebState,
                sessionController = quizWebSessionController,
                onBack = viewModel::returnFromQuiz,
                onRetry = viewModel::retryQuizLaunch,
                onRetryCleanup = viewModel::retryQuizCleanup,
            )
        }
    }
}

@Composable
private fun CourseDashboardScreen(
    state: CourseListUiState,
    supportSiteHost: String? = null,
    liveClassesState: DashboardSectionUiState<DashboardLiveClass>,
    eventsState: DashboardSectionUiState<DashboardEvent>,
    supportActionsState: DashboardSectionUiState<DashboardSupportAction>,
    onRetry: () -> Unit,
    onRetryLiveClasses: () -> Unit,
    onRetryEvents: () -> Unit,
    onRetrySupportActions: () -> Unit,
    onCourseSelected: (Course) -> Unit,
    onViewAllCourses: () -> Unit,
) {
    AcademyAdaptivePane(
        primary = { twoPane ->
            Column(
                modifier = if (twoPane) Modifier.fillMaxSize().verticalScroll(rememberScrollState()) else Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(AcademySpacing.Medium),
            ) {
                Text(
                    stringResource(Res.string.nav_my_learning),
                    style = if (twoPane) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.headlineMedium,
                )
                if (state is CourseListUiState.Content) {
                    Text(
                        stringResource(Res.string.dashboard_course_preview_title),
                        style = if (twoPane) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleLarge,
                    )
                }
            }
        },
        secondary = {
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(AcademySpacing.Large),
            ) {
                when (state) {
            CourseListUiState.SignedOut -> AcademyEmptyState(
                title = stringResource(Res.string.courses_sign_in_required),
                message = stringResource(Res.string.courses_sign_in_message),
            )
            CourseListUiState.Loading -> AcademyLoadingState(stringResource(Res.string.courses_loading))
            CourseListUiState.Empty -> AcademyEmptyState(
                title = stringResource(Res.string.courses_empty_title),
                message = stringResource(Res.string.courses_empty_message),
            )
            is CourseListUiState.Error -> AcademyErrorState(
                title = stringResource(Res.string.courses_load_error_title),
                message = state.message,
                onAction = onRetry,
            )
            is CourseListUiState.Content -> {
                dashboardPreviewCourses(state.courses).forEach { course ->
                    AcademyCourseCard(course = course, onClick = { onCourseSelected(course) })
                }
                OutlinedButton(onClick = onViewAllCourses, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(Res.string.dashboard_view_all_courses))
                }
            }
                }
                DashboardLiveClassesSection(liveClassesState, onRetryLiveClasses)
                DashboardEventsSection(eventsState, onRetryEvents)
                DashboardSupportActionsSection(supportActionsState, supportSiteHost, onRetrySupportActions)
            }
        }
    )
}

@Composable
private fun DashboardLiveClassesSection(
    state: DashboardSectionUiState<DashboardLiveClass>,
    onRetry: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(AcademySpacing.Medium)) {
        Text(stringResource(Res.string.dashboard_live_classes), style = MaterialTheme.typography.titleLarge)
        when (state) {
            DashboardSectionUiState.Loading -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AcademySpacing.Small)) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                Text(stringResource(Res.string.dashboard_live_classes_loading))
            }
            DashboardSectionUiState.Empty -> Text(stringResource(Res.string.dashboard_live_classes_empty))
            DashboardSectionUiState.Unavailable -> SectionUnavailable(stringResource(Res.string.dashboard_section_unavailable), onRetry)
            is DashboardSectionUiState.Error -> SectionUnavailable(state.message, onRetry)
            is DashboardSectionUiState.Content -> state.items.forEach { item -> DashboardLiveClassCard(item) }
        }
    }
}

@Composable
private fun DashboardLiveClassCard(item: DashboardLiveClass) {
    val uriHandler = LocalUriHandler.current
    val safeJoinUrl = item.joinUrl?.takeIf { item.joinable }?.let(::safeZoomJoinUrl)
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(AcademySpacing.Medium), verticalArrangement = Arrangement.spacedBy(AcademySpacing.Small)) {
            Text(item.title, style = MaterialTheme.typography.titleMedium)
            Text(item.courseTitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(formatDashboardUtcTime(item.startAt), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (safeJoinUrl != null) {
                OutlinedButton(onClick = { runCatching { uriHandler.openUri(safeJoinUrl) } }) {
                    Text(stringResource(Res.string.dashboard_join_class))
                }
            }
        }
    }
}

@Composable
private fun DashboardEventsSection(
    state: DashboardSectionUiState<DashboardEvent>,
    onRetry: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(AcademySpacing.Medium)) {
        Text(stringResource(Res.string.dashboard_events), style = MaterialTheme.typography.titleLarge)
        when (state) {
            DashboardSectionUiState.Loading -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AcademySpacing.Small)) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                Text(stringResource(Res.string.dashboard_events_loading))
            }
            DashboardSectionUiState.Empty -> Text(stringResource(Res.string.dashboard_events_empty))
            DashboardSectionUiState.Unavailable -> SectionUnavailable(stringResource(Res.string.dashboard_section_unavailable), onRetry)
            is DashboardSectionUiState.Error -> SectionUnavailable(state.message, onRetry)
            is DashboardSectionUiState.Content -> state.items.forEach { item -> DashboardEventCard(item) }
        }
    }
}

@Composable
private fun DashboardEventCard(item: DashboardEvent) {
    val uriHandler = LocalUriHandler.current
    val statusLabel = when (item.status) {
        DashboardEventStatus.SCHEDULED -> stringResource(Res.string.dashboard_event_status_scheduled)
        DashboardEventStatus.STARTING -> stringResource(Res.string.dashboard_event_status_starting)
        DashboardEventStatus.LIVE -> stringResource(Res.string.dashboard_event_status_live)
    }
    val safeJoinUrl = safeEventJoinUrl(item.joinable, item.joinUrl)
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(AcademySpacing.Medium), verticalArrangement = Arrangement.spacedBy(AcademySpacing.Small)) {
            Text(statusLabel, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            Text(item.title, style = MaterialTheme.typography.titleMedium)
            Text(
                "${formatDashboardUtcTime(item.startAt)} – ${formatDashboardUtcTime(item.endAt)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (safeJoinUrl != null && item.actionLabel.isNotBlank()) {
                OutlinedButton(onClick = { runCatching { uriHandler.openUri(safeJoinUrl) } }) {
                    Text(item.actionLabel)
                }
            }
        }
    }
}

@Composable
private fun DashboardSupportActionsSection(
    state: DashboardSectionUiState<DashboardSupportAction>,
    supportSiteHost: String?,
    onRetry: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(AcademySpacing.Medium)) {
        Text(stringResource(Res.string.dashboard_support), style = MaterialTheme.typography.titleLarge)
        when (state) {
            DashboardSectionUiState.Loading -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AcademySpacing.Small)) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                Text(stringResource(Res.string.dashboard_support_loading))
            }
            DashboardSectionUiState.Empty -> Text(stringResource(Res.string.dashboard_support_empty))
            DashboardSectionUiState.Unavailable -> SectionUnavailable(stringResource(Res.string.dashboard_section_unavailable), onRetry)
            is DashboardSectionUiState.Error -> SectionUnavailable(state.message, onRetry)
            is DashboardSectionUiState.Content -> state.items.forEach { action -> DashboardSupportActionCard(action, supportSiteHost) }
        }
    }
}

@Composable
private fun DashboardSupportActionCard(action: DashboardSupportAction, supportSiteHost: String?) {
    val uriHandler = LocalUriHandler.current
    val safeUrl = safeSupportActionUrl(action.url, supportSiteHost)
    if (safeUrl != null) {
        OutlinedButton(onClick = { runCatching { uriHandler.openUri(safeUrl) } }, modifier = Modifier.fillMaxWidth()) {
            Text(action.label)
        }
    }
}

@Composable
private fun SectionUnavailable(message: String, onRetry: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(AcademySpacing.XSmall)) {
        Text(message, color = MaterialTheme.colorScheme.error)
        OutlinedButton(onClick = onRetry) { Text(stringResource(Res.string.try_again)) }
    }
}

private fun formatDashboardUtcTime(value: String): String {
    val date = value.substringBefore('T')
    val time = value.substringAfter('T', "").take(5)
    return if (date.isNotBlank() && time.length == 5) "$date · $time UTC" else value
}

internal fun safeZoomJoinUrl(url: String): String? = safeHttpsUrl(url) { host ->
    host == "zoom.us" || host.endsWith(".zoom.us") || host == "zoom.com" || host.endsWith(".zoom.com")
}

internal fun safeEventUrl(url: String): String? = safeHttpsUrl(url) { true }

internal fun safeEventJoinUrl(joinable: Boolean, url: String?): String? =
    url?.takeIf { joinable }?.let(::safeEventUrl)

internal fun safeSupportActionUrl(url: String, configuredSiteHost: String? = null): String? = safeHttpsUrl(url) { host ->
    host in setOf("academy.online", "www.academy.online", "wa.me", "api.whatsapp.com") ||
        (configuredSiteHost?.lowercase() == host)
}

private fun safeHttpsUrl(url: String, hostAllowed: (String) -> Boolean): String? {
    if (url.length > 2048 || url.any(Char::isWhitespace)) return null
    val match = Regex("^https://([^/?#]+)(?:[/?#].*)?$", RegexOption.IGNORE_CASE).matchEntire(url) ?: return null
    val authority = match.groupValues[1]
    if ('@' in authority) return null
    if (authority.count { it == ':' } > 1) return null
    val host = authority.substringBefore(':').lowercase()
    val port = authority.substringAfter(':', "")
    val hasPort = ':' in authority
    if (host.isBlank() || (hasPort && port != "443") || !hostAllowed(host)) return null
    return url
}

@Composable
private fun CourseListScreen(
    state: CourseListUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onCourseSelected: (Course) -> Unit,
) {
    AcademyAdaptivePane(
        primary = { twoPane ->
            Column(
                modifier = if (twoPane) Modifier.fillMaxSize().verticalScroll(rememberScrollState()) else Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(AcademySpacing.Large),
            ) {
                OutlinedButton(onClick = onBack) {
                    Text(stringResource(Res.string.back_to_dashboard))
                }
                Text(
                    stringResource(Res.string.dashboard_all_courses_title),
                    style = if (twoPane) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.headlineMedium,
                )
            }
        },
        secondary = {
            when (state) {
            CourseListUiState.SignedOut -> AcademyEmptyState(
                title = stringResource(Res.string.courses_sign_in_required),
                message = stringResource(Res.string.courses_sign_in_message),
            )
            CourseListUiState.Loading -> AcademyLoadingState(stringResource(Res.string.courses_loading))
            CourseListUiState.Empty -> AcademyEmptyState(
                title = stringResource(Res.string.courses_empty_title),
                message = stringResource(Res.string.courses_empty_message),
            )
            is CourseListUiState.Error -> AcademyErrorState(
                title = stringResource(Res.string.courses_load_error_title),
                message = state.message,
                onAction = onRetry,
            )
            is CourseListUiState.Content -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = AcademySpacing.Large),
                verticalArrangement = Arrangement.spacedBy(AcademySpacing.MediumSmall),
            ) {
                items(coursesForLearning(state.courses), key = Course::id) { course ->
                    AcademyCourseCard(course = course, onClick = { onCourseSelected(course) })
                }
            }
        }
        },
    )
}

@Composable
private fun CurriculumScreen(
    state: CurriculumUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onLessonSelected: (Lesson) -> Unit,
    onQuizSelected: (Quiz) -> Unit,
) {
    AcademyAdaptivePane(
        primary = { twoPane ->
            Column(
                modifier = if (twoPane) Modifier.fillMaxSize().verticalScroll(rememberScrollState()) else Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(AcademySpacing.MediumSmall),
            ) {
                OutlinedButton(
                    onClick = onBack,
                    shape = AcademyShapes.Pill,
                    contentPadding = PaddingValues(horizontal = AcademySpacing.Medium, vertical = AcademySpacing.Small),
                ) { Text(stringResource(Res.string.back_to_courses)) }
                val course = when (state) {
                    CurriculumUiState.Hidden -> null
                    is CurriculumUiState.Loading -> state.course
                    is CurriculumUiState.Empty -> state.course
                    is CurriculumUiState.Error -> state.course
                    is CurriculumUiState.Content -> state.course
                }
                course?.let {
                    Text(
                        stringResource(
                            if (state is CurriculumUiState.Loading) Res.string.curriculum_loading else Res.string.curriculum,
                        ),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        it.title,
                        style = if (twoPane) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineSmall,
                    )
                    if (state is CurriculumUiState.Content) {
                        val completedLessonCount = state.lessons.count {
                            it.type == LessonType.LESSON && it.completed
                        }
                        Text(
                            stringResource(
                                if (completedLessonCount == 1) Res.string.curriculum_completed_lesson
                                else Res.string.curriculum_completed_lessons,
                                completedLessonCount,
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        },
        secondary = {
            when (state) {
                CurriculumUiState.Hidden -> AcademyStateContainer {
                    AcademyLoadingState(stringResource(Res.string.courses_loading))
                }
                is CurriculumUiState.Loading -> AcademyStateContainer {
                    AcademyLoadingState(stringResource(Res.string.curriculum_loading))
                }
                is CurriculumUiState.Empty -> AcademyStateContainer {
                    AcademyEmptyState(
                        title = stringResource(Res.string.curriculum_empty_title),
                        message = stringResource(Res.string.curriculum_empty_message),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    )
                }
                is CurriculumUiState.Error -> AcademyStateContainer {
                    AcademyErrorState(
                        title = stringResource(Res.string.curriculum_load_error_title),
                        message = state.message,
                        onAction = onRetry,
                        modifier = Modifier.widthIn(max = AcademyDimensions.ReadingMaxWidth),
                    )
                }
                is CurriculumUiState.Content -> {
                val sections = state.lessons.filter { it.type == LessonType.LESSON }
                val topicsBySection = remember(state.course.id, state.lessons) {
                    state.lessons
                        .filter { it.type == LessonType.TOPIC && it.parentLessonId != null }
                        .groupBy { it.parentLessonId!! }
                }
                val quizzesBySection = remember(state.course.id, state.quizzes) {
                    state.quizzes
                        .filter { it.parentLessonId != null }
                        .groupBy { it.parentLessonId!! }
                }
                val courseQuizzes = remember(state.course.id, state.quizzes) {
                    state.quizzes.filter { it.parentLessonId == null }
                }
                val sectionHeadingsBeforeStep = remember(state.course.id, state.sectionHeadings) {
                    state.sectionHeadings.groupBy { it.beforeStepId }
                }
                val mostRecentViewedSectionId = remember(state.course.id, state.course.progressPercent, state.lessons) {
                    if (state.course.progressPercent !in 1..99) {
                        null
                    } else {
                        state.lessons
                            .asSequence()
                            .filter { it.type == LessonType.TOPIC && it.parentLessonId != null }
                            .filter { !it.lastViewedAt.isNullOrBlank() }
                            .maxByOrNull { it.lastViewedAt.orEmpty() }
                            ?.parentLessonId
                    }
                }
                val mostRecentViewedTopicId = remember(state.course.id, state.course.progressPercent, state.lessons) {
                    if (state.course.progressPercent !in 1..99) {
                        null
                    } else {
                        state.lessons
                            .asSequence()
                            .filter { it.type == LessonType.TOPIC && it.parentLessonId != null }
                            .filter { !it.lastViewedAt.isNullOrBlank() }
                            .maxByOrNull { it.lastViewedAt.orEmpty() }
                            ?.id
                    }
                }
                var expandedSectionId by remember(state.course.id, state.course.progressPercent, state.lessons) {
                    mutableStateOf(mostRecentViewedSectionId)
                }
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = AcademySpacing.Large),
                    verticalArrangement = Arrangement.spacedBy(AcademySpacing.MediumSmall),
                ) {
                    sections.forEach { section ->
                        val topics = topicsBySection[section.id].orEmpty()
                        val sectionQuizzes = quizzesBySection[section.id].orEmpty()
                        sectionHeadingsBeforeStep[section.id].orEmpty().forEach { heading ->
                            item(key = "curriculum-heading:${heading.beforeStepId}:${heading.title}") {
                                Text(
                                    heading.title.uppercase(),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = AcademySpacing.Small),
                                )
                            }
                        }
                        if (topics.isEmpty()) {
                            item(key = "lesson:${section.id}") {
                                Column(verticalArrangement = Arrangement.spacedBy(AcademySpacing.Small)) {
                                    AcademyLessonRow(
                                        section.title,
                                        lessonSummary(section),
                                        section.completed,
                                        { onLessonSelected(section) },
                                        hasVideo = section.video != null,
                                    )
                                    sectionQuizzes.forEach { quiz ->
                                        QuizRow(quiz = quiz, onClick = { onQuizSelected(quiz) })
                                    }
                                }
                            }
                        } else {
                            item(key = "section:${section.id}") {
                                Column(verticalArrangement = Arrangement.spacedBy(AcademySpacing.XSmall)) {
                                    AcademyLessonSectionHeader(
                                        title = section.title,
                                        expanded = section.id == expandedSectionId,
                                        showNext = topics.any {
                                            it.id == mostRecentViewedTopicId && !it.completed
                                        },
                                        onToggle = {
                                            expandedSectionId = if (section.id == expandedSectionId) null else section.id
                                        },
                                    )
                                    AnimatedVisibility(
                                        visible = section.id == expandedSectionId,
                                        enter = fadeIn(tween(180)) + expandVertically(
                                            animationSpec = tween(220),
                                            expandFrom = Alignment.Top,
                                        ),
                                        exit = fadeOut(tween(130)) + shrinkVertically(
                                            animationSpec = tween(180),
                                            shrinkTowards = Alignment.Top,
                                        ),
                                    ) {
                                        Column(verticalArrangement = Arrangement.spacedBy(AcademySpacing.XSmall)) {
                                            topics.forEach { topic ->
                                                AcademyTopicRow(
                                                    topic.title,
                                                    lessonSummary(topic),
                                                    topic.completed,
                                                    isNext = topic.id == mostRecentViewedTopicId && !topic.completed,
                                                    onClick = { onLessonSelected(topic) },
                                                    modifier = Modifier.padding(start = AcademySpacing.Small),
                                                )
                                            }
                                            sectionQuizzes.forEach { quiz ->
                                                QuizRow(
                                                    quiz = quiz,
                                                    onClick = { onQuizSelected(quiz) },
                                                    modifier = Modifier.padding(start = AcademySpacing.Medium),
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    state.lessons.filter { it.type == LessonType.TOPIC && it.parentLessonId == null }.forEach { topic ->
                        item(key = "orphan:${topic.id}") {
                            AcademyLessonRow(topic.title, lessonSummary(topic), topic.completed, { onLessonSelected(topic) })
                        }
                    }
                    items(courseQuizzes, key = { "quiz:${it.id}" }) { quiz ->
                        QuizRow(quiz = quiz, onClick = { onQuizSelected(quiz) })
                    }
                }
            }
            }
        },
    )
}

@Composable
private fun QuizRow(
    quiz: Quiz,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = AcademyShapes.ListRow,
        colors = CardDefaults.cardColors(containerColor = AcademyColors.OrangeContainer),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(AcademySpacing.MediumSmall),
            horizontalArrangement = Arrangement.spacedBy(AcademySpacing.MediumSmall),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(28.dp),
                shape = AcademyShapes.Pill,
                color = AcademyColors.BrandOrange,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = AcademyNavigationIcons.QuizPencil,
                        contentDescription = null,
                        tint = AcademyColors.White,
                        modifier = Modifier.size(17.dp),
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(AcademySpacing.XSmall),
            ) {
                Text(quiz.title, style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = stringResource(
                        if (quiz.completed) Res.string.quiz_completed_status else Res.string.quiz_incomplete_status,
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (quiz.completed) MaterialTheme.colorScheme.onSurfaceVariant
                    else AcademyColors.BrandOrangeDark,
                )
            }
            Icon(
                imageVector = AcademyNavigationIcons.ChevronRight,
                contentDescription = null,
                tint = AcademyColors.BrandOrangeDark,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun QuizWebScreen(
    destination: CourseDestination.QuizWeb,
    state: QuizWebUiState,
    sessionController: QuizWebSessionController,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onRetryCleanup: () -> Unit,
) {
    AcademyAdaptivePane(
        primary = { twoPane ->
            Column(
                modifier = if (twoPane) Modifier.fillMaxSize().verticalScroll(rememberScrollState()) else Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(AcademySpacing.MediumSmall),
            ) {
                OutlinedButton(onClick = onBack) { Text(stringResource(Res.string.back_to_curriculum)) }
                Text(
                    destination.course.title,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    destination.quiz.title,
                    style = if (twoPane) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineSmall,
                )
                if (!twoPane) HorizontalDivider()
            }
        },
        secondary = {
            when (state) {
            QuizWebUiState.Hidden,
            QuizWebUiState.Loading,
            QuizWebUiState.Closing,
            -> AcademyStateContainer { AcademyLoadingState(stringResource(Res.string.quiz_loading)) }
            is QuizWebUiState.Error -> AcademyStateContainer {
                AcademyErrorState(
                    title = stringResource(Res.string.quiz_load_error_title),
                    message = state.message,
                    onAction = onRetry,
                    modifier = Modifier.widthIn(max = AcademyDimensions.ReadingMaxWidth),
                )
            }
            is QuizWebUiState.CleanupError -> AcademyStateContainer {
                AcademyErrorState(
                    title = stringResource(Res.string.quiz_close_error_title),
                    message = state.message,
                    onAction = onRetryCleanup,
                    modifier = Modifier.widthIn(max = AcademyDimensions.ReadingMaxWidth),
                )
            }
            is QuizWebUiState.Ready -> {
                var pageReady by remember(state.launch.expiresAt) { mutableStateOf(false) }
                var pageFailed by remember(state.launch.expiresAt) { mutableStateOf(false) }
                if (pageFailed) {
                    AcademyStateContainer {
                        AcademyErrorState(
                            title = stringResource(Res.string.quiz_page_load_error_title),
                            message = stringResource(Res.string.quiz_page_load_error_message),
                            onAction = onRetry,
                            modifier = Modifier.widthIn(max = AcademyDimensions.ReadingMaxWidth),
                        )
                    }
                } else {
                    Box(modifier = Modifier.fillMaxSize()) {
                        AcademyQuizWebView(
                            launch = state.launch,
                            sessionController = sessionController,
                            modifier = Modifier.fillMaxSize(),
                            onPageReady = { pageReady = true },
                            onPageError = { pageFailed = true },
                        )
                        if (!pageReady) {
                            AcademyStateContainer { AcademyLoadingState(stringResource(Res.string.quiz_loading)) }
                        }
                    }
                }
            }
        }
        },
    )
}

@Composable
private fun LessonScreen(
    state: LessonUiState,
    playbackState: PlaybackUiState,
    completionState: LessonCompletionUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onRetryPlayback: () -> Unit,
    onMarkComplete: () -> Unit,
    onRetryCompletion: () -> Unit,
) {
    AcademyAdaptivePane(
        primary = { twoPane ->
            Column(
                modifier = if (twoPane) Modifier.fillMaxSize().verticalScroll(rememberScrollState()) else Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(AcademySpacing.MediumSmall),
            ) {
                OutlinedButton(onClick = onBack) { Text(stringResource(Res.string.back_to_curriculum)) }
                when (state) {
                    LessonUiState.Hidden -> Unit
                    is LessonUiState.Loading -> LessonMetadata(state.course, state.selectedLesson.title, twoPane)
                    is LessonUiState.Error -> LessonMetadata(state.course, state.selectedLesson.title, twoPane)
                    is LessonUiState.Content -> LessonMetadata(state.course, state.lesson.title, twoPane)
                }
                if (!twoPane) HorizontalDivider()
            }
        },
        secondary = {
            when (state) {
                LessonUiState.Hidden -> AcademyStateContainer {
                    AcademyLoadingState(stringResource(Res.string.lesson_loading))
                }
                is LessonUiState.Loading -> AcademyStateContainer {
                    AcademyLoadingState(stringResource(Res.string.lesson_loading))
                }
                is LessonUiState.Error -> AcademyStateContainer {
                    AcademyErrorState(
                        title = stringResource(Res.string.lesson_load_error_title),
                        message = state.message,
                        onAction = onRetry,
                        modifier = Modifier.widthIn(max = AcademyDimensions.ReadingMaxWidth),
                    )
                }
                is LessonUiState.Content -> LessonDetails(
                    lesson = state.lesson,
                    playbackState = playbackState,
                    completionState = completionState,
                    onRetryPlayback = onRetryPlayback,
                    onMarkComplete = onMarkComplete,
                    onRetryCompletion = onRetryCompletion,
                )
            }
        },
    )
}

@Composable
private fun LessonMetadata(course: Course, lessonTitle: String, twoPane: Boolean) {
    Text(course.title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
    Text(
        lessonTitle,
        style = if (twoPane) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineSmall,
    )
}

@Composable
private fun LessonDetails(
    lesson: Lesson,
    playbackState: PlaybackUiState,
    completionState: LessonCompletionUiState,
    onRetryPlayback: () -> Unit,
    onMarkComplete: () -> Unit,
    onRetryCompletion: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(AcademySpacing.Medium),
    ) {
        val uriHandler = LocalUriHandler.current
        when (playbackState) {
            PlaybackUiState.Hidden,
            PlaybackUiState.Loading,
            -> AcademyStateContainer { AcademyLoadingState(stringResource(Res.string.video_loading)) }
            PlaybackUiState.NoVideo -> AcademyStateContainer {
                AcademyEmptyState(
                    title = stringResource(Res.string.no_video_title),
                    message = stringResource(Res.string.no_video_message),
                    horizontalAlignment = Alignment.CenterHorizontally,
                )
            }
            is PlaybackUiState.Error -> AcademyStateContainer {
                AcademyErrorState(
                    title = stringResource(Res.string.video_load_error_title),
                    message = playbackState.message,
                    onAction = onRetryPlayback,
                    modifier = Modifier.widthIn(max = AcademyDimensions.ReadingMaxWidth),
                )
            }
            is PlaybackUiState.Playable -> BunnyEmbedPlayer(
                playbackUrl = playbackState.playbackUrl,
                modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f),
            )
        }
        LessonCompletionControls(
            lesson = lesson,
            state = completionState,
            onMarkComplete = onMarkComplete,
            onRetry = onRetryCompletion,
        )
        if (lesson.resources.isNotEmpty()) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(AcademySpacing.Medium),
                    verticalArrangement = Arrangement.spacedBy(AcademySpacing.Small),
                ) {
                    Text(stringResource(Res.string.lesson_materials), style = MaterialTheme.typography.titleMedium)
                    lesson.resources.forEach { resource ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(resource.title, style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    stringResource(
                                        if (resource.mediaType == "application/pdf") {
                                            Res.string.lesson_material_pdf
                                        } else {
                                            Res.string.lesson_material_powerpoint
                                        },
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                            TextButton(
                                onClick = { uriHandler.openUri(resource.url) },
                                enabled = isLessonResourceUrlOpenable(resource.url),
                            ) {
                                Text(stringResource(Res.string.lesson_material_open))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LessonCompletionControls(
    lesson: Lesson,
    state: LessonCompletionUiState,
    onMarkComplete: () -> Unit,
    onRetry: () -> Unit,
) {
    val notice = lessonCompletionNotice(state)
    if (notice != null) {
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Text(
                stringResource(
                    when (notice) {
                        LessonCompletionNotice.Saved -> Res.string.lesson_marked_complete
                        LessonCompletionNotice.AlreadyCompleted -> Res.string.lesson_is_complete
                    },
                ),
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
            )
        }
    } else {
        when (state) {
            LessonCompletionUiState.Hidden -> Unit
            LessonCompletionUiState.Available -> Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Button(onClick = onMarkComplete) {
                    Text(stringResource(Res.string.mark_complete))
                }
            }
            LessonCompletionUiState.Saving -> Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Button(onClick = {}, enabled = false) {
                    Text(stringResource(Res.string.saving))
                }
            }
            LessonCompletionUiState.Saved,
            LessonCompletionUiState.AlreadyCompleted,
            -> Unit
            is LessonCompletionUiState.Error -> Column(
                verticalArrangement = Arrangement.spacedBy(AcademySpacing.Small),
            ) {
                Text(state.message, color = MaterialTheme.colorScheme.error)
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Button(onClick = onRetry) {
                        Text(stringResource(if (lesson.completed) Res.string.retry_sync else Res.string.try_again))
                    }
                }
            }
        }
    }
}

@Composable
private fun lessonSummary(lesson: Lesson): String = buildList {
    add(stringResource(if (lesson.type == LessonType.TOPIC) Res.string.lesson_summary_topic else Res.string.lesson_summary_lesson))
    lesson.durationSeconds?.let { add(durationLabel(it)) }
    if (lesson.video != null) add(stringResource(Res.string.lesson_summary_video))
    if (lesson.completed) add(stringResource(Res.string.completed))
}.joinToString(" · ")

@Composable
private fun durationLabel(seconds: Int): String {
    val safeSeconds = seconds.coerceAtLeast(0)
    val minutes = safeSeconds / 60
    val remainder = safeSeconds % 60
    return if (minutes > 0) {
        stringResource(Res.string.duration_minutes_seconds, minutes, remainder)
    } else {
        stringResource(Res.string.duration_seconds, remainder)
    }
}

private val previewCourse = Course(1, "Neuroeducación y aprendizaje", "Estrategias para acompañar el aprendizaje.", null, 42, listOf("Neuroeducación"))
private val previewCurriculum = listOf(
    Lesson(10, 1, null, LessonType.LESSON, "Introducción", 1, null, true, 0, null),
    Lesson(11, 1, null, LessonType.LESSON, "Acceso a Mentorías en vivo", 2, null, true, 0, null),
    Lesson(12, 1, null, LessonType.LESSON, "Centro de ayuda", 3, null, false, 0, null),
    Lesson(13, 1, null, LessonType.LESSON, "Neurofisiología", 1, null, false, 0, null),
    Lesson(14, 1, null, LessonType.LESSON, "Neuromitos", 2, null, false, 0, null),
    Lesson(15, 1, 14, LessonType.TOPIC, "Neuromitos 1 – ¿Cómo nace el mito?", 1, 420, false, 120, null,
        lastViewedAt = "2026-10-09T11:00:00Z"),
    Lesson(16, 1, 14, LessonType.TOPIC, "Neuromitos 2 – Modelo Diádico", 2, 520, false, 0, null),
)

@Preview(widthDp = 390, heightDp = 844, name = "Mi aprendizaje · compact")
@Composable
private fun CourseDashboardCompactPreview() {
    AcademyTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            CourseDashboardScreen(
                state = CourseListUiState.Content(listOf(previewCourse)),
                liveClassesState = DashboardSectionUiState.Empty,
                eventsState = DashboardSectionUiState.Empty,
                supportActionsState = DashboardSectionUiState.Empty,
                onRetry = {}, onRetryLiveClasses = {}, onRetryEvents = {}, onRetrySupportActions = {},
                onCourseSelected = {}, onViewAllCourses = {},
            )
        }
    }
}

@Preview(widthDp = 390, heightDp = 844, name = "Contenido del curso · compact")
@Composable
private fun CurriculumCompactPreview() {
    AcademyTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            CurriculumScreen(
                CurriculumUiState.Content(
                    previewCourse,
                    previewCurriculum,
                    emptyList(),
                    listOf(CourseSectionHeading("Módulos", 13)),
                ),
                {}, {}, {}, {},
            )
        }
    }
}

@Preview(widthDp = 914, heightDp = 411, name = "Contenido del curso · phone landscape")
@Composable
private fun CurriculumLandscapePreview() {
    AcademyTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            CurriculumScreen(
                CurriculumUiState.Content(
                    previewCourse,
                    previewCurriculum,
                    emptyList(),
                    listOf(CourseSectionHeading("Módulos", 13)),
                ),
                {}, {}, {}, {},
            )
        }
    }
}

@Preview(widthDp = 914, heightDp = 411, name = "Lección · phone landscape")
@Composable
private fun LessonLandscapePreview() {
    val lesson = Lesson(12, 1, null, LessonType.LESSON, "Introducción al tema", 1, null, false, 0, null)
    AcademyTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            LessonScreen(
                state = LessonUiState.Content(previewCourse, lesson),
                playbackState = PlaybackUiState.NoVideo,
                completionState = LessonCompletionUiState.Available,
                onBack = {},
                onRetry = {},
                onRetryPlayback = {},
                onMarkComplete = {},
                onRetryCompletion = {},
            )
        }
    }
}

@Preview(widthDp = 390, heightDp = 760, name = "Lección · sin video · disponible")
@Composable
private fun LessonNoVideoCompletionPreview() {
    AcademyTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            LessonDetails(
                lesson = Lesson(12, 1, null, LessonType.LESSON, "Introducción al tema", 1, null, false, 0, null),
                playbackState = PlaybackUiState.NoVideo,
                completionState = LessonCompletionUiState.Available,
                onRetryPlayback = {},
                onMarkComplete = {},
                onRetryCompletion = {},
            )
        }
    }
}

@Preview(widthDp = 390, heightDp = 760, name = "Lección · sin video · completada")
@Composable
private fun LessonCompletedPreview() {
    AcademyTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            LessonDetails(
                lesson = Lesson(12, 1, null, LessonType.LESSON, "Introducción al tema", 1, null, true, 0, null),
                playbackState = PlaybackUiState.NoVideo,
                completionState = LessonCompletionUiState.AlreadyCompleted,
                onRetryPlayback = {},
                onMarkComplete = {},
                onRetryCompletion = {},
            )
        }
    }
}
