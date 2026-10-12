package com.example.academyapp.api

import com.example.academyapp.api.model.CourseContent
import com.example.academyapp.api.model.LoginResponse
import com.example.academyapp.api.model.PlaybackResponse
import com.example.academyapp.api.model.ProgressRequest
import com.example.academyapp.api.model.ProgressResponse
import com.example.academyapp.api.model.User
import com.example.academyapp.network.configureAcademyHttpClient
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class LiveAcademyApiIntegrationTest {
    @Test
    fun exercisesLocalLearnerFlowWhenEnabled() = runBlocking {
        if (System.getenv("ACADEMY_LIVE_TEST") != "1") return@runBlocking

        val baseUrl = requiredEnvironment("ACADEMY_API_BASE_URL").trimEnd('/') + "/"
        val email = requiredEnvironment("ACADEMY_TEST_EMAIL")
        val password = requiredEnvironment("ACADEMY_TEST_PASSWORD")
        val courseId = requiredEnvironment("ACADEMY_TEST_COURSE_ID").toLong()
        val lessonId = requiredEnvironment("ACADEMY_TEST_LESSON_ID").toLong()
        var accessToken: String? = null
        val client = HttpClient(CIO) { configureAcademyHttpClient() }
        val api = KtorAcademyApi(
            configuration = AcademyApiConfiguration(AcademyEnvironment.LOCAL, baseUrl, true),
            client = client,
            accessTokenProvider = AccessTokenProvider { accessToken },
        )

        try {
            val login = assertIs<ApiResult.Success<LoginResponse>>(api.login(email, password)).value
            accessToken = login.accessToken

            val me = assertIs<ApiResult.Success<User>>(api.getMe()).value
            assertEquals(login.user.id, me.id)

            val courses = assertIs<ApiResult.Success<*>>(api.getCourses()).value
                as List<*>
            assertTrue(courses.any { (it as? com.example.academyapp.api.model.Course)?.id == courseId })

            val content = assertIs<ApiResult.Success<CourseContent>>(api.getCourseContent(courseId)).value
            assertEquals(courseId, content.course.id)
            assertTrue(content.steps.any { it.id == lessonId })

            val lesson = assertIs<ApiResult.Success<com.example.academyapp.api.model.Lesson>>(
                api.getLesson(courseId, lessonId),
            ).value
            assertEquals(courseId, lesson.courseId)
            assertEquals(lessonId, lesson.id)

            val playback = assertIs<ApiResult.Success<PlaybackResponse>>(
                api.authorizePlayback(courseId, lessonId),
            ).value
            assertEquals(lessonId, playback.lessonId)
            assertTrue(playback.playbackUrl.startsWith("https://iframe.mediadelivery.net/embed/"))

            val progress = assertIs<ApiResult.Success<ProgressResponse>>(
                api.saveLessonProgress(
                    lessonId,
                    ProgressRequest(
                        courseId = courseId,
                        completed = false,
                        positionSeconds = 3,
                        clientEventId = "phase3-${System.currentTimeMillis()}",
                    ),
                ),
            ).value
            assertEquals(courseId, progress.courseId)
            assertEquals(lessonId, progress.lessonId)
            assertTrue(progress.accepted)
        } finally {
            client.close()
        }
    }

    private fun requiredEnvironment(name: String): String =
        requireNotNull(System.getenv(name)?.takeIf(String::isNotBlank)) { "$name is required" }
}
