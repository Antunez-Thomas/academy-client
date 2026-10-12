package com.example.academyapp.api

import com.example.academyapp.api.model.ApiError
import com.example.academyapp.api.model.AuthOptions
import com.example.academyapp.api.model.Course
import com.example.academyapp.api.model.DashboardResponse
import com.example.academyapp.api.model.CourseContent
import com.example.academyapp.api.model.CourseProgress
import com.example.academyapp.api.model.Entitlement
import com.example.academyapp.api.model.Lesson
import com.example.academyapp.api.model.LoginRequest
import com.example.academyapp.api.model.LoginResponse
import com.example.academyapp.api.model.PasswordResetRequest
import com.example.academyapp.api.model.PasswordResetResponse
import com.example.academyapp.api.model.RegisterRequest
import com.example.academyapp.api.model.LogoutResponse
import com.example.academyapp.api.model.PlaybackRequest
import com.example.academyapp.api.model.PlaybackResponse
import com.example.academyapp.api.model.ProgressRequest
import com.example.academyapp.api.model.ProgressResponse
import com.example.academyapp.api.model.QuizWebLaunch
import com.example.academyapp.api.model.QuizWebLaunchRequest
import com.example.academyapp.api.model.QuizWebLaunchWireResponse
import com.example.academyapp.api.model.RefreshRequest
import com.example.academyapp.api.model.User
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.prepareGet
import io.ktor.client.request.delete
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsChannel
import kotlinx.io.readByteArray
import io.ktor.utils.io.readRemaining
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.ktor.http.contentType
import io.ktor.serialization.ContentConvertException
import io.ktor.network.sockets.SocketTimeoutException
import kotlinx.io.IOException
import kotlinx.serialization.SerializationException
import kotlin.coroutines.cancellation.CancellationException

