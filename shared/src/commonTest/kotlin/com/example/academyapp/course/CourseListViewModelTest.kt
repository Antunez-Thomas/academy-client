package com.example.academyapp.course

import com.example.academyapp.course.navigation.LearningRoute

import com.example.academyapp.testUserFacingStrings
import com.example.academyapp.api.ApiResult
import com.example.academyapp.api.AcademyApi
import com.example.academyapp.api.AcademyApiError
import com.example.academyapp.api.model.Course
import com.example.academyapp.api.model.CourseContent
import com.example.academyapp.api.model.CourseProgress
import com.example.academyapp.api.model.DashboardEvent
import com.example.academyapp.api.model.DashboardEventSection
import com.example.academyapp.api.model.DashboardEventStatus
import com.example.academyapp.api.model.DashboardLiveClassSection
import com.example.academyapp.api.model.DashboardLiveClass
import com.example.academyapp.api.model.DashboardResponse
import com.example.academyapp.api.model.DashboardSectionStatus
import com.example.academyapp.api.model.DashboardSupportActionSection
import com.example.academyapp.api.model.DashboardSupportAction
import com.example.academyapp.api.model.Entitlement
import com.example.academyapp.api.model.Lesson
import com.example.academyapp.api.model.LessonType
import com.example.academyapp.api.model.LoginResponse
import com.example.academyapp.api.model.LogoutResponse
import com.example.academyapp.api.model.PlaybackResponse
import com.example.academyapp.api.model.ProgressRequest
import com.example.academyapp.api.model.ProgressResponse
import com.example.academyapp.api.model.QuizWebLaunch
import com.example.academyapp.api.model.QuizWebLaunchRequest
import com.example.academyapp.api.model.Quiz
import com.example.academyapp.api.model.User
import com.example.academyapp.api.model.Video
import com.example.academyapp.session.AcademySessionManager
import com.example.academyapp.session.MutableAccessTokenProvider
import com.example.academyapp.session.SessionTokenStorage
import com.example.academyapp.session.SessionTokens
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class CourseRepositoryTest {
    @Test
    fun coursesUseAuthenticatedExecutionAndRefreshOnce() = runTest {
        val provider = MutableAccessTokenProvider()
        val api = FakeCourseApi(provider).apply {
            coursesRequireReplacementToken = true
        }
        val session = AcademySessionManager(api, MemoryStorage(OLD_TOKENS), provider)
        session.initialize()
        val repository = AuthenticatedCourseRepository(api, session)

        val result = repository.getCourses()

        assertEquals(listOf(COURSE), assertIs<ApiResult.Success<List<Course>>>(result).value)
        assertEquals(1, api.refreshCalls)
        assertEquals(2, api.courseCalls)
    }

    @Test
    fun lessonUsesCourseScopedAuthenticatedExecutionAndRefreshOnce() = runTest {
        val provider = MutableAccessTokenProvider()
        val api = FakeCourseApi(provider).apply {
            lessonRequiresReplacementToken = true
        }
        val session = AcademySessionManager(api, MemoryStorage(OLD_TOKENS), provider)
        session.initialize()
        val repository = AuthenticatedCourseRepository(api, session)

        val result = repository.getLesson(COURSE.id, LESSON.id)

        assertEquals(LESSON, assertIs<ApiResult.Success<Lesson>>(result).value)
        assertEquals(1, api.refreshCalls)
        assertEquals(2, api.lessonCalls)
        assertEquals(listOf(COURSE.id to LESSON.id, COURSE.id to LESSON.id), api.lessonRequests)
    }

    @Test
    fun playbackUsesCourseScopedAuthenticatedExecutionAndRefreshOnce() = runTest {
        val provider = MutableAccessTokenProvider()
        val api = FakeCourseApi(provider).apply {
            playbackRequiresReplacementToken = true
        }
        val session = AcademySessionManager(api, MemoryStorage(OLD_TOKENS), provider)
        session.initialize()
        val repository = AuthenticatedCourseRepository(api, session)

        val result = repository.authorizePlayback(COURSE.id, VIDEO_LESSON.id)

        assertEquals(PLAYBACK, assertIs<ApiResult.Success<PlaybackResponse>>(result).value)
        assertEquals(1, api.refreshCalls)
        assertEquals(2, api.playbackCalls)
        assertEquals(
            listOf(COURSE.id to VIDEO_LESSON.id, COURSE.id to VIDEO_LESSON.id),
            api.playbackRequests,
        )
    }

    @Test
    fun progressWriteUsesAuthenticatedExecutionAndPreservesRequestOnRefresh() = runTest {
        val provider = MutableAccessTokenProvider()
        val api = FakeCourseApi(provider).apply {
            progressRequiresReplacementToken = true
        }
        val session = AcademySessionManager(api, MemoryStorage(OLD_TOKENS), provider)
        session.initialize()
        val repository = AuthenticatedCourseRepository(api, session)

        val result = repository.saveLessonProgress(LESSON.id, PROGRESS_REQUEST)

        assertEquals(PROGRESS_RESPONSE, assertIs<ApiResult.Success<ProgressResponse>>(result).value)
        assertEquals(1, api.refreshCalls)
        assertEquals(2, api.progressCalls)
        assertEquals(
            listOf(LESSON.id to PROGRESS_REQUEST, LESSON.id to PROGRESS_REQUEST),
            api.progressRequests,
        )
    }

    @Test
    fun courseProgressReadUsesAuthenticatedExecutionAndRefreshOnce() = runTest {
        val provider = MutableAccessTokenProvider()
        val api = FakeCourseApi(provider).apply {
            courseProgressRequiresReplacementToken = true
        }
        val session = AcademySessionManager(api, MemoryStorage(OLD_TOKENS), provider)
        session.initialize()
        val repository = AuthenticatedCourseRepository(api, session)

        val result = repository.getCourseProgress(COURSE.id)

        assertEquals(COURSE_PROGRESS, assertIs<ApiResult.Success<CourseProgress>>(result).value)
        assertEquals(1, api.refreshCalls)
        assertEquals(2, api.courseProgressCalls)
        assertEquals(listOf(COURSE.id, COURSE.id), api.courseProgressRequests)
    }

    @Test
    fun quizLaunchUsesAuthenticatedExecutionAndRefreshOnce() = runTest {
        val provider = MutableAccessTokenProvider()
        val api = FakeCourseApi(provider).apply {
            quizLaunchRequiresReplacementToken = true
        }
        val session = AcademySessionManager(api, MemoryStorage(OLD_TOKENS), provider)
        session.initialize()
        val repository = AuthenticatedCourseRepository(api, session)

        val result = repository.createQuizWebLaunch(QUIZ_REQUEST)

        assertEquals(QUIZ_LAUNCH, assertIs<ApiResult.Success<QuizWebLaunch>>(result).value)
        assertEquals(1, api.refreshCalls)
        assertEquals(2, api.quizLaunchCalls)
        assertEquals(listOf(QUIZ_REQUEST, QUIZ_REQUEST), api.quizLaunchRequests)
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class CourseListViewModelTest {
    @Test
    fun dashboardConsumesAggregateCoursesAndKeepsOptionalSectionsIndependent() = runTest {
        val session = signedInSession()
        val dashboardGate = CompletableDeferred<Unit>()
        val repository = FakeCourseRepository(
            coursesResult = ApiResult.Success(listOf(COURSE)),
            dashboardGate = dashboardGate,
            dashboardResult = ApiResult.Success(
                DashboardResponse(
                    courses = listOf(COURSE),
                    upcomingLiveClasses = DashboardLiveClassSection(
                        DashboardSectionStatus.READY,
                        listOf(DashboardLiveClass(10, 20, "Course title", "Live class", "2026-10-07T18:00:00+00:00", "2026-10-07T19:00:00+00:00", true, "https://zoom.us/j/123")),
                    ),
                    events = DashboardEventSection(
                        DashboardSectionStatus.READY,
                        listOf(DashboardEvent("Evento", "2026-10-07T18:00:00Z", "2026-10-07T19:00:00Z", DashboardEventStatus.STARTING, true, "Ingresar al evento", "https://events.example.test/join")),
                    ),
                    supportActions = DashboardSupportActionSection(DashboardSectionStatus.UNAVAILABLE, emptyList()),
                ),
            ),
        )
        val viewModel = CourseListViewModel(session, repository, backgroundScope)

        runCurrent()
        assertEquals(DashboardSectionUiState.Loading, viewModel.liveClassesState.value)
        assertEquals(DashboardSectionUiState.Loading, viewModel.eventsState.value)
        assertEquals(DashboardSectionUiState.Loading, viewModel.supportActionsState.value)
        dashboardGate.complete(Unit)
        runCurrent()

        assertEquals(listOf(COURSE), assertIs<CourseListUiState.Content>(viewModel.listState.value).courses)
        assertEquals("Live class", assertIs<DashboardSectionUiState.Content<DashboardLiveClass>>(viewModel.liveClassesState.value).items.single().title)
        assertEquals("Evento", assertIs<DashboardSectionUiState.Content<DashboardEvent>>(viewModel.eventsState.value).items.single().title)
        assertEquals(DashboardSectionUiState.Unavailable, viewModel.supportActionsState.value)
        assertEquals(1, repository.courseCalls)
        viewModel.retrySupportActions()
        runCurrent()
        assertEquals(2, repository.courseCalls)
    }

    @Test
    fun unavailableEventsDoNotHideCoursesOrOtherDashboardSections() = runTest {
        val repository = FakeCourseRepository(
            coursesResult = ApiResult.Success(listOf(COURSE)),
            dashboardResult = ApiResult.Success(
                DashboardResponse(
                    courses = listOf(COURSE),
                    upcomingLiveClasses = DashboardLiveClassSection(DashboardSectionStatus.READY, emptyList()),
                    events = DashboardEventSection(DashboardSectionStatus.UNAVAILABLE, emptyList()),
                    supportActions = DashboardSupportActionSection(DashboardSectionStatus.READY, emptyList()),
                ),
            ),
        )
        val viewModel = CourseListViewModel(signedInSession(), repository, backgroundScope)

        runCurrent()

        assertEquals(listOf(COURSE), assertIs<CourseListUiState.Content>(viewModel.listState.value).courses)
        assertEquals(DashboardSectionUiState.Unavailable, viewModel.eventsState.value)
        assertEquals(DashboardSectionUiState.Empty, viewModel.liveClassesState.value)
        assertEquals(DashboardSectionUiState.Empty, viewModel.supportActionsState.value)
    }

    @Test
    fun dashboardPreviewIsBoundedAndAllCoursesActionPreservesEveryFetchedCourse() = runTest {
        val courses = (1L..12L).map { id ->
            COURSE.copy(id = id, title = "Synthetic course $id", progressPercent = id.toInt())
        }
        val session = signedInSession()
        val repository = FakeCourseRepository(coursesResult = ApiResult.Success(courses))
        val viewModel = CourseListViewModel(session, repository, backgroundScope)

        runCurrent()

        assertEquals(LearningRoute.Dashboard, viewModel.backStack.last())
        val loadedCourses = assertIs<CourseListUiState.Content>(viewModel.listState.value).courses
        assertEquals(courses.take(COURSE_DASHBOARD_PREVIEW_LIMIT), dashboardPreviewCourses(loadedCourses))
        assertEquals(courses, loadedCourses)
        assertEquals(1, repository.courseCalls)

        viewModel.showAllCourses()
        assertEquals(LearningRoute.Courses, viewModel.backStack.last())
        assertEquals(courses, assertIs<CourseListUiState.Content>(viewModel.listState.value).courses)

        viewModel.openCourse(courses.last())
        runCurrent()
        assertEquals(LearningRoute.Curriculum(courses.last().id), viewModel.backStack.last())
        viewModel.showCourses()
        assertEquals(LearningRoute.Courses, viewModel.backStack.last())

        viewModel.showDashboard()
        assertEquals(LearningRoute.Dashboard, viewModel.backStack.last())
        viewModel.openCourse(courses.first())
        runCurrent()
        viewModel.showCourses()
        assertEquals(LearningRoute.Dashboard, viewModel.backStack.last())
        assertEquals(1, repository.courseCalls)
    }

    @Test
    fun signedInSessionLoadsCourseContent() = runTest {
        val session = signedInSession()
        val repository = FakeCourseRepository(coursesResult = ApiResult.Success(listOf(COURSE)))
        val viewModel = CourseListViewModel(
            session,
            repository,
            backgroundScope,
            userFacingStrings = testUserFacingStrings,
        )

        runCurrent()

        assertEquals(CourseListUiState.Content(listOf(COURSE)), viewModel.listState.value)
        assertEquals(1, repository.courseCalls)
    }

    @Test
    fun emptyAndRetryStatesAreExposed() = runTest {
        val session = signedInSession()
        val repository = FakeCourseRepository(coursesResult = ApiResult.Success(emptyList()))
        val viewModel = CourseListViewModel(session, repository, backgroundScope)
        runCurrent()
        assertEquals(CourseListUiState.Empty, viewModel.listState.value)
        assertEquals(DashboardSectionUiState.Empty, viewModel.liveClassesState.value)
        assertEquals(DashboardSectionUiState.Empty, viewModel.eventsState.value)
        assertEquals(DashboardSectionUiState.Empty, viewModel.supportActionsState.value)

        repository.coursesResult = ApiResult.Success(listOf(COURSE))
        viewModel.retryCourses()
        runCurrent()

        assertIs<CourseListUiState.Content>(viewModel.listState.value)
        assertEquals(2, repository.courseCalls)
    }

    @Test
    fun errorStateProvidesSafeMessageAndRetry() = runTest {
        val offline = AcademyApiError.NoConnection(IllegalStateException("offline"))
        val session = signedInSession()
        val repository = FakeCourseRepository(coursesResult = ApiResult.Failure(offline))
        val viewModel = CourseListViewModel(
            session,
            repository,
            backgroundScope,
            userFacingStrings = testUserFacingStrings,
        )
        runCurrent()

        val error = assertIs<CourseListUiState.Error>(viewModel.listState.value)
        assertEquals("No se pudo conectar con Academy. Revisa tu conexión e inténtalo de nuevo.", error.message)
        assertIs<DashboardSectionUiState.Error>(viewModel.liveClassesState.value)
        assertIs<DashboardSectionUiState.Error>(viewModel.eventsState.value)
        assertIs<DashboardSectionUiState.Error>(viewModel.supportActionsState.value)

        repository.coursesResult = ApiResult.Success(listOf(COURSE))
        viewModel.retryCourses()
        runCurrent()
        assertIs<CourseListUiState.Content>(viewModel.listState.value)
    }

    @Test
    fun courseSelectionLoadsCurriculumAndBackReturnsToList() = runTest {
        val session = signedInSession()
        val repository = FakeCourseRepository(
            coursesResult = ApiResult.Success(listOf(COURSE)),
            contentResult = ApiResult.Success(COURSE_CONTENT),
        )
        val viewModel = CourseListViewModel(
            session,
            repository,
            backgroundScope,
            userFacingStrings = testUserFacingStrings,
        )
        runCurrent()

        viewModel.openCourse(COURSE)
        runCurrent()

        assertEquals(LearningRoute.Curriculum(COURSE.id), viewModel.backStack.last())
        val content = assertIs<CurriculumUiState.Content>(viewModel.curriculumState.value)
        assertEquals(listOf(LESSON), content.lessons)

        viewModel.showCourses()
        assertEquals(LearningRoute.Dashboard, viewModel.backStack.last())
        assertEquals(CurriculumUiState.Hidden, viewModel.curriculumState.value)
    }

    @Test
    fun lessonSelectionUsesCourseContextAndExposesLoadingThenContent() = runTest {
        val gate = CompletableDeferred<Unit>()
        val session = signedInSession()
        val repository = FakeCourseRepository(
            coursesResult = ApiResult.Success(listOf(COURSE)),
            contentResult = ApiResult.Success(COURSE_CONTENT),
            lessonResult = ApiResult.Success(LOADED_LESSON),
            lessonGate = gate,
        )
        val viewModel = CourseListViewModel(
            session,
            repository,
            backgroundScope,
            userFacingStrings = testUserFacingStrings,
        )
        runCurrent()
        viewModel.openCourse(COURSE)
        runCurrent()

        viewModel.openLesson(LESSON)
        runCurrent()

        assertEquals(LearningRoute.LessonDetail(COURSE.id, LESSON.id), viewModel.backStack.last())
        assertEquals(LessonUiState.Loading(COURSE, LESSON), viewModel.lessonState.value)
        assertEquals(listOf(COURSE.id to LESSON.id), repository.lessonRequests)

        gate.complete(Unit)
        runCurrent()

        assertEquals(LessonUiState.Content(COURSE, LOADED_LESSON), viewModel.lessonState.value)
        assertEquals(PlaybackUiState.NoVideo, viewModel.playbackState.value)
        assertTrue(repository.playbackRequests.isEmpty())
    }

    @Test
    fun videoLessonAuthorizesPlaybackAfterLessonLoads() = runTest {
        val playbackGate = CompletableDeferred<Unit>()
        val session = signedInSession()
        val repository = FakeCourseRepository(
            coursesResult = ApiResult.Success(listOf(COURSE)),
            contentResult = ApiResult.Success(COURSE_CONTENT),
            lessonResult = ApiResult.Success(VIDEO_LESSON),
            playbackResult = ApiResult.Success(PLAYBACK),
            playbackGate = playbackGate,
        )
        val viewModel = CourseListViewModel(
            session,
            repository,
            backgroundScope,
            userFacingStrings = testUserFacingStrings,
        )
        runCurrent()
        viewModel.openCourse(COURSE)
        runCurrent()

        viewModel.openLesson(LESSON)
        runCurrent()

        assertEquals(LessonUiState.Content(COURSE, VIDEO_LESSON), viewModel.lessonState.value)
        assertEquals(PlaybackUiState.Loading, viewModel.playbackState.value)
        assertEquals(listOf(COURSE.id to VIDEO_LESSON.id), repository.playbackRequests)

        playbackGate.complete(Unit)
        runCurrent()

        val playable = assertIs<PlaybackUiState.Playable>(viewModel.playbackState.value)
        assertEquals(PLAYBACK.playbackUrl, playable.playbackUrl)
        assertEquals("Playable", playable.toString())
    }

    @Test
    fun playbackAuthorizationErrorCanRetry() = runTest {
        val offline = AcademyApiError.NoConnection(IllegalStateException("sensitive transport detail"))
        val session = signedInSession()
        val repository = FakeCourseRepository(
            coursesResult = ApiResult.Success(listOf(COURSE)),
            contentResult = ApiResult.Success(COURSE_CONTENT),
            lessonResult = ApiResult.Success(VIDEO_LESSON),
            playbackResult = ApiResult.Failure(offline),
        )
        val viewModel = CourseListViewModel(
            session,
            repository,
            backgroundScope,
            userFacingStrings = testUserFacingStrings,
        )
        runCurrent()
        viewModel.openCourse(COURSE)
        runCurrent()
        viewModel.openLesson(LESSON)
        runCurrent()

        val error = assertIs<PlaybackUiState.Error>(viewModel.playbackState.value)
        assertEquals("No pudimos autorizar este video. Inténtalo de nuevo.", error.message)

        repository.playbackResult = ApiResult.Success(PLAYBACK)
        viewModel.retryPlayback()
        runCurrent()

        assertIs<PlaybackUiState.Playable>(viewModel.playbackState.value)
        assertEquals(2, repository.playbackRequests.size)
    }

    @Test
    fun backWhilePlaybackAuthorizationIsLoadingClearsPlaybackState() = runTest {
        val playbackGate = CompletableDeferred<Unit>()
        val session = signedInSession()
        val repository = FakeCourseRepository(
            coursesResult = ApiResult.Success(listOf(COURSE)),
            contentResult = ApiResult.Success(COURSE_CONTENT),
            lessonResult = ApiResult.Success(VIDEO_LESSON),
            playbackResult = ApiResult.Success(PLAYBACK),
            playbackGate = playbackGate,
        )
        val viewModel = CourseListViewModel(
            session,
            repository,
            backgroundScope,
            userFacingStrings = testUserFacingStrings,
        )
        runCurrent()
        viewModel.openCourse(COURSE)
        runCurrent()
        viewModel.openLesson(LESSON)
        runCurrent()
        assertEquals(PlaybackUiState.Loading, viewModel.playbackState.value)

        viewModel.showCurriculum()
        playbackGate.complete(Unit)
        runCurrent()

        assertEquals(LearningRoute.Curriculum(COURSE.id), viewModel.backStack.last())
        assertEquals(PlaybackUiState.Hidden, viewModel.playbackState.value)
    }

    @Test
    fun invalidPlaybackLocationIsRejectedWithoutExposingIt() = runTest {
        val session = signedInSession()
        val repository = FakeCourseRepository(
            coursesResult = ApiResult.Success(listOf(COURSE)),
            contentResult = ApiResult.Success(COURSE_CONTENT),
            lessonResult = ApiResult.Success(VIDEO_LESSON),
            playbackResult = ApiResult.Success(
                PLAYBACK.copy(playbackUrl = "https://example.test/embed/123/$VIDEO_ID"),
            ),
        )
        val viewModel = CourseListViewModel(
            session,
            repository,
            backgroundScope,
            userFacingStrings = testUserFacingStrings,
        )
        runCurrent()
        viewModel.openCourse(COURSE)
        runCurrent()
        viewModel.openLesson(LESSON)
        runCurrent()

        val error = assertIs<PlaybackUiState.Error>(viewModel.playbackState.value)
        assertEquals("No pudimos autorizar este video. Inténtalo de nuevo.", error.message)
    }

    @Test
    fun lessonErrorCanRetryAndBackReturnsToExistingCurriculum() = runTest {
        val offline = AcademyApiError.NoConnection(IllegalStateException("offline"))
        val session = signedInSession()
        val repository = FakeCourseRepository(
            coursesResult = ApiResult.Success(listOf(COURSE)),
            contentResult = ApiResult.Success(COURSE_CONTENT),
            lessonResult = ApiResult.Failure(offline),
        )
        val viewModel = CourseListViewModel(
            session,
            repository,
            backgroundScope,
            userFacingStrings = testUserFacingStrings,
        )
        runCurrent()
        viewModel.openCourse(COURSE)
        runCurrent()
        viewModel.openLesson(LESSON)
        runCurrent()

        val error = assertIs<LessonUiState.Error>(viewModel.lessonState.value)
        assertEquals("No se pudo conectar con Academy. Revisa tu conexión e inténtalo de nuevo.", error.message)

        repository.lessonResult = ApiResult.Success(LOADED_LESSON)
        viewModel.retryLesson()
        runCurrent()
        assertEquals(LessonUiState.Content(COURSE, LOADED_LESSON), viewModel.lessonState.value)
        assertEquals(2, repository.lessonRequests.size)

        viewModel.showCurriculum()
        assertEquals(LearningRoute.Curriculum(COURSE.id), viewModel.backStack.last())
        assertEquals(LessonUiState.Hidden, viewModel.lessonState.value)
        assertEquals(PlaybackUiState.Hidden, viewModel.playbackState.value)
        assertIs<CurriculumUiState.Content>(viewModel.curriculumState.value)
    }

    @Test
    fun signingOutClearsLessonDestinationAndState() = runTest {
        val session = signedInSession()
        val repository = FakeCourseRepository(
            coursesResult = ApiResult.Success(listOf(COURSE)),
            contentResult = ApiResult.Success(COURSE_CONTENT),
            lessonResult = ApiResult.Success(VIDEO_LESSON),
            playbackResult = ApiResult.Success(PLAYBACK),
        )
        val viewModel = CourseListViewModel(session, repository, backgroundScope)
        runCurrent()
        viewModel.openCourse(COURSE)
        runCurrent()
        viewModel.openLesson(LESSON)
        runCurrent()
        assertIs<LessonUiState.Content>(viewModel.lessonState.value)
        assertIs<PlaybackUiState.Playable>(viewModel.playbackState.value)

        session.logout()
        runCurrent()

        assertEquals(LearningRoute.Dashboard, viewModel.backStack.last())
        assertEquals(LessonUiState.Hidden, viewModel.lessonState.value)
        assertEquals(PlaybackUiState.Hidden, viewModel.playbackState.value)
        assertEquals(CourseListUiState.SignedOut, viewModel.listState.value)
        assertEquals(LessonCompletionUiState.Hidden, viewModel.completionState.value)
    }

    @Test
    fun successfulCompletionUsesServerPositionAndRefreshesCourseProgress() = runTest {
        val session = signedInSession()
        val repository = FakeCourseRepository(
            coursesResult = ApiResult.Success(listOf(COURSE)),
            lessonResult = ApiResult.Success(INCOMPLETE_LOADED_LESSON),
            progressResult = ApiResult.Success(PROGRESS_RESPONSE),
            courseProgressResult = ApiResult.Success(COURSE_PROGRESS),
        )
        val viewModel = CourseListViewModel(
            session = session,
            repository = repository,
            coroutineScope = backgroundScope,
            eventIdGenerator = ProgressEventIdGenerator { PROGRESS_EVENT_ID },
        )
        runCurrent()
        viewModel.openCourse(COURSE)
        runCurrent()
        viewModel.openLesson(LESSON)
        runCurrent()

        viewModel.markLessonComplete()
        runCurrent()

        assertEquals(listOf(LESSON.id to PROGRESS_REQUEST), repository.progressRequests)
        assertEquals(listOf(COURSE.id), repository.courseProgressRequests)
        assertEquals(LessonCompletionUiState.Saved, viewModel.completionState.value)
        val lesson = assertIs<LessonUiState.Content>(viewModel.lessonState.value).lesson
        assertTrue(lesson.completed)
        assertEquals(INCOMPLETE_LOADED_LESSON.positionSeconds, lesson.positionSeconds)
        val curriculum = assertIs<CurriculumUiState.Content>(viewModel.curriculumState.value)
        assertTrue(curriculum.lessons.single().completed)
        assertEquals(COURSE_PROGRESS.progressPercent, curriculum.course.progressPercent)
        val courses = assertIs<CourseListUiState.Content>(viewModel.listState.value)
        assertEquals(COURSE_PROGRESS.progressPercent, courses.courses.single().progressPercent)
    }

    @Test
    fun failedCompletionRetriesWithExactlyTheSameEventAndPayload() = runTest {
        val offline = AcademyApiError.NoConnection(IllegalStateException("offline"))
        val session = signedInSession()
        val repository = FakeCourseRepository(
            coursesResult = ApiResult.Success(listOf(COURSE)),
            lessonResult = ApiResult.Success(INCOMPLETE_LOADED_LESSON),
            progressResult = ApiResult.Failure(offline),
        )
        var generatedIds = 0
        val viewModel = CourseListViewModel(
            session = session,
            repository = repository,
            coroutineScope = backgroundScope,
            eventIdGenerator = ProgressEventIdGenerator {
                generatedIds += 1
                PROGRESS_EVENT_ID
            },
            userFacingStrings = testUserFacingStrings,
        )
        runCurrent()
        viewModel.openCourse(COURSE)
        runCurrent()
        viewModel.openLesson(LESSON)
        runCurrent()

        viewModel.markLessonComplete()
        runCurrent()

        assertIs<LessonCompletionUiState.Error>(viewModel.completionState.value)
        assertFalse(assertIs<LessonUiState.Content>(viewModel.lessonState.value).lesson.completed)

        repository.progressResult = ApiResult.Success(PROGRESS_RESPONSE.copy(duplicate = true))
        viewModel.retryLessonCompletion()
        runCurrent()

        assertEquals(1, generatedIds)
        assertEquals(
            listOf(LESSON.id to PROGRESS_REQUEST, LESSON.id to PROGRESS_REQUEST),
            repository.progressRequests,
        )
        assertEquals(LessonCompletionUiState.Saved, viewModel.completionState.value)
        assertTrue(assertIs<LessonUiState.Content>(viewModel.lessonState.value).lesson.completed)
    }

    @Test
    fun failedCourseProgressRefreshRetriesAcceptedWriteAndThenUpdatesPercentage() = runTest {
        val offline = AcademyApiError.NoConnection(IllegalStateException("offline"))
        val session = signedInSession()
        val repository = FakeCourseRepository(
            coursesResult = ApiResult.Success(listOf(COURSE)),
            lessonResult = ApiResult.Success(INCOMPLETE_LOADED_LESSON),
            progressResult = ApiResult.Success(PROGRESS_RESPONSE),
            courseProgressResult = ApiResult.Failure(offline),
        )
        var generatedIds = 0
        val viewModel = CourseListViewModel(
            session = session,
            repository = repository,
            coroutineScope = backgroundScope,
            eventIdGenerator = ProgressEventIdGenerator {
                generatedIds += 1
                PROGRESS_EVENT_ID
            },
            userFacingStrings = testUserFacingStrings,
        )
        runCurrent()
        viewModel.openCourse(COURSE)
        runCurrent()
        viewModel.openLesson(LESSON)
        runCurrent()

        viewModel.markLessonComplete()
        runCurrent()

        assertTrue(assertIs<LessonUiState.Content>(viewModel.lessonState.value).lesson.completed)
        val error = assertIs<LessonCompletionUiState.Error>(viewModel.completionState.value)
        assertEquals(
            "Se guardó la finalización, pero no pudimos actualizar el progreso del curso. Inténtalo de nuevo.",
            error.message,
        )
        assertEquals(listOf(LESSON.id to PROGRESS_REQUEST), repository.progressRequests)
        assertEquals(listOf(COURSE.id), repository.courseProgressRequests)
        val staleCourses = assertIs<CourseListUiState.Content>(viewModel.listState.value)
        assertEquals(COURSE.progressPercent, staleCourses.courses.single().progressPercent)

        repository.progressResult = ApiResult.Success(PROGRESS_RESPONSE.copy(duplicate = true))
        repository.courseProgressResult = ApiResult.Success(COURSE_PROGRESS)
        viewModel.retryLessonCompletion()
        runCurrent()

        assertEquals(1, generatedIds)
        assertEquals(
            listOf(LESSON.id to PROGRESS_REQUEST, LESSON.id to PROGRESS_REQUEST),
            repository.progressRequests,
        )
        assertEquals(listOf(COURSE.id, COURSE.id), repository.courseProgressRequests)
        assertEquals(LessonCompletionUiState.Saved, viewModel.completionState.value)
        val refreshedCourses = assertIs<CourseListUiState.Content>(viewModel.listState.value)
        assertEquals(COURSE_PROGRESS.progressPercent, refreshedCourses.courses.single().progressPercent)
    }

    @Test
    fun mismatchedProgressResponseDoesNotCompleteLesson() = runTest {
        val session = signedInSession()
        val repository = FakeCourseRepository(
            coursesResult = ApiResult.Success(listOf(COURSE)),
            lessonResult = ApiResult.Success(INCOMPLETE_LOADED_LESSON),
            progressResult = ApiResult.Success(PROGRESS_RESPONSE.copy(lessonId = LESSON.id + 1)),
        )
        val viewModel = CourseListViewModel(
            session,
            repository,
            backgroundScope,
            ProgressEventIdGenerator { PROGRESS_EVENT_ID },
            userFacingStrings = testUserFacingStrings,
        )
        runCurrent()
        viewModel.openCourse(COURSE)
        runCurrent()
        viewModel.openLesson(LESSON)
        runCurrent()

        viewModel.markLessonComplete()
        runCurrent()

        assertIs<LessonCompletionUiState.Error>(viewModel.completionState.value)
        assertFalse(assertIs<LessonUiState.Content>(viewModel.lessonState.value).lesson.completed)
        assertTrue(repository.courseProgressRequests.isEmpty())
    }

    @Test
    fun duplicateCompletionSubmissionsAreIgnoredWhileSaving() = runTest {
        val gate = CompletableDeferred<Unit>()
        val session = signedInSession()
        val repository = FakeCourseRepository(
            coursesResult = ApiResult.Success(listOf(COURSE)),
            lessonResult = ApiResult.Success(INCOMPLETE_LOADED_LESSON),
            progressGate = gate,
        )
        val viewModel = CourseListViewModel(
            session,
            repository,
            backgroundScope,
            ProgressEventIdGenerator { PROGRESS_EVENT_ID },
        )
        runCurrent()
        viewModel.openCourse(COURSE)
        runCurrent()
        viewModel.openLesson(LESSON)
        runCurrent()

        viewModel.markLessonComplete()
        viewModel.markLessonComplete()
        runCurrent()

        assertEquals(LessonCompletionUiState.Saving, viewModel.completionState.value)
        assertEquals(listOf(LESSON.id to PROGRESS_REQUEST), repository.progressRequests)

        gate.complete(Unit)
        runCurrent()
        assertEquals(LessonCompletionUiState.Saved, viewModel.completionState.value)
    }

    @Test
    fun alreadyCompletedLessonsDoNotSubmitCompletion() = runTest {
        val session = signedInSession()
        val repository = FakeCourseRepository(
            coursesResult = ApiResult.Success(listOf(COURSE)),
            lessonResult = ApiResult.Success(LOADED_LESSON),
        )
        val viewModel = CourseListViewModel(session, repository, backgroundScope)
        runCurrent()
        viewModel.openCourse(COURSE)
        runCurrent()
        viewModel.openLesson(LESSON)
        runCurrent()

        assertEquals(LessonCompletionUiState.AlreadyCompleted, viewModel.completionState.value)
        viewModel.markLessonComplete()

        assertTrue(repository.progressRequests.isEmpty())
    }

    @Test
    fun signingOutCancelsCompletionAndClearsPendingState() = runTest {
        val gate = CompletableDeferred<Unit>()
        val session = signedInSession()
        val repository = FakeCourseRepository(
            coursesResult = ApiResult.Success(listOf(COURSE)),
            lessonResult = ApiResult.Success(INCOMPLETE_LOADED_LESSON),
            progressGate = gate,
        )
        val viewModel = CourseListViewModel(
            session,
            repository,
            backgroundScope,
            ProgressEventIdGenerator { PROGRESS_EVENT_ID },
        )
        runCurrent()
        viewModel.openCourse(COURSE)
        runCurrent()
        viewModel.openLesson(LESSON)
        runCurrent()
        viewModel.markLessonComplete()
        runCurrent()
        assertEquals(LessonCompletionUiState.Saving, viewModel.completionState.value)

        session.logout()
        gate.complete(Unit)
        runCurrent()

        assertEquals(LearningRoute.Dashboard, viewModel.backStack.last())
        assertEquals(LessonUiState.Hidden, viewModel.lessonState.value)
        assertEquals(LessonCompletionUiState.Hidden, viewModel.completionState.value)
        assertEquals(listOf(LESSON.id to PROGRESS_REQUEST), repository.progressRequests)
    }

    @Test
    fun quizSelectionCreatesOneLaunchAndExposesReadyState() = runTest {
        val session = signedInSession()
        val repository = FakeCourseRepository(coursesResult = ApiResult.Success(listOf(COURSE)))
        val viewModel = CourseListViewModel(
            session,
            repository,
            backgroundScope,
            quizRequestIdGenerator = QuizLaunchRequestIdGenerator { QUIZ_REQUEST_ID },
        )
        runCurrent()
        viewModel.openCourse(COURSE)
        runCurrent()

        viewModel.openQuiz(QUIZ)
        runCurrent()

        assertEquals(LearningRoute.QuizWeb(COURSE.id, QUIZ.id), viewModel.backStack.last())
        assertEquals(listOf(QUIZ_REQUEST), repository.quizLaunchRequests)
        assertEquals(QUIZ_LAUNCH, assertIs<QuizWebUiState.Ready>(viewModel.quizWebState.value).launch)
    }

    @Test
    fun failedQuizLaunchRetriesWithIdenticalRequestAndEventId() = runTest {
        val offline = AcademyApiError.NoConnection(IllegalStateException("private transport detail"))
        val session = signedInSession()
        val repository = FakeCourseRepository(
            coursesResult = ApiResult.Success(listOf(COURSE)),
            quizLaunchResult = ApiResult.Failure(offline),
        )
        val viewModel = CourseListViewModel(
            session,
            repository,
            backgroundScope,
            quizRequestIdGenerator = QuizLaunchRequestIdGenerator { QUIZ_REQUEST_ID },
            userFacingStrings = testUserFacingStrings,
        )
        runCurrent()
        viewModel.openCourse(COURSE)
        runCurrent()
        viewModel.openQuiz(QUIZ)
        runCurrent()

        assertIs<QuizWebUiState.Error>(viewModel.quizWebState.value)
        repository.quizLaunchResult = ApiResult.Success(QUIZ_LAUNCH)
        viewModel.retryQuizLaunch()
        runCurrent()

        assertEquals(listOf(QUIZ_REQUEST, QUIZ_REQUEST), repository.quizLaunchRequests)
        assertIs<QuizWebUiState.Ready>(viewModel.quizWebState.value)
    }

    @Test
    fun duplicateQuizLaunchIsIgnoredWhileRequestIsInFlight() = runTest {
        val gate = CompletableDeferred<Unit>()
        val session = signedInSession()
        val repository = FakeCourseRepository(
            coursesResult = ApiResult.Success(listOf(COURSE)),
            quizLaunchGate = gate,
        )
        val viewModel = CourseListViewModel(
            session,
            repository,
            backgroundScope,
            quizRequestIdGenerator = QuizLaunchRequestIdGenerator { QUIZ_REQUEST_ID },
        )
        runCurrent()
        viewModel.openCourse(COURSE)
        runCurrent()
        viewModel.openQuiz(QUIZ)
        viewModel.retryQuizLaunch()
        runCurrent()

        assertEquals(listOf(QUIZ_REQUEST), repository.quizLaunchRequests)
        assertEquals(QuizWebUiState.Loading, viewModel.quizWebState.value)
        gate.complete(Unit)
        runCurrent()
        assertIs<QuizWebUiState.Ready>(viewModel.quizWebState.value)
    }

    @Test
    fun returningFromQuizClearsLaunchAndRefreshesCurriculumAndCourseProgress() = runTest {
        val session = signedInSession()
        val repository = FakeCourseRepository(coursesResult = ApiResult.Success(listOf(COURSE)))
        val controller = RecordingQuizWebSessionController()
        val viewModel = CourseListViewModel(
            session,
            repository,
            backgroundScope,
            quizRequestIdGenerator = QuizLaunchRequestIdGenerator { QUIZ_REQUEST_ID },
            quizWebSessionController = controller,
        )
        runCurrent()
        viewModel.openCourse(COURSE)
        runCurrent()
        viewModel.openQuiz(QUIZ)
        runCurrent()

        viewModel.returnFromQuiz()
        runCurrent()

        assertEquals(QuizWebUiState.Hidden, viewModel.quizWebState.value)
        assertEquals(1, controller.revokeCalls)
        assertEquals(listOf(COURSE.id), repository.courseProgressRequests)
        assertEquals(50, assertIs<CurriculumUiState.Content>(viewModel.curriculumState.value).course.progressPercent)
    }

    @Test
    fun quizReturnAndCurriculumBackRestoreDashboardOrAllCoursesParent() = runTest {
        for (openedFromAllCourses in listOf(false, true)) {
            val session = signedInSession()
            val repository = FakeCourseRepository(coursesResult = ApiResult.Success(listOf(COURSE)))
            val viewModel = CourseListViewModel(
                session,
                repository,
                backgroundScope,
                quizRequestIdGenerator = QuizLaunchRequestIdGenerator { QUIZ_REQUEST_ID },
                quizWebSessionController = RecordingQuizWebSessionController(),
            )
            runCurrent()
            if (openedFromAllCourses) viewModel.showAllCourses()

            viewModel.openCourse(COURSE)
            runCurrent()
            viewModel.openQuiz(QUIZ)
            runCurrent()
            viewModel.returnFromQuiz()
            runCurrent()

            assertEquals(LearningRoute.Curriculum(COURSE.id), viewModel.backStack.last())
            viewModel.showCourses()
            assertEquals(
                if (openedFromAllCourses) LearningRoute.Courses else LearningRoute.Dashboard,
                viewModel.backStack.last(),
            )
        }
    }

    @Test
    fun failedQuizCleanupKeepsDestinationAndRetryClosesWithFreshProgress() = runTest {
        val session = signedInSession()
        val repository = FakeCourseRepository(coursesResult = ApiResult.Success(listOf(COURSE)))
        val controller = RecordingQuizWebSessionController(QuizWebSessionCleanupResult.Failed)
        val viewModel = CourseListViewModel(
            session,
            repository,
            backgroundScope,
            quizRequestIdGenerator = QuizLaunchRequestIdGenerator { QUIZ_REQUEST_ID },
            quizWebSessionController = controller,
            userFacingStrings = testUserFacingStrings,
        )
        runCurrent()
        viewModel.openCourse(COURSE)
        runCurrent()
        viewModel.openQuiz(QUIZ)
        runCurrent()

        viewModel.returnFromQuiz()
        runCurrent()

        assertEquals(LearningRoute.QuizWeb(COURSE.id, QUIZ.id), viewModel.backStack.last())
        assertIs<QuizWebUiState.CleanupError>(viewModel.quizWebState.value)
        assertTrue(repository.courseProgressRequests.isEmpty())

        controller.result = QuizWebSessionCleanupResult.Cleared
        viewModel.retryQuizCleanup()
        runCurrent()

        assertEquals(2, controller.revokeCalls)
        assertEquals(LearningRoute.Curriculum(COURSE.id), viewModel.backStack.last())
        assertEquals(QuizWebUiState.Hidden, viewModel.quizWebState.value)
        assertEquals(listOf(COURSE.id), repository.courseProgressRequests)
    }

    @Test
    fun signingOutClearsPendingQuizLaunch() = runTest {
        val gate = CompletableDeferred<Unit>()
        val session = signedInSession()
        val repository = FakeCourseRepository(
            coursesResult = ApiResult.Success(listOf(COURSE)),
            quizLaunchGate = gate,
        )
        val viewModel = CourseListViewModel(
            session,
            repository,
            backgroundScope,
            quizRequestIdGenerator = QuizLaunchRequestIdGenerator { QUIZ_REQUEST_ID },
        )
        runCurrent()
        viewModel.openCourse(COURSE)
        runCurrent()
        viewModel.openQuiz(QUIZ)
        runCurrent()

        session.logout()
        gate.complete(Unit)
        runCurrent()

        assertEquals(LearningRoute.Dashboard, viewModel.backStack.last())
        assertEquals(QuizWebUiState.Hidden, viewModel.quizWebState.value)
    }

    @Test
    fun generatedProgressEventIdsAreDistinctAndContractValid() {
        val random = Random(1234)
        val first = newProgressEventId(random)
        val second = newProgressEventId(random)

        assertNotEquals(first, second)
        assertTrue(first.length in 8..128)
        assertTrue(first.matches(Regex("[A-Za-z0-9._:-]+")))
    }

    @Test
    fun generatedQuizLaunchIdsAreDistinctAndContractValid() {
        val random = Random(4321)
        val first = newQuizLaunchRequestId(random)
        val second = newQuizLaunchRequestId(random)

        assertNotEquals(first, second)
        assertTrue(first.length in 8..128)
        assertTrue(first.matches(Regex("[A-Za-z0-9._:-]+")))
    }

    @Test
    fun restoredLessonRehydratesIdsAndBackReturnsToItsCurriculum() = runTest {
        val session = signedInSession()
        val repository = FakeCourseRepository(ApiResult.Success(listOf(COURSE)))
        val vm = CourseListViewModel(session, repository, backgroundScope, userFacingStrings = testUserFacingStrings)
        runCurrent()
        val routes = listOf(LearningRoute.Dashboard, LearningRoute.Courses,
            LearningRoute.Curriculum(COURSE.id), LearningRoute.LessonDetail(COURSE.id, LESSON.id))
        vm.restoreNavigation(USER.id, com.example.academyapp.course.navigation.saveLearningNavigation(USER.id, routes))
        runCurrent()
        assertEquals(routes, vm.backStack.toList())
        assertIs<LessonUiState.Content>(vm.lessonState.value)
        assertFalse(vm.isRestoringNavigation.value)
        vm.navigateBack()
        assertEquals(routes.dropLast(1), vm.backStack.toList())
        vm.navigateBack()
        assertEquals(listOf(LearningRoute.Dashboard, LearningRoute.Courses), vm.backStack.toList())
    }

    @Test
    fun interruptedQuizNeverLaunchesAndCleanupFailureBlocksFreshAttemptUntilRetry() = runTest {
        val session = signedInSession()
        val repository = FakeCourseRepository(ApiResult.Success(listOf(COURSE)))
        val controller = RecordingQuizWebSessionController(QuizWebSessionCleanupResult.Failed)
        val vm = CourseListViewModel(session, repository, backgroundScope,
            quizWebSessionController = controller, userFacingStrings = testUserFacingStrings)
        runCurrent()
        val routes = listOf(LearningRoute.Dashboard, LearningRoute.Curriculum(COURSE.id),
            LearningRoute.QuizWeb(COURSE.id, QUIZ.id))
        vm.restoreNavigation(USER.id, com.example.academyapp.course.navigation.saveLearningNavigation(USER.id, routes))
        assertEquals(LearningRoute.Curriculum(COURSE.id), vm.backStack.last())
        runCurrent()
        assertTrue(vm.interruptedQuizCleanupPending.value)
        assertIs<QuizWebUiState.CleanupError>(vm.quizWebState.value)
        vm.openQuiz(QUIZ)
        runCurrent()
        assertTrue(repository.quizLaunchRequests.isEmpty())
        assertTrue(repository.progressRequests.isEmpty())
        controller.result = QuizWebSessionCleanupResult.Cleared
        vm.retryInterruptedQuizCleanup()
        runCurrent()
        assertEquals(2, controller.revokeCalls)
        assertFalse(vm.interruptedQuizCleanupPending.value)
        assertIs<CurriculumUiState.Content>(vm.curriculumState.value)
        vm.openQuiz(QUIZ)
        runCurrent()
        assertEquals(1, repository.quizLaunchRequests.size)
        assertTrue(repository.progressRequests.isEmpty())
    }

    @Test
    fun unavailableRestoredCourseAndMissingLessonReturnSafely() = runTest {
        for (missingCourse in listOf(false, true)) {
            val session = signedInSession()
            val repository = FakeCourseRepository(ApiResult.Success(if (missingCourse) emptyList() else listOf(COURSE)),
                contentResult = ApiResult.Success(COURSE_CONTENT.copy(steps = emptyList())))
            val vm = CourseListViewModel(session, repository, backgroundScope, userFacingStrings = testUserFacingStrings)
            runCurrent()
            val routes = listOf(LearningRoute.Dashboard, LearningRoute.Curriculum(COURSE.id),
                LearningRoute.LessonDetail(COURSE.id, LESSON.id))
            vm.restoreNavigation(USER.id, com.example.academyapp.course.navigation.saveLearningNavigation(USER.id, routes))
            runCurrent()
            assertEquals(if (missingCourse) LearningRoute.Courses else LearningRoute.Curriculum(COURSE.id), vm.backStack.last())
            assertEquals(LessonUiState.Hidden, vm.lessonState.value)
        }
    }

    @Test
    fun retainedQuizSurvivesConfigurationRestoreWhileFreshVmDiscardsIt() = runTest {
        val session = signedInSession()
        val repository = FakeCourseRepository(ApiResult.Success(listOf(COURSE)))
        val vm = CourseListViewModel(session, repository, backgroundScope, userFacingStrings = testUserFacingStrings)
        runCurrent()
        vm.openCourse(COURSE)
        runCurrent()
        vm.openQuiz(QUIZ)
        runCurrent()
        vm.restoreNavigation(USER.id, vm.saveNavigation(USER.id))
        assertIs<LearningRoute.QuizWeb>(vm.backStack.last())
        assertFalse(vm.interruptedQuizCleanupPending.value)
    }

    @Test
    fun interruptedQuizCleanupSuccessDiscardsAttemptWithoutProgressAndForeignOwnerCannotRestore() = runTest {
        for (result in listOf(QuizWebSessionCleanupResult.Cleared, QuizWebSessionCleanupResult.NoActiveSession)) {
            val session = signedInSession()
            val repository = FakeCourseRepository(ApiResult.Success(listOf(COURSE)))
            val controller = RecordingQuizWebSessionController(result)
            val vm = CourseListViewModel(session, repository, backgroundScope,
                quizWebSessionController = controller, userFacingStrings = testUserFacingStrings)
            runCurrent()
            val routes = listOf(LearningRoute.Dashboard, LearningRoute.Courses,
                LearningRoute.Curriculum(COURSE.id), LearningRoute.QuizWeb(COURSE.id, QUIZ.id))
            val saved = com.example.academyapp.course.navigation.saveLearningNavigation(USER.id, routes)
            vm.restoreNavigation(USER.id + 1, saved)
            assertEquals(listOf(LearningRoute.Dashboard), vm.backStack.toList())
            vm.restoreNavigation(USER.id, saved)
            runCurrent()
            assertEquals(routes.dropLast(1), vm.backStack.toList())
            assertEquals(1, controller.revokeCalls)
            assertEquals(QuizWebUiState.Hidden, vm.quizWebState.value)
            assertFalse(vm.interruptedQuizCleanupPending.value)
            assertTrue(repository.quizLaunchRequests.isEmpty())
            assertTrue(repository.progressRequests.isEmpty())
            assertTrue(repository.courseProgressRequests.isEmpty())
        }
    }

    @Test
    fun persistedCleanupWithoutSavedRouteBlocksQuizUntilCleanupHandled() = runTest {
        val session = signedInSession()
        val repository = FakeCourseRepository(ApiResult.Success(listOf(COURSE)))
        val controller = RecordingQuizWebSessionController(QuizWebSessionCleanupResult.Failed, pendingCleanup = true)
        val vm = CourseListViewModel(session, repository, backgroundScope,
            quizWebSessionController = controller, userFacingStrings = testUserFacingStrings)
        runCurrent()
        assertEquals(listOf(LearningRoute.Dashboard), vm.backStack.toList())
        assertEquals(1, controller.revokeCalls)
        assertTrue(vm.interruptedQuizCleanupPending.value)
        vm.openCourse(COURSE)
        vm.openQuiz(QUIZ)
        runCurrent()
        assertTrue(repository.quizLaunchRequests.isEmpty())
        assertTrue(repository.progressRequests.isEmpty())
        controller.result = QuizWebSessionCleanupResult.Cleared
        vm.retryInterruptedQuizCleanup()
        runCurrent()
        assertFalse(vm.interruptedQuizCleanupPending.value)
        vm.openCourse(COURSE)
        runCurrent()
        vm.openQuiz(QUIZ)
        runCurrent()
        assertIs<LearningRoute.QuizWeb>(vm.backStack.last())
        assertEquals(1, repository.quizLaunchRequests.size)
    }

    @Test
    fun deniedRestoredCourseContentPreservesRouteAndAllowsExplicitBack() = runTest {
        val session = signedInSession()
        val repository = FakeCourseRepository(ApiResult.Success(listOf(COURSE)),
            contentResult = ApiResult.Failure(AcademyApiError.Forbidden(null)))
        val vm = CourseListViewModel(session, repository, backgroundScope, userFacingStrings = testUserFacingStrings)
        runCurrent()
        vm.restoreNavigation(USER.id, com.example.academyapp.course.navigation.saveLearningNavigation(USER.id,
            listOf(LearningRoute.Dashboard, LearningRoute.Curriculum(COURSE.id))))
        runCurrent()
        assertEquals(listOf(LearningRoute.Dashboard, LearningRoute.Curriculum(COURSE.id)), vm.backStack.toList())
        assertEquals("Tu cuenta no tiene acceso a este contenido.", vm.restorationError.value)
        assertFalse(vm.isRestoringNavigation.value)
        vm.navigateBack()
        assertEquals(listOf(LearningRoute.Dashboard), vm.backStack.toList())
        assertEquals(CurriculumUiState.Hidden, vm.curriculumState.value)
        assertEquals(null, vm.restorationError.value)
    }

    @Test
    fun savedRouteArrivingDuringStartupCleanupIsRehydratedAfterCleanup() = runTest {
        val session = signedInSession()
        val repository = FakeCourseRepository(ApiResult.Success(listOf(COURSE)))
        val gate = CompletableDeferred<Unit>()
        val controller = RecordingQuizWebSessionController(pendingCleanup = true, gate = gate)
        val vm = CourseListViewModel(session, repository, backgroundScope,
            quizWebSessionController = controller, userFacingStrings = testUserFacingStrings)
        runCurrent()
        assertTrue(vm.isRestoringNavigation.value)
        val routes = listOf(LearningRoute.Dashboard, LearningRoute.Curriculum(COURSE.id),
            LearningRoute.QuizWeb(COURSE.id, QUIZ.id))
        vm.restoreNavigation(USER.id, com.example.academyapp.course.navigation.saveLearningNavigation(USER.id, routes))
        gate.complete(Unit)
        runCurrent()
        assertEquals(routes.dropLast(1), vm.backStack.toList())
        assertIs<CurriculumUiState.Content>(vm.curriculumState.value)
        assertEquals(1, controller.revokeCalls)
        assertFalse(vm.interruptedQuizCleanupPending.value)
        assertTrue(repository.quizLaunchRequests.isEmpty())
    }

    @Test
    fun signOutCancelsPendingRestorationAndLateCleanupCannotRestoreChild() = runTest {
        val session = signedInSession()
        val repository = FakeCourseRepository(ApiResult.Success(listOf(COURSE)))
        val gate = CompletableDeferred<Unit>()
        val controller = RecordingQuizWebSessionController(pendingCleanup = true, gate = gate)
        val vm = CourseListViewModel(session, repository, backgroundScope,
            quizWebSessionController = controller, userFacingStrings = testUserFacingStrings)
        runCurrent()
        val saved = com.example.academyapp.course.navigation.saveLearningNavigation(USER.id,
            listOf(LearningRoute.Dashboard, LearningRoute.Curriculum(COURSE.id)))
        vm.restoreNavigation(USER.id, saved)
        session.logout()
        runCurrent()
        gate.complete(Unit)
        runCurrent()
        assertEquals(listOf(LearningRoute.Dashboard), vm.backStack.toList())
        assertEquals(CourseListUiState.SignedOut, vm.listState.value)
        assertFalse(vm.isRestoringNavigation.value)
        assertFalse(vm.interruptedQuizCleanupPending.value)
        vm.restoreNavigation(USER.id, saved)
        assertEquals(listOf(LearningRoute.Dashboard), vm.backStack.toList())
    }

    @Test
    fun courseListRestorationFailuresPreserveSavedLessonAndRetryOriginalRoute() = runTest {
        for (failure in listOf(AcademyApiError.NoConnection(IllegalStateException("synthetic offline")),
            AcademyApiError.Server(503, null))) {
            val session = signedInSession()
            val repository = FakeCourseRepository(ApiResult.Success(listOf(COURSE)))
            val vm = CourseListViewModel(session, repository, backgroundScope, userFacingStrings = testUserFacingStrings)
            runCurrent()
            val routes = listOf(LearningRoute.Dashboard, LearningRoute.Courses,
                LearningRoute.Curriculum(COURSE.id), LearningRoute.LessonDetail(COURSE.id, LESSON.id))
            repository.coursesResult = ApiResult.Failure(failure)
            vm.restoreNavigation(USER.id, com.example.academyapp.course.navigation.saveLearningNavigation(USER.id, routes))
            runCurrent()
            assertEquals(routes, vm.backStack.toList())
            assertTrue(vm.restorationError.value != null)
            assertFalse(vm.isRestoringNavigation.value)
            assertEquals(routes, com.example.academyapp.course.navigation.restoreLearningNavigation(
                USER.id, vm.saveNavigation(USER.id))!!.routes)
            vm.retryNavigationRestoration()
            runCurrent()
            assertEquals(routes, vm.backStack.toList())
            assertTrue(vm.restorationError.value != null)
            repository.coursesResult = ApiResult.Success(listOf(COURSE))
            vm.retryNavigationRestoration()
            assertEquals(null, vm.restorationError.value)
            runCurrent()
            assertEquals(routes, vm.backStack.toList())
            assertIs<LessonUiState.Content>(vm.lessonState.value)
            assertEquals(null, vm.restorationError.value)
            assertTrue(repository.progressRequests.isEmpty())
        }
    }

    @Test
    fun curriculumRestorationFailuresPreserveHistoryAndRetryToContent() = runTest {
        for (failure in listOf(AcademyApiError.NoConnection(IllegalStateException("synthetic offline")),
            AcademyApiError.Server(503, null))) {
            val session = signedInSession()
            val repository = FakeCourseRepository(ApiResult.Success(listOf(COURSE)),
                contentResult = ApiResult.Failure(failure))
            val vm = CourseListViewModel(session, repository, backgroundScope, userFacingStrings = testUserFacingStrings)
            runCurrent()
            val routes = listOf(LearningRoute.Dashboard, LearningRoute.Curriculum(COURSE.id))
            vm.restoreNavigation(USER.id, com.example.academyapp.course.navigation.saveLearningNavigation(USER.id, routes))
            runCurrent()
            assertEquals(routes, vm.backStack.toList())
            assertIs<CurriculumUiState.Error>(vm.curriculumState.value)
            assertTrue(vm.restorationError.value != null)
            assertFalse(vm.isRestoringNavigation.value)
            repository.contentResult = ApiResult.Success(COURSE_CONTENT)
            vm.retryNavigationRestoration()
            runCurrent()
            assertEquals(routes, vm.backStack.toList())
            assertIs<CurriculumUiState.Content>(vm.curriculumState.value)
            assertEquals(null, vm.restorationError.value)
            vm.navigateBack()
            assertEquals(listOf(LearningRoute.Dashboard), vm.backStack.toList())
        }
    }

    @Test
    fun restoredCourseConfirmedAbsentOnRetryStillFallsBackToPicker() = runTest {
        val session = signedInSession()
        val repository = FakeCourseRepository(ApiResult.Success(listOf(COURSE)))
        val vm = CourseListViewModel(session, repository, backgroundScope, userFacingStrings = testUserFacingStrings)
        runCurrent()
        repository.coursesResult = ApiResult.Failure(AcademyApiError.Server(503, null))
        val routes = listOf(LearningRoute.Dashboard, LearningRoute.Curriculum(COURSE.id))
        vm.restoreNavigation(USER.id, com.example.academyapp.course.navigation.saveLearningNavigation(USER.id, routes))
        runCurrent()
        assertEquals(routes, vm.backStack.toList())
        repository.coursesResult = ApiResult.Success(emptyList())
        vm.retryNavigationRestoration()
        runCurrent()
        assertEquals(listOf(LearningRoute.Dashboard, LearningRoute.Courses), vm.backStack.toList())
        assertEquals(null, vm.restorationError.value)
    }

    @Test
    fun explicitBackFromFailedLessonRestorationReturnsToKnownParent() = runTest {
        val session = signedInSession()
        val repository = FakeCourseRepository(ApiResult.Success(listOf(COURSE)))
        val vm = CourseListViewModel(session, repository, backgroundScope, userFacingStrings = testUserFacingStrings)
        runCurrent()
        repository.coursesResult = ApiResult.Failure(AcademyApiError.Server(503, null))
        val routes = listOf(LearningRoute.Dashboard, LearningRoute.Courses,
            LearningRoute.Curriculum(COURSE.id), LearningRoute.LessonDetail(COURSE.id, LESSON.id))
        vm.restoreNavigation(USER.id, com.example.academyapp.course.navigation.saveLearningNavigation(USER.id, routes))
        runCurrent()
        vm.navigateBack()
        assertEquals(listOf(LearningRoute.Dashboard, LearningRoute.Courses), vm.backStack.toList())
        assertEquals(null, vm.restorationError.value)
        repository.coursesResult = ApiResult.Success(listOf(COURSE))
        vm.retryNavigationRestoration()
        runCurrent()
        assertEquals(listOf(LearningRoute.Dashboard, LearningRoute.Courses), vm.backStack.toList())
    }

    @Test
    fun backDuringCourseOrCurriculumRestorationCancelsAndKeepsRenderableParent() = runTest {
        for (picker in listOf(false, true)) for (waitingForCourses in listOf(false, true)) {
            val session = signedInSession()
            val repository = FakeCourseRepository(ApiResult.Success(listOf(COURSE)))
            val vm = CourseListViewModel(session, repository, backgroundScope, userFacingStrings = testUserFacingStrings)
            runCurrent()
            val gate = CompletableDeferred<Unit>()
            if (waitingForCourses) repository.coursesGate = gate else repository.contentGate = gate
            val parent = if (picker) listOf(LearningRoute.Dashboard, LearningRoute.Courses)
                else listOf(LearningRoute.Dashboard)
            val routes = parent + listOf(LearningRoute.Curriculum(COURSE.id),
                LearningRoute.LessonDetail(COURSE.id, LESSON.id))
            vm.restoreNavigation(USER.id, com.example.academyapp.course.navigation.saveLearningNavigation(USER.id, routes))
            runCurrent()
            assertTrue(vm.isRestoringNavigation.value)
            assertEquals(null, vm.restorationError.value)
            vm.navigateBack()
            assertEquals(parent, vm.backStack.toList())
            assertFalse(vm.isRestoringNavigation.value)
            assertEquals(CurriculumUiState.Hidden, vm.curriculumState.value)
            assertEquals(LessonUiState.Hidden, vm.lessonState.value)
            gate.complete(Unit)
            runCurrent()
            vm.retryNavigationRestoration()
            runCurrent()
            assertEquals(parent, vm.backStack.toList())
            assertEquals(CurriculumUiState.Hidden, vm.curriculumState.value)
            assertEquals(LessonUiState.Hidden, vm.lessonState.value)
            assertEquals(null, vm.restorationError.value)
            assertTrue(repository.lessonRequests.isEmpty())
            vm.openCourse(COURSE)
            runCurrent()
            assertEquals(LearningRoute.Curriculum(COURSE.id), vm.backStack.last())
            assertIs<CurriculumUiState.Content>(vm.curriculumState.value)
        }
    }

    @Test
    fun backDuringInterruptedCleanupRendersParentWhileCleanupAndRetryStayPending() = runTest {
        for (picker in listOf(false, true)) {
            val session = signedInSession()
            val repository = FakeCourseRepository(ApiResult.Success(listOf(COURSE)))
            val gate = CompletableDeferred<Unit>()
            val controller = RecordingQuizWebSessionController(QuizWebSessionCleanupResult.Failed, gate = gate)
            val vm = CourseListViewModel(session, repository, backgroundScope,
                quizWebSessionController = controller, userFacingStrings = testUserFacingStrings)
            runCurrent()
            val parent = if (picker) listOf(LearningRoute.Dashboard, LearningRoute.Courses)
                else listOf(LearningRoute.Dashboard)
            val routes = parent + listOf(LearningRoute.Curriculum(COURSE.id),
                LearningRoute.QuizWeb(COURSE.id, QUIZ.id))
            vm.restoreNavigation(USER.id, com.example.academyapp.course.navigation.saveLearningNavigation(USER.id, routes))
            runCurrent()
            assertEquals(LearningRecoveryPresentation.Loading, recoveryPresentation(vm))

            vm.navigateBack()
            assertEquals(parent, vm.backStack.toList())
            assertTrue(vm.interruptedQuizCleanupPending.value)
            assertTrue(vm.recoveryDismissed.value)
            assertFalse(vm.isRestoringNavigation.value)
            // This is the exact selector used by CourseRoute: assert visibility before releasing cleanup.
            assertEquals(LearningRecoveryPresentation.Parent, recoveryPresentation(vm))
            assertIs<CourseListUiState.Content>(vm.listState.value)
            assertFalse(gate.isCompleted)
            assertEquals(0, controller.revokeCalls)
            vm.openCourse(COURSE)
            vm.retryInterruptedQuizCleanup()
            runCurrent()
            assertEquals(parent, vm.backStack.toList())
            assertEquals(LearningRecoveryPresentation.Parent, recoveryPresentation(vm))
            // Parent navigation remains usable while the same cleanup coroutine is suspended.
            if (picker) vm.navigateBack()
            assertEquals(listOf(LearningRoute.Dashboard), vm.backStack.toList())
            assertFalse(gate.isCompleted)

            gate.complete(Unit)
            runCurrent()
            assertEquals(1, controller.revokeCalls)
            assertIs<QuizWebUiState.CleanupError>(vm.quizWebState.value)
            assertEquals(LearningRecoveryPresentation.Parent, recoveryPresentation(vm))
            vm.openCourse(COURSE)
            assertEquals(listOf(LearningRoute.Dashboard), vm.backStack.toList())

            val retryGate = CompletableDeferred<Unit>()
            controller.gate = retryGate
            controller.result = QuizWebSessionCleanupResult.Cleared
            vm.retryInterruptedQuizCleanup()
            runCurrent()
            assertIs<QuizWebUiState.Closing>(vm.quizWebState.value)
            assertTrue(vm.interruptedQuizCleanupPending.value)
            assertFalse(vm.isRestoringNavigation.value)
            assertEquals(LearningRecoveryPresentation.Parent, recoveryPresentation(vm))
            assertFalse(retryGate.isCompleted)
            vm.openCourse(COURSE)
            assertEquals(listOf(LearningRoute.Dashboard), vm.backStack.toList())
            retryGate.complete(Unit)
            runCurrent()
            assertEquals(2, controller.revokeCalls)
            assertFalse(vm.interruptedQuizCleanupPending.value)
            assertFalse(vm.isRestoringNavigation.value)
            assertEquals(listOf(LearningRoute.Dashboard), vm.backStack.toList())
            assertEquals(CurriculumUiState.Hidden, vm.curriculumState.value)
            assertTrue(repository.quizLaunchRequests.isEmpty())
            assertTrue(repository.progressRequests.isEmpty())
            vm.openCourse(COURSE)
            runCurrent()
            assertIs<CurriculumUiState.Content>(vm.curriculumState.value)
            assertFalse(vm.recoveryDismissed.value)
        }
    }

    @Test
    fun recoveryPresentationKeepsUndismissedErrorsAndLetsDismissedCleanupRenderParent() {
        assertEquals(LearningRecoveryPresentation.CleanupError,
            learningRecoveryPresentation(false, true, null, false))
        assertEquals(LearningRecoveryPresentation.RestorationError,
            learningRecoveryPresentation(false, false, "Temporary failure", false))
        assertEquals(LearningRecoveryPresentation.Parent,
            learningRecoveryPresentation(true, true, null, true))
    }

    private fun recoveryPresentation(vm: CourseListViewModel) = learningRecoveryPresentation(
        vm.isRestoringNavigation.value,
        vm.interruptedQuizCleanupPending.value,
        vm.restorationError.value,
        vm.recoveryDismissed.value,
    )

    private suspend fun signedInSession(): AcademySessionManager {
        val provider = MutableAccessTokenProvider()
        val api = FakeCourseApi(provider)
        return AcademySessionManager(api, MemoryStorage(OLD_TOKENS), provider).also {
            it.initialize()
        }
    }
}

private class FakeCourseRepository(
    var coursesResult: ApiResult<List<Course>>,
    var dashboardResult: ApiResult<DashboardResponse>? = null,
    var dashboardGate: CompletableDeferred<Unit>? = null,
    var coursesGate: CompletableDeferred<Unit>? = null,
    var contentGate: CompletableDeferred<Unit>? = null,
    var contentResult: ApiResult<CourseContent> = ApiResult.Success(COURSE_CONTENT),
    var lessonResult: ApiResult<Lesson> = ApiResult.Success(LOADED_LESSON),
    var lessonGate: CompletableDeferred<Unit>? = null,
    var playbackResult: ApiResult<PlaybackResponse> = ApiResult.Success(PLAYBACK),
    var playbackGate: CompletableDeferred<Unit>? = null,
    var progressResult: ApiResult<ProgressResponse> = ApiResult.Success(PROGRESS_RESPONSE),
    var progressGate: CompletableDeferred<Unit>? = null,
    var courseProgressResult: ApiResult<CourseProgress> = ApiResult.Success(COURSE_PROGRESS),
    var quizLaunchResult: ApiResult<QuizWebLaunch> = ApiResult.Success(QUIZ_LAUNCH),
    var quizLaunchGate: CompletableDeferred<Unit>? = null,
) : CourseRepository {
    var courseCalls = 0
    val lessonRequests = mutableListOf<Pair<Long, Long>>()
    val playbackRequests = mutableListOf<Pair<Long, Long>>()
    val progressRequests = mutableListOf<Pair<Long, ProgressRequest>>()
    val courseProgressRequests = mutableListOf<Long>()
    val quizLaunchRequests = mutableListOf<QuizWebLaunchRequest>()

    override suspend fun getCourses(): ApiResult<List<Course>> {
        courseCalls += 1
        coursesGate?.await()
        return coursesResult
    }

    override suspend fun getDashboard(): ApiResult<DashboardResponse> {
        courseCalls += 1
        dashboardGate?.let { gate ->
            gate.await()
            dashboardGate = null
        }
        dashboardResult?.let { return it }
        return when (val result = coursesResult) {
            is ApiResult.Success -> ApiResult.Success(
                DashboardResponse(
                    courses = result.value,
                    upcomingLiveClasses = DashboardLiveClassSection(DashboardSectionStatus.READY, emptyList()),
                    events = DashboardEventSection(DashboardSectionStatus.READY, emptyList()),
                    supportActions = DashboardSupportActionSection(DashboardSectionStatus.READY, emptyList()),
                ),
            )
            is ApiResult.Failure -> result
        }
    }

    override suspend fun getCourseContent(courseId: Long): ApiResult<CourseContent> {
        contentGate?.await()
        return contentResult
    }

    override suspend fun getLesson(courseId: Long, lessonId: Long): ApiResult<Lesson> {
        lessonRequests += courseId to lessonId
        lessonGate?.await()
        return lessonResult
    }

    override suspend fun saveLessonProgress(
        lessonId: Long,
        request: ProgressRequest,
    ): ApiResult<ProgressResponse> {
        progressRequests += lessonId to request
        progressGate?.await()
        return progressResult
    }

    override suspend fun getCourseProgress(courseId: Long): ApiResult<CourseProgress> {
        courseProgressRequests += courseId
        return courseProgressResult
    }

    override suspend fun authorizePlayback(courseId: Long, lessonId: Long): ApiResult<PlaybackResponse> {
        playbackRequests += courseId to lessonId
        playbackGate?.await()
        return playbackResult
    }

    override suspend fun createQuizWebLaunch(request: QuizWebLaunchRequest): ApiResult<QuizWebLaunch> {
        quizLaunchRequests += request
        quizLaunchGate?.await()
        return quizLaunchResult
    }
}

private class MemoryStorage(
    private var tokens: SessionTokens? = null,
) : SessionTokenStorage {
    override suspend fun load(): SessionTokens? = tokens
    override suspend fun save(tokens: SessionTokens) {
        this.tokens = tokens
    }
    override suspend fun clear() {
        tokens = null
    }
}

private class RecordingQuizWebSessionController(
    var result: QuizWebSessionCleanupResult = QuizWebSessionCleanupResult.Cleared,
    var pendingCleanup: Boolean = false,
    var gate: CompletableDeferred<Unit>? = null,
) : QuizWebSessionController {
    override fun hasPendingCleanup(): Boolean = pendingCleanup
    var revokeCalls = 0

    override fun activate(logoutUrl: String) = Unit

    override suspend fun revokeAndClear(): QuizWebSessionCleanupResult {
        gate?.await()
        revokeCalls += 1
        if (result != QuizWebSessionCleanupResult.Failed) pendingCleanup = false
        return result
    }
}

private class FakeCourseApi(
    private val provider: MutableAccessTokenProvider,
) : AcademyApi {
    var coursesRequireReplacementToken = false
    var lessonRequiresReplacementToken = false
    var playbackRequiresReplacementToken = false
    var progressRequiresReplacementToken = false
    var courseProgressRequiresReplacementToken = false
    var quizLaunchRequiresReplacementToken = false
    var refreshCalls = 0
    var courseCalls = 0
    var lessonCalls = 0
    var playbackCalls = 0
    var progressCalls = 0
    var courseProgressCalls = 0
    var quizLaunchCalls = 0
    val lessonRequests = mutableListOf<Pair<Long, Long>>()
    val playbackRequests = mutableListOf<Pair<Long, Long>>()
    val progressRequests = mutableListOf<Pair<Long, ProgressRequest>>()
    val courseProgressRequests = mutableListOf<Long>()
    val quizLaunchRequests = mutableListOf<QuizWebLaunchRequest>()

    override suspend fun getMe(): ApiResult<User> = ApiResult.Success(USER)

    override suspend fun getCourses(): ApiResult<List<Course>> {
        courseCalls += 1
        return if (coursesRequireReplacementToken && provider.accessToken() == OLD_TOKENS.accessToken) {
            ApiResult.Failure(AcademyApiError.Unauthorized(null))
        } else {
            ApiResult.Success(listOf(COURSE))
        }
    }

    override suspend fun refresh(refreshToken: String): ApiResult<LoginResponse> {
        refreshCalls += 1
        return ApiResult.Success(REPLACEMENT_LOGIN)
    }

    override suspend fun getCourseContent(id: Long): ApiResult<CourseContent> = ApiResult.Success(COURSE_CONTENT)
    override suspend fun getLesson(courseId: Long, lessonId: Long): ApiResult<Lesson> {
        lessonCalls += 1
        lessonRequests += courseId to lessonId
        return if (lessonRequiresReplacementToken && provider.accessToken() == OLD_TOKENS.accessToken) {
            ApiResult.Failure(AcademyApiError.Unauthorized(null))
        } else {
            ApiResult.Success(LESSON)
        }
    }
    override suspend fun login(email: String, password: String): ApiResult<LoginResponse> = unsupported()
    override suspend fun logout(): ApiResult<LogoutResponse> = ApiResult.Success(LogoutResponse(true))
    override suspend fun getCourse(id: Long): ApiResult<Course> = unsupported()
    override suspend fun saveLessonProgress(
        lessonId: Long,
        request: ProgressRequest,
    ): ApiResult<ProgressResponse> {
        progressCalls += 1
        progressRequests += lessonId to request
        return if (progressRequiresReplacementToken && provider.accessToken() == OLD_TOKENS.accessToken) {
            ApiResult.Failure(AcademyApiError.Unauthorized(null))
        } else {
            ApiResult.Success(PROGRESS_RESPONSE)
        }
    }
    override suspend fun getCourseProgress(courseId: Long): ApiResult<CourseProgress> {
        courseProgressCalls += 1
        courseProgressRequests += courseId
        return if (courseProgressRequiresReplacementToken && provider.accessToken() == OLD_TOKENS.accessToken) {
            ApiResult.Failure(AcademyApiError.Unauthorized(null))
        } else {
            ApiResult.Success(COURSE_PROGRESS)
        }
    }
    override suspend fun authorizePlayback(
        courseId: Long,
        lessonId: Long,
    ): ApiResult<PlaybackResponse> {
        playbackCalls += 1
        playbackRequests += courseId to lessonId
        return if (playbackRequiresReplacementToken && provider.accessToken() == OLD_TOKENS.accessToken) {
            ApiResult.Failure(AcademyApiError.Unauthorized(null))
        } else {
            ApiResult.Success(PLAYBACK)
        }
    }
    override suspend fun createQuizWebLaunch(request: QuizWebLaunchRequest): ApiResult<QuizWebLaunch> {
        quizLaunchCalls += 1
        quizLaunchRequests += request
        return if (quizLaunchRequiresReplacementToken && provider.accessToken() == OLD_TOKENS.accessToken) {
            ApiResult.Failure(AcademyApiError.Unauthorized(null))
        } else {
            ApiResult.Success(QUIZ_LAUNCH)
        }
    }
    override suspend fun getEntitlements(): ApiResult<List<Entitlement>> = unsupported()

    private fun <T> unsupported(): ApiResult<T> =
        ApiResult.Failure(AcademyApiError.NoConnection(IllegalStateException("Unexpected call")))
}

private val USER = User(42, "learner@example.test", "Learner")
private val COURSE = Course(
    id = 10,
    title = "Course title",
    excerpt = "Course excerpt",
    imageUrl = null,
    progressPercent = 25,
    categories = listOf("Category"),
)
private val LESSON = Lesson(
    id = 20,
    courseId = COURSE.id,
    parentLessonId = null,
    type = LessonType.LESSON,
    title = "Lesson title",
    position = 1,
    durationSeconds = 120,
    completed = false,
    positionSeconds = 0,
    video = null,
)
private val LOADED_LESSON = LESSON.copy(completed = true, positionSeconds = 45)
private val INCOMPLETE_LOADED_LESSON = LESSON.copy(positionSeconds = 45)
private const val PROGRESS_EVENT_ID = "progress:test-event-1"
private val PROGRESS_REQUEST = ProgressRequest(
    courseId = COURSE.id,
    completed = true,
    positionSeconds = INCOMPLETE_LOADED_LESSON.positionSeconds,
    clientEventId = PROGRESS_EVENT_ID,
)
private val PROGRESS_RESPONSE = ProgressResponse(
    lessonId = LESSON.id,
    courseId = COURSE.id,
    completed = true,
    positionSeconds = INCOMPLETE_LOADED_LESSON.positionSeconds,
    accepted = true,
    clientEventId = PROGRESS_EVENT_ID,
    duplicate = false,
)
private val COURSE_PROGRESS = CourseProgress(COURSE.id, 50)
private const val VIDEO_ID = "018f47ac-38d7-7e45-a5d9-82018f4b87fb"
private val VIDEO_LESSON = LOADED_LESSON.copy(video = Video(videoId = VIDEO_ID, libraryId = 123))
private val PLAYBACK = PlaybackResponse(
    lessonId = VIDEO_LESSON.id,
    videoId = VIDEO_ID,
    libraryId = 123,
    token = "synthetic-playback-token",
    expires = 2_000_000_000,
    tokenScheme = "embed_sha256_hex",
    playbackUrl = "https://iframe.mediadelivery.net/embed/123/$VIDEO_ID" +
        "?token=synthetic-playback-token&expires=2000000000",
    expiresAt = "2033-05-18T03:33:20Z",
)
private val QUIZ = Quiz(
    id = 30,
    courseId = COURSE.id,
    title = "Knowledge check",
    position = 2,
    completed = false,
)
private val COURSE_CONTENT = CourseContent(COURSE, listOf(LESSON), listOf(QUIZ))
private const val QUIZ_REQUEST_ID = "quiz-launch:test-request"
private val QUIZ_REQUEST = QuizWebLaunchRequest(COURSE.id, QUIZ.id, QUIZ_REQUEST_ID)
private val QUIZ_LAUNCH = QuizWebLaunch(
    userId = USER.id,
    courseId = COURSE.id,
    quizId = QUIZ.id,
    ticket = "d".repeat(64),
    redeemUrl = "http://10.0.2.2:8080/wp-json/academy-mobile/v1/quiz/web-redeem",
    logoutUrl = "http://10.0.2.2:8080/wp-json/academy-mobile/v1/quiz/web-logout",
    quizUrl = "http://10.0.2.2:8080/quizzes/knowledge-check/",
    expiresAt = "2026-10-02T13:05:00Z",
)
private val OLD_TOKENS = SessionTokens("old-access", "old-refresh", "2026-09-23T12:00:00Z")
private val REPLACEMENT_LOGIN = LoginResponse(
    user = USER,
    accessToken = "new-access",
    refreshToken = "new-refresh",
    accessTokenExpiresAt = "2026-09-23T13:00:00Z",
)
