package com.example.academyapp.api

import com.example.academyapp.api.model.Course
import com.example.academyapp.api.model.DashboardResponse
import com.example.academyapp.api.model.AuthOptions
import com.example.academyapp.api.model.CourseContent
import com.example.academyapp.api.model.CourseProgress
import com.example.academyapp.api.model.Entitlement
import com.example.academyapp.api.model.Lesson
import com.example.academyapp.api.model.LoginResponse
import com.example.academyapp.api.model.PasswordResetResponse
import com.example.academyapp.api.model.RegisterRequest
import com.example.academyapp.api.model.LogoutResponse
import com.example.academyapp.api.model.PlaybackResponse
import com.example.academyapp.api.model.ProgressRequest
import com.example.academyapp.api.model.ProgressResponse
import com.example.academyapp.api.model.QuizWebLaunch
import com.example.academyapp.api.model.QuizWebLaunchRequest
import com.example.academyapp.api.model.User

fun interface AccessTokenProvider {
    suspend fun accessToken(): String?
}

interface AcademyApi {
    /** Host from the app-configured API URL, used only for first-party link allowlisting. */
    val configuredApiHost: String? get() = null
    suspend fun getAuthOptions(): ApiResult<AuthOptions> = ApiResult.Success(AuthOptions(false))
    suspend fun login(email: String, password: String): ApiResult<LoginResponse>
    suspend fun register(request: RegisterRequest): ApiResult<LoginResponse> =
        ApiResult.Failure(AcademyApiError.UnexpectedFailure(UnsupportedOperationException("Registration is not implemented.")))
    suspend fun requestPasswordReset(email: String): ApiResult<PasswordResetResponse> =
        ApiResult.Failure(AcademyApiError.UnexpectedFailure(UnsupportedOperationException("Password reset is not implemented.")))
    suspend fun refresh(refreshToken: String): ApiResult<LoginResponse>
    suspend fun logout(): ApiResult<LogoutResponse>
    suspend fun requestAccountDeletion(): ApiResult<com.example.academyapp.api.model.DeletionRequestResponse> =
        ApiResult.Failure(AcademyApiError.UnexpectedFailure(UnsupportedOperationException("Account deletion is not implemented.")))
    suspend fun getAvatarImage(url: String): ByteArray? = null
    suspend fun getMe(): ApiResult<User>
    suspend fun updateProfile(request: com.example.academyapp.api.model.ProfileUpdateRequest): ApiResult<User> =
        ApiResult.Failure(AcademyApiError.UnexpectedFailure(UnsupportedOperationException("Profile is not implemented.")))
    suspend fun uploadAvatar(bytes: ByteArray, mimeType: String): ApiResult<User> =
        ApiResult.Failure(AcademyApiError.UnexpectedFailure(UnsupportedOperationException("Avatar is not implemented.")))
    suspend fun removeAvatar(): ApiResult<User> =
        ApiResult.Failure(AcademyApiError.UnexpectedFailure(UnsupportedOperationException("Avatar is not implemented.")))
    suspend fun getCourses(): ApiResult<List<Course>>
    suspend fun getDashboard(): ApiResult<DashboardResponse> =
        ApiResult.Failure(AcademyApiError.UnexpectedFailure(UnsupportedOperationException("Dashboard is not implemented.")))
    suspend fun getCourse(id: Long): ApiResult<Course>
    suspend fun getCourseContent(id: Long): ApiResult<CourseContent>
    suspend fun getLesson(courseId: Long, lessonId: Long): ApiResult<Lesson>
    suspend fun saveLessonProgress(lessonId: Long, request: ProgressRequest): ApiResult<ProgressResponse>
    suspend fun getCourseProgress(courseId: Long): ApiResult<CourseProgress>
    suspend fun authorizePlayback(courseId: Long, lessonId: Long): ApiResult<PlaybackResponse>
    suspend fun createQuizWebLaunch(request: QuizWebLaunchRequest): ApiResult<QuizWebLaunch>
    suspend fun getEntitlements(): ApiResult<List<Entitlement>>
}
