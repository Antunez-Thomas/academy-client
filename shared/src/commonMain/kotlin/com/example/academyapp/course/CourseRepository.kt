package com.example.academyapp.course

import com.example.academyapp.api.ApiResult
import com.example.academyapp.api.AcademyApi
import com.example.academyapp.api.model.Course
import com.example.academyapp.api.model.DashboardResponse
import com.example.academyapp.api.model.CourseContent
import com.example.academyapp.api.model.CourseProgress
import com.example.academyapp.api.model.Lesson
import com.example.academyapp.api.model.PlaybackResponse
import com.example.academyapp.api.model.ProgressRequest
import com.example.academyapp.api.model.ProgressResponse
import com.example.academyapp.api.model.QuizWebLaunch
import com.example.academyapp.api.model.QuizWebLaunchRequest
import com.example.academyapp.session.AcademySessionManager

interface CourseRepository {
    val supportSiteHost: String? get() = null
    suspend fun getCourses(): ApiResult<List<Course>>
    suspend fun getDashboard(): ApiResult<DashboardResponse>
    suspend fun getCourseContent(courseId: Long): ApiResult<CourseContent>
    suspend fun getLesson(courseId: Long, lessonId: Long): ApiResult<Lesson>
    suspend fun saveLessonProgress(lessonId: Long, request: ProgressRequest): ApiResult<ProgressResponse>
    suspend fun getCourseProgress(courseId: Long): ApiResult<CourseProgress>
    suspend fun authorizePlayback(courseId: Long, lessonId: Long): ApiResult<PlaybackResponse>
    suspend fun createQuizWebLaunch(request: QuizWebLaunchRequest): ApiResult<QuizWebLaunch>
}

class AuthenticatedCourseRepository(
    private val api: AcademyApi,
    private val session: AcademySessionManager,
) : CourseRepository {
    override val supportSiteHost: String? get() = api.configuredApiHost

    override suspend fun getCourses(): ApiResult<List<Course>> =
        session.executeAuthenticated { api.getCourses() }

    override suspend fun getDashboard(): ApiResult<DashboardResponse> =
        session.executeAuthenticated { api.getDashboard() }

    override suspend fun getCourseContent(courseId: Long): ApiResult<CourseContent> =
        session.executeAuthenticated { api.getCourseContent(courseId) }

    override suspend fun getLesson(courseId: Long, lessonId: Long): ApiResult<Lesson> =
        session.executeAuthenticated { api.getLesson(courseId, lessonId) }

    override suspend fun saveLessonProgress(
        lessonId: Long,
        request: ProgressRequest,
    ): ApiResult<ProgressResponse> =
        session.executeAuthenticated { api.saveLessonProgress(lessonId, request) }

    override suspend fun getCourseProgress(courseId: Long): ApiResult<CourseProgress> =
        session.executeAuthenticated { api.getCourseProgress(courseId) }

    override suspend fun authorizePlayback(courseId: Long, lessonId: Long): ApiResult<PlaybackResponse> =
        session.executeAuthenticated { api.authorizePlayback(courseId, lessonId) }

    override suspend fun createQuizWebLaunch(request: QuizWebLaunchRequest): ApiResult<QuizWebLaunch> =
        session.executeAuthenticated { api.createQuizWebLaunch(request) }
}
