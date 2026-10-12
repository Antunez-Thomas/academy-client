package com.example.academyapp.api

import com.example.academyapp.api.model.Course
import com.example.academyapp.api.model.CourseContent
import com.example.academyapp.api.model.Lesson
import com.example.academyapp.api.model.LoginRequest
import com.example.academyapp.api.model.RegisterRequest
import com.example.academyapp.api.model.PasswordResetRequest
import com.example.academyapp.api.model.PlaybackRequest
import com.example.academyapp.api.model.ProgressRequest
import com.example.academyapp.api.model.QuizWebLaunchRequest
import com.example.academyapp.api.model.QuizWebLaunchWireResponse
import com.example.academyapp.api.model.RefreshRequest
import com.example.academyapp.network.AcademyJson
import com.example.academyapp.network.configureAcademyHttpClient
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.decodeFromString
import kotlinx.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class KtorAcademyApiTest {
    private val staging = AcademyApiEnvironments.configurationFor(AcademyEnvironment.STAGING)
    private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")
    private val tokenProvider = AccessTokenProvider { "test-access-token" }

    @Test
    fun quizLaunchWireResponseRedactsTicketFromStringRepresentation() {
        val wire = QuizWebLaunchWireResponse(
            userId = 42,
            courseId = 1001,
            quizId = 3001,
            ticket = "sensitive-single-use-ticket",
            redeemPath = "/redeem",
            logoutPath = "/logout",
            quizPath = "/quiz",
            expiresAt = "2026-10-02T13:05:00Z",
        )

        assertTrue("sensitive-single-use-ticket" !in wire.toString())
        assertTrue("ticket=[redacted]" in wire.toString())
    }

    @Test
    fun deletionRequestUsesOnlyTheCurrentBearerAndAnEmptyBody() = runTest {
        val client = mockClient { request ->
            assertEquals(HttpMethod.Post, request.method)
            assertEquals("/wp-json/academy-mobile/v1/account/deletion-request", request.url.encodedPath)
            assertEquals("Bearer test-access-token", request.headers[HttpHeaders.Authorization])
            assertEquals("{}", request.body.toByteArray().decodeToString())
            respond("""{"state":"awaiting_confirmation","confirmationRequired":true}""", HttpStatusCode.Accepted, jsonHeaders)
        }
        val api = KtorAcademyApi(staging, client, tokenProvider)
        val result = assertIs<ApiResult.Success<com.example.academyapp.api.model.DeletionRequestResponse>>(api.requestAccountDeletion())
        assertEquals("awaiting_confirmation", result.value.state)
        assertTrue(result.value.confirmationRequired)
        client.close()
    }

    @Test
    fun deletionRequestDoesNotSendWithoutAnAccessToken() = runTest {
        val client = mockClient { error("Unexpected network request") }
        assertIs<ApiResult.Failure>(KtorAcademyApi(staging, client).requestAccountDeletion())
        client.close()
    }

    @Test
    fun logsInAndDecodesTheResponse() = runTest {
        val client = mockClient { request ->
            assertEquals(HttpMethod.Post, request.method)
            assertEquals("/wp-json/academy-mobile/v1/auth/login", request.url.encodedPath)
            val body = AcademyJson.decodeFromString<LoginRequest>(request.body.toByteArray().decodeToString())
            assertEquals("learner@example.test", body.email)
            respond(TestFixtures.LOGIN_RESPONSE, headers = jsonHeaders)
        }
        val api = KtorAcademyApi(staging, client)

        val result = api.login("learner@example.test", "synthetic-test-password")

        val success = assertIs<ApiResult.Success<*>>(result)
        assertEquals(42L, (success.value as com.example.academyapp.api.model.LoginResponse).user.id)
        client.close()
    }

    @Test
    fun usesAdditivePublicRegistrationAndPasswordResetRoutes() = runTest {
        val visited = mutableListOf<String>()
        val client = mockClient { request ->
            val path = request.url.encodedPath
            visited += path
            when (path) {
                "/wp-json/academy-mobile/v1/auth/options" -> {
                    assertEquals(HttpMethod.Get, request.method)
                    respond(TestFixtures.AUTH_OPTIONS, headers = jsonHeaders)
                }
                "/wp-json/academy-mobile/v1/auth/register" -> {
                    assertEquals(HttpMethod.Post, request.method)
                    val body = AcademyJson.decodeFromString<RegisterRequest>(request.body.toByteArray().decodeToString())
                    assertEquals("learner@example.test", body.email)
                    assertEquals("synthetic-test-password", body.password)
                    assertTrue(body.acceptLegalAgreement)
                    assertEquals("Learner Example", body.displayName)
                    assertEquals(null, request.headers[HttpHeaders.Authorization])
                    respond(TestFixtures.LOGIN_RESPONSE, HttpStatusCode.Created, jsonHeaders)
                }
                "/wp-json/academy-mobile/v1/auth/password-reset" -> {
                    assertEquals(HttpMethod.Post, request.method)
                    val body = AcademyJson.decodeFromString<PasswordResetRequest>(request.body.toByteArray().decodeToString())
                    assertEquals("learner@example.test", body.email)
                    respond(TestFixtures.PASSWORD_RESET_RESPONSE, headers = jsonHeaders)
                }
                else -> respondError(HttpStatusCode.NotFound)
            }
        }
        val api = KtorAcademyApi(staging, client)

        val options = assertIs<ApiResult.Success<*>>(api.getAuthOptions()).value as com.example.academyapp.api.model.AuthOptions
        assertTrue(options.registrationAvailable)
        assertTrue(options.legalAgreementRequired)
        assertEquals("https://example.test/terms", options.termsUrl)
        assertEquals("https://example.test/privacy", options.privacyUrl)
        assertIs<ApiResult.Success<*>>(api.register(RegisterRequest("learner@example.test", "synthetic-test-password", true, "Learner Example")))
        assertIs<ApiResult.Success<*>>(api.requestPasswordReset("learner@example.test"))
        assertEquals(
            listOf("/wp-json/academy-mobile/v1/auth/options", "/wp-json/academy-mobile/v1/auth/register", "/wp-json/academy-mobile/v1/auth/password-reset"),
            visited,
        )
        client.close()
    }

    @Test
    fun callsCourseScopedLessonProgressAndPlaybackEndpoints() = runTest {
        val visited = mutableSetOf<String>()
        val client = mockClient { request ->
            assertEquals("Bearer test-access-token", request.headers[HttpHeaders.Authorization])
            val path = request.url.encodedPath
            visited += path
            when (path) {
                "/wp-json/academy-mobile/v1/courses" -> respond(TestFixtures.COURSES, headers = jsonHeaders)
                "/wp-json/academy-mobile/v1/dashboard" -> respond(TestFixtures.DASHBOARD, headers = jsonHeaders)
                "/wp-json/academy-mobile/v1/courses/1001" -> respond(TestFixtures.COURSE, headers = jsonHeaders)
                "/wp-json/academy-mobile/v1/courses/1001/content" -> respond(TestFixtures.COURSE_CONTENT, headers = jsonHeaders)
                "/wp-json/academy-mobile/v1/courses/1001/progress" -> respond(TestFixtures.COURSE_PROGRESS, headers = jsonHeaders)
                "/wp-json/academy-mobile/v1/lessons/2002" -> {
                    assertEquals("1001", request.url.parameters["courseId"])
                    respond(TestFixtures.LESSON, headers = jsonHeaders)
                }
                "/wp-json/academy-mobile/v1/lessons/2002/progress" -> {
                    val body = AcademyJson.decodeFromString<ProgressRequest>(request.body.toByteArray().decodeToString())
                    assertEquals(1001L, body.courseId)
                    assertEquals(120, body.positionSeconds)
                    respond(TestFixtures.PROGRESS_RESPONSE, headers = jsonHeaders)
                }
                "/wp-json/academy-mobile/v1/video/playback" -> {
                    val body = AcademyJson.decodeFromString<PlaybackRequest>(request.body.toByteArray().decodeToString())
                    assertEquals(1001L, body.courseId)
                    assertEquals(2002L, body.lessonId)
                    respond(TestFixtures.PLAYBACK_RESPONSE, headers = jsonHeaders)
                }
                "/wp-json/academy-mobile/v1/entitlements" -> respond(TestFixtures.ENTITLEMENTS, headers = jsonHeaders)
                else -> respondError(HttpStatusCode.NotFound)
            }
        }
        val api = KtorAcademyApi(staging, client, tokenProvider)
        val progressRequest = ProgressRequest(
            courseId = 1001,
            completed = false,
            positionSeconds = 120,
            clientEventId = "018f47ac-38d7-7e45-a5d9-82018f4b87fb",
        )

        assertIs<ApiResult.Success<List<Course>>>(api.getCourses())
        val dashboard = assertIs<ApiResult.Success<com.example.academyapp.api.model.DashboardResponse>>(api.getDashboard()).value
        assertIs<ApiResult.Success<Course>>(api.getCourse(1001))
        val courseContent = assertIs<ApiResult.Success<CourseContent>>(api.getCourseContent(1001)).value
        assertEquals(3, courseContent.steps.single().resources.size)
        val lesson = assertIs<ApiResult.Success<Lesson>>(api.getLesson(1001, 2002)).value
        assertEquals(listOf(7101L, 7102L, 7103L), lesson.resources.map { it.id })
        assertEquals(
            listOf(
                "application/pdf",
                "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                "application/vnd.ms-powerpoint",
            ),
            lesson.resources.map { it.mediaType },
        )
        assertEquals("https://example.test/wp-content/uploads/2026/01/reading.pdf", lesson.resources.first().url)
        assertIs<ApiResult.Success<*>>(api.saveLessonProgress(2002, progressRequest))
        assertIs<ApiResult.Success<*>>(api.getCourseProgress(1001))
        assertIs<ApiResult.Success<*>>(api.authorizePlayback(1001, 2002))
        assertIs<ApiResult.Success<*>>(api.getEntitlements())
        assertEquals(9, visited.size)
        assertEquals(1, dashboard.courses.size)
        assertEquals("Clase de prueba", dashboard.upcomingLiveClasses.items.single().title)
        assertEquals("Evento & especial", dashboard.events.items.single().title)
        assertEquals("Ingresar al evento", dashboard.events.items.single().actionLabel)
        client.close()
    }

    @Test
    fun callsRefreshMeAndLogoutEndpoints() = runTest {
        val client = mockClient { request ->
            when (request.url.encodedPath) {
                "/wp-json/academy-mobile/v1/auth/refresh" -> {
                    val body = AcademyJson.decodeFromString<RefreshRequest>(request.body.toByteArray().decodeToString())
                    assertEquals("refresh-token", body.refreshToken)
                    respond(TestFixtures.LOGIN_RESPONSE, headers = jsonHeaders)
                }
                "/wp-json/academy-mobile/v1/me" -> respond(TestFixtures.USER, headers = jsonHeaders)
                "/wp-json/academy-mobile/v1/auth/logout" -> respond(TestFixtures.LOGOUT, headers = jsonHeaders)
                else -> respondError(HttpStatusCode.NotFound)
            }
        }
        val api = KtorAcademyApi(staging, client, tokenProvider)

        assertIs<ApiResult.Success<*>>(api.refresh("refresh-token"))
        assertIs<ApiResult.Success<*>>(api.getMe())
        assertIs<ApiResult.Success<*>>(api.logout())
        client.close()
    }

    @Test
    fun createsAuthenticatedQuizLaunchAndResolvesOnlyFirstPartyPaths() = runTest {
        val client = mockClient { request ->
            assertEquals(HttpMethod.Post, request.method)
            assertEquals("/wp-json/academy-mobile/v1/quiz/web-launch", request.url.encodedPath)
            assertEquals("Bearer test-access-token", request.headers[HttpHeaders.Authorization])
            val body = AcademyJson.decodeFromString<QuizWebLaunchRequest>(
                request.body.toByteArray().decodeToString(),
            )
            assertEquals(1001L, body.courseId)
            assertEquals(3001L, body.quizId)
            assertEquals("quiz-launch:test-request", body.clientRequestId)
            respond(TestFixtures.QUIZ_WEB_LAUNCH, headers = jsonHeaders)
        }
        val api = KtorAcademyApi(staging, client, tokenProvider)

        val result = api.createQuizWebLaunch(
            QuizWebLaunchRequest(1001, 3001, "quiz-launch:test-request"),
        )

        val launch = assertIs<ApiResult.Success<*>>(result).value as com.example.academyapp.api.model.QuizWebLaunch
        assertEquals(
            "https://api.example.com/wp-json/academy-mobile/v1/quiz/web-redeem",
            launch.redeemUrl,
        )
        assertEquals("https://api.example.com/quizzes/knowledge-check/?lesson=1001", launch.quizUrl)
        assertTrue("dddddddd" !in launch.toString())
        client.close()
    }

    @Test
    fun mapsWordPressHttpErrors() = runTest {
        val cases = listOf(
            HttpStatusCode.BadRequest to AcademyApiError.BadRequest::class,
            HttpStatusCode.Unauthorized to AcademyApiError.Unauthorized::class,
            HttpStatusCode.Forbidden to AcademyApiError.Forbidden::class,
            HttpStatusCode.NotFound to AcademyApiError.NotFound::class,
            HttpStatusCode.Conflict to AcademyApiError.Conflict::class,
            HttpStatusCode.TooManyRequests to AcademyApiError.RateLimited::class,
            HttpStatusCode.InternalServerError to AcademyApiError.Server::class,
            HttpStatusCode.ServiceUnavailable to AcademyApiError.Server::class,
        )

        for ((status, expectedType) in cases) {
            val client = mockClient { respond(TestFixtures.API_ERROR, status, jsonHeaders) }
            val api = KtorAcademyApi(staging, client, tokenProvider)
            val failure = assertIs<ApiResult.Failure>(api.getCourses())
            assertTrue(expectedType.isInstance(failure.error), "$status mapped to ${failure.error}")
            client.close()
        }
    }

    @Test
    fun mapsMalformedJsonToInvalidResponse() = runTest {
        val client = mockClient { respond("{not-json", headers = jsonHeaders) }
        val api = KtorAcademyApi(staging, client, tokenProvider)

        val failure = assertIs<ApiResult.Failure>(api.getCourses())

        assertIs<AcademyApiError.InvalidResponse>(failure.error)
        client.close()
    }

    @Test
    fun mapsTimeoutConnectionAndUnexpectedFailures() = runTest {
        val timeoutClient = mockClient {
            throw HttpRequestTimeoutException("https://example.test", 1_000)
        }
        val offlineClient = mockClient {
            throw IOException("offline")
        }
        val unexpectedClient = mockClient {
            throw IllegalStateException("engine misconfiguration")
        }

        val timeout = assertIs<ApiResult.Failure>(
            KtorAcademyApi(staging, timeoutClient, tokenProvider).getCourses(),
        )
        val offline = assertIs<ApiResult.Failure>(
            KtorAcademyApi(staging, offlineClient, tokenProvider).getCourses(),
        )
        val unexpected = assertIs<ApiResult.Failure>(
            KtorAcademyApi(staging, unexpectedClient, tokenProvider).getCourses(),
        )

        assertIs<AcademyApiError.Timeout>(timeout.error)
        assertIs<AcademyApiError.NoConnection>(offline.error)
        assertIs<AcademyApiError.UnexpectedFailure>(unexpected.error)
        timeoutClient.close()
        offlineClient.close()
        unexpectedClient.close()
    }

    @Test
    fun rejectsAuthenticatedCallsWithoutATokenBeforeSendingARequest() = runTest {
        val client = mockClient { error("The request must not be sent") }
        val api = KtorAcademyApi(staging, client)

        val failure = assertIs<ApiResult.Failure>(api.getCourses())

        assertIs<AcademyApiError.MissingAccessToken>(failure.error)
        client.close()
    }

    private fun mockClient(handler: io.ktor.client.engine.mock.MockRequestHandler): HttpClient =
        HttpClient(MockEngine(handler)) {
            configureAcademyHttpClient()
        }
}