class KtorAcademyApi(
    configuration: AcademyApiConfiguration,
    private val client: HttpClient,
    private val accessTokenProvider: AccessTokenProvider = AccessTokenProvider { null },
) : AcademyApi {
    private val baseUrl = configuration.requireBaseUrl()
    override val configuredApiHost: String? = configuredHostFromApiBaseUrl(baseUrl)

    override suspend fun login(email: String, password: String): ApiResult<LoginResponse> = execute {
        client.post(endpoint("auth/login")) {
            contentType(ContentType.Application.Json)
            setBody(LoginRequest(email = email, password = password))
        }
    }

    override suspend fun getAuthOptions(): ApiResult<AuthOptions> = execute {
        client.get(endpoint("auth/options"))
    }

    override suspend fun register(request: RegisterRequest): ApiResult<LoginResponse> = execute {
        client.post(endpoint("auth/register")) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }

    override suspend fun requestPasswordReset(email: String): ApiResult<PasswordResetResponse> = execute {
        client.post(endpoint("auth/password-reset")) {
            contentType(ContentType.Application.Json)
            setBody(PasswordResetRequest(email))
        }
    }

    override suspend fun refresh(refreshToken: String): ApiResult<LoginResponse> = execute {
        client.post(endpoint("auth/refresh")) {
            contentType(ContentType.Application.Json)
            setBody(RefreshRequest(refreshToken))
        }
    }

    override suspend fun logout(): ApiResult<LogoutResponse> = authorized { token ->
        execute {
            client.post(endpoint("auth/logout")) {
                bearerAuth(token)
            }
        }
    }

    override suspend fun requestAccountDeletion(): ApiResult<com.example.academyapp.api.model.DeletionRequestResponse> = authorized { token ->
        execute {
            client.post(endpoint("account/deletion-request")) {
                bearerAuth(token)
                contentType(ContentType.Application.Json)
                setBody(kotlinx.serialization.json.JsonObject(emptyMap()))
            }
        }
    }

    override suspend fun updateProfile(request: com.example.academyapp.api.model.ProfileUpdateRequest): ApiResult<User> = authorized { token ->
        execute { client.post(endpoint("me/profile")) { bearerAuth(token); contentType(ContentType.Application.Json); setBody(request) } }
    }

    override suspend fun uploadAvatar(bytes: ByteArray, mimeType: String): ApiResult<User> = authorized { token ->
        execute { client.post(endpoint("me/avatar")) {
            bearerAuth(token)
            setBody(MultiPartFormDataContent(formData {
                append("photo", bytes, Headers.build {
                    append(HttpHeaders.ContentType, mimeType)
                    append(HttpHeaders.ContentDisposition, "filename=profile.${if (mimeType == "image/png") "png" else "jpg"}")
                })
            }))
        } }
    }

    override suspend fun removeAvatar(): ApiResult<User> = authorized { token ->
        execute { client.delete(endpoint("me/avatar")) { bearerAuth(token) } }
    }

    override suspend fun getAvatarImage(url: String): ByteArray? = try {
        val target = Url(url)
        val origin = Url(baseUrl)
        if (target.host != origin.host || target.protocol != origin.protocol || target.port != origin.port || target.user != null || target.password != null) null
        else {
            val photoClient = client.config { followRedirects = false }
            try {
                // Stream a bounded public image; never attach a bearer or follow a redirect.
                photoClient.prepareGet(url).execute { response ->
                    if (response.status != HttpStatusCode.OK || response.contentType()?.withoutParameters() !in listOf(ContentType.Image.JPEG, ContentType.Image.PNG)) null
                    else response.bodyAsChannel().readRemaining(5L * 1024 * 1024 + 1).readByteArray().takeIf { it.size <= 5 * 1024 * 1024 }
                }
            } finally { photoClient.close() }
        }
    } catch (error: CancellationException) { throw error } catch (_: Throwable) { null }

    override suspend fun getMe(): ApiResult<User> = authorizedGet("me")

    override suspend fun getCourses(): ApiResult<List<Course>> =
        authorizedGet<List<Course>>("courses").mapSuccess { courses -> courses.map(Course::withDecodedText) }

    override suspend fun getDashboard(): ApiResult<DashboardResponse> =
        authorizedGet<DashboardResponse>("dashboard").mapSuccess { dashboard ->
            dashboard.copy(
                courses = dashboard.courses.map(Course::withDecodedText),
                upcomingLiveClasses = dashboard.upcomingLiveClasses.copy(
                    items = dashboard.upcomingLiveClasses.items.map { item ->
                        item.copy(courseTitle = item.courseTitle.decodeHtmlEntities(), title = item.title.decodeHtmlEntities())
                    },
                ),
                events = dashboard.events.copy(
                    items = dashboard.events.items.map { item ->
                        item.copy(
                            title = item.title.decodeHtmlEntities(),
                            actionLabel = item.actionLabel.decodeHtmlEntities(),
                        )
                    },
                ),
                supportActions = dashboard.supportActions.copy(
                    items = dashboard.supportActions.items.map { it.copy(label = it.label.decodeHtmlEntities()) },
                ),
            )
        }

    override suspend fun getCourse(id: Long): ApiResult<Course> =
        authorizedGet<Course>("courses/$id").mapSuccess(Course::withDecodedText)

    override suspend fun getCourseContent(id: Long): ApiResult<CourseContent> =
        authorizedGet<CourseContent>("courses/$id/content").mapSuccess(CourseContent::withDecodedText)

    override suspend fun getLesson(courseId: Long, lessonId: Long): ApiResult<Lesson> = authorized<Lesson> { token ->
        execute<Lesson> {
            client.get(endpoint("lessons/$lessonId")) {
                bearerAuth(token)
                url.parameters.append("courseId", courseId.toString())
            }
        }
    }.mapSuccess(Lesson::withDecodedText)

    override suspend fun saveLessonProgress(
        lessonId: Long,
        request: ProgressRequest,
    ): ApiResult<ProgressResponse> = authorized { token ->
        execute {
            client.post(endpoint("lessons/$lessonId/progress")) {
                bearerAuth(token)
                contentType(ContentType.Application.Json)
                setBody(request)
            }
        }
    }

    override suspend fun getCourseProgress(courseId: Long): ApiResult<CourseProgress> =
        authorizedGet("courses/$courseId/progress")

    override suspend fun authorizePlayback(
        courseId: Long,
        lessonId: Long,
    ): ApiResult<PlaybackResponse> = authorized { token ->
        execute {
            client.post(endpoint("video/playback")) {
                bearerAuth(token)
                contentType(ContentType.Application.Json)
                setBody(PlaybackRequest(courseId = courseId, lessonId = lessonId))
            }
        }
    }

    override suspend fun createQuizWebLaunch(request: QuizWebLaunchRequest): ApiResult<QuizWebLaunch> =
        authorized { token ->
            when (val result = execute<QuizWebLaunchWireResponse> {
                client.post(endpoint("quiz/web-launch")) {
                    bearerAuth(token)
                    contentType(ContentType.Application.Json)
                    setBody(request)
                }
            }) {
                is ApiResult.Success -> result.value.toLaunch(request)
                is ApiResult.Failure -> result
            }
        }

    override suspend fun getEntitlements(): ApiResult<List<Entitlement>> = authorizedGet("entitlements")

    private suspend inline fun <reified T> authorizedGet(path: String): ApiResult<T> = authorized { token ->
        execute {
            client.get(endpoint(path)) {
                bearerAuth(token)
            }
        }
    }

    private suspend inline fun <T> authorized(
        request: (String) -> ApiResult<T>,
    ): ApiResult<T> {
        val token = accessTokenProvider.accessToken()?.takeIf(String::isNotBlank)
            ?: return ApiResult.Failure(AcademyApiError.MissingAccessToken())
        return request(token)
    }

    private suspend inline fun <reified T> execute(
        request: () -> HttpResponse,
    ): ApiResult<T> = try {
        val response = request()
        if (response.status.value in 200..299) {
            ApiResult.Success(response.body<T>())
        } else {
            ApiResult.Failure(mapHttpError(response))
        }
    } catch (cause: CancellationException) {
        throw cause
    } catch (cause: HttpRequestTimeoutException) {
        ApiResult.Failure(AcademyApiError.Timeout(cause))
    } catch (cause: ConnectTimeoutException) {
        ApiResult.Failure(AcademyApiError.Timeout(cause))
    } catch (cause: SocketTimeoutException) {
        ApiResult.Failure(AcademyApiError.Timeout(cause))
    } catch (cause: ContentConvertException) {
        ApiResult.Failure(AcademyApiError.InvalidResponse(cause))
    } catch (cause: SerializationException) {
        ApiResult.Failure(AcademyApiError.InvalidResponse(cause))
    } catch (cause: IOException) {
        ApiResult.Failure(AcademyApiError.NoConnection(cause))
    } catch (cause: Throwable) {
        ApiResult.Failure(AcademyApiError.UnexpectedFailure(cause))
    }

    private suspend fun mapHttpError(response: HttpResponse): AcademyApiError {
        val details = try {
            response.body<ApiError>()
        } catch (cause: CancellationException) {
            throw cause
        } catch (_: Throwable) {
            null
        }
        return when (response.status) {
            HttpStatusCode.BadRequest -> AcademyApiError.BadRequest(details)
            HttpStatusCode.Unauthorized -> AcademyApiError.Unauthorized(details)
            HttpStatusCode.Forbidden -> AcademyApiError.Forbidden(details)
            HttpStatusCode.NotFound -> AcademyApiError.NotFound(details)
            HttpStatusCode.Gone -> AcademyApiError.Gone(details)
            HttpStatusCode.Conflict -> AcademyApiError.Conflict(details)
            HttpStatusCode.TooManyRequests -> AcademyApiError.RateLimited(details)
            HttpStatusCode.InternalServerError,
            HttpStatusCode.ServiceUnavailable,
            -> AcademyApiError.Server(response.status.value, details)
            else -> AcademyApiError.UnexpectedHttpStatus(response.status.value, details)
        }
    }

    private fun endpoint(path: String): String = baseUrl + path.trimStart('/')

    private fun QuizWebLaunchWireResponse.toLaunch(
        request: QuizWebLaunchRequest,
    ): ApiResult<QuizWebLaunch> {
        if (
            userId < 1 || courseId != request.courseId || quizId != request.quizId ||
            !ticket.matches(Regex("^[A-Fa-f0-9]{64}$"))
        ) {
            return ApiResult.Failure(
                AcademyApiError.InvalidResponse(IllegalArgumentException("Quiz launch identity is invalid.")),
            )
        }
        val redeemUrl = resolveFirstPartyPath(redeemPath)
        val logoutUrl = resolveFirstPartyPath(logoutPath)
        val quizUrl = resolveFirstPartyPath(quizPath)
        if (redeemUrl == null || logoutUrl == null || quizUrl == null) {
            return ApiResult.Failure(
                AcademyApiError.InvalidResponse(IllegalArgumentException("Quiz launch path is invalid.")),
            )
        }
        return ApiResult.Success(
            QuizWebLaunch(
                userId = userId,
                courseId = courseId,
                quizId = quizId,
                ticket = ticket,
                redeemUrl = redeemUrl,
                logoutUrl = logoutUrl,
                quizUrl = quizUrl,
                expiresAt = expiresAt,
            ),
        )
    }

    private fun resolveFirstPartyPath(path: String): String? {
        if (!path.startsWith('/') || path.startsWith("//") || '#' in path) return null
        val api = runCatching { Url(baseUrl) }.getOrNull() ?: return null
        val authority = buildString {
            append(api.protocol.name)
            append("://")
            append(api.host)
            if (api.port != api.protocol.defaultPort) append(":${api.port}")
        }
        return runCatching { Url(authority + path).toString() }.getOrNull()
    }
}

private inline fun <T, R> ApiResult<T>.mapSuccess(transform: (T) -> R): ApiResult<R> = when (this) {
    is ApiResult.Success -> ApiResult.Success(transform(value))
    is ApiResult.Failure -> this
}
