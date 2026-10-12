package com.example.academyapp.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(
    val email: String,
    val password: String,
)

@Serializable
data class RegisterRequest(
    val email: String,
    val password: String,
    val acceptLegalAgreement: Boolean,
    val displayName: String? = null,
)

@Serializable
data class PasswordResetRequest(val email: String)

@Serializable
data class AuthOptions(
    val registrationAvailable: Boolean,
    val legalAgreementRequired: Boolean = false,
    val termsUrl: String? = null,
    val privacyUrl: String? = null,
)

@Serializable
data class PasswordResetResponse(val ok: Boolean, val message: String)

@Serializable
data class RefreshRequest(
    val refreshToken: String,
)

@Serializable
data class LoginResponse(
    val user: User,
    val accessToken: String,
    val refreshToken: String,
    val accessTokenExpiresAt: String,
)

@Serializable
data class User(
    val id: Long,
    val email: String,
    val displayName: String,
    val username: String = "",
    val firstName: String = "",
    val surname: String = "",
    val phone: String = "",
    val avatarUrl: String? = null,
)

@Serializable
data class ProfileUpdateRequest(val firstName: String, val surname: String, val phone: String)

@Serializable
data class Course(
    val id: Long,
    val title: String,
    val excerpt: String,
    val imageUrl: String?,
    val progressPercent: Int,
    val categories: List<String>,
)

@Serializable
enum class DashboardSectionStatus {
    @SerialName("ready") READY,
    @SerialName("unavailable") UNAVAILABLE,
}

@Serializable
data class DashboardResponse(
    val courses: List<Course>,
    val upcomingLiveClasses: DashboardLiveClassSection,
    val events: DashboardEventSection,
    val supportActions: DashboardSupportActionSection,
)

@Serializable
data class DashboardLiveClassSection(
    val status: DashboardSectionStatus,
    val items: List<DashboardLiveClass>,
)

@Serializable
data class DashboardLiveClass(
    val courseId: Long,
    val lessonId: Long,
    val courseTitle: String,
    val title: String,
    val startAt: String,
    val endAt: String,
    val joinable: Boolean,
    val joinUrl: String?,
)

@Serializable
data class DashboardEventSection(
    val status: DashboardSectionStatus,
    val items: List<DashboardEvent>,
)

@Serializable
data class DashboardEvent(
    val title: String,
    val startAt: String,
    val endAt: String,
    val status: DashboardEventStatus,
    val joinable: Boolean,
    val actionLabel: String,
    val joinUrl: String?,
)

@Serializable
enum class DashboardEventStatus {
    @SerialName("scheduled") SCHEDULED,
    @SerialName("starting") STARTING,
    @SerialName("live") LIVE,
}

@Serializable
data class DashboardSupportActionSection(
    val status: DashboardSectionStatus,
    val items: List<DashboardSupportAction>,
)

@Serializable
data class DashboardSupportAction(
    val label: String,
    val kind: String,
    val url: String,
)

@Serializable
data class CourseContent(
    val course: Course,
    val steps: List<Lesson>,
    val quizzes: List<Quiz> = emptyList(),
    val sectionHeadings: List<CourseSectionHeading> = emptyList(),
)

@Serializable
data class CourseSectionHeading(
    val title: String,
    val beforeStepId: Long,
)

@Serializable
data class Quiz(
    val id: Long,
    val courseId: Long,
    val title: String,
    val position: Int,
    val completed: Boolean,
    val parentLessonId: Long? = null,
)

@Serializable
data class Lesson(
    val id: Long,
    val courseId: Long,
    val parentLessonId: Long?,
    val type: LessonType,
    val title: String,
    val position: Int,
    val durationSeconds: Int?,
    val completed: Boolean,
    val positionSeconds: Int,
    val video: Video?,
    val lastViewedAt: String? = null,
    val resources: List<LessonResource> = emptyList(),
)

@Serializable
data class LessonResource(
    val id: Long,
    val title: String,
    val mediaType: String,
    val url: String,
)

@Serializable
enum class LessonType {
    @SerialName("lesson")
    LESSON,

    @SerialName("topic")
    TOPIC,
}

@Serializable
data class Video(
    val videoId: String,
    val libraryId: Long,
)

@Serializable
data class ProgressRequest(
    val courseId: Long,
    val completed: Boolean,
    val positionSeconds: Int,
    val clientEventId: String,
)

@Serializable
data class ProgressResponse(
    val lessonId: Long,
    val courseId: Long,
    val completed: Boolean,
    val positionSeconds: Int,
    val accepted: Boolean,
    val clientEventId: String,
    val duplicate: Boolean,
)

@Serializable
data class PlaybackRequest(
    val courseId: Long,
    val lessonId: Long,
)

@Serializable
data class PlaybackResponse(
    val lessonId: Long,
    val videoId: String,
    val libraryId: Long,
    val token: String,
    val expires: Long,
    val tokenScheme: String,
    val playbackUrl: String,
    val expiresAt: String,
)

@Serializable
data class QuizWebLaunchRequest(
    val courseId: Long,
    val quizId: Long,
    val clientRequestId: String,
)

@Serializable
internal class QuizWebLaunchWireResponse(
    val userId: Long,
    val courseId: Long,
    val quizId: Long,
    val ticket: String,
    val redeemPath: String,
    val logoutPath: String,
    val quizPath: String,
    val expiresAt: String,
) {
    override fun toString(): String =
        "QuizWebLaunchWireResponse(userId=$userId, courseId=$courseId, quizId=$quizId, ticket=[redacted], expiresAt=$expiresAt)"
}

class QuizWebLaunch internal constructor(
    val userId: Long,
    val courseId: Long,
    val quizId: Long,
    val ticket: String,
    val redeemUrl: String,
    val logoutUrl: String,
    val quizUrl: String,
    val expiresAt: String,
) {
    override fun toString(): String =
        "QuizWebLaunch(userId=$userId, courseId=$courseId, quizId=$quizId, ticket=[redacted], expiresAt=$expiresAt)"
}

@Serializable
data class Entitlement(
    val userId: Long,
    val courseId: Long,
    val source: String,
    val status: String,
    val expiresAt: String?,
    val transactionId: String?,
)

@Serializable
data class ApiError(
    val code: String,
    val message: String,
    val data: ApiErrorData,
)

@Serializable
data class ApiErrorData(
    val status: Int,
    val fieldErrors: Map<String, String> = emptyMap(),
)

@Serializable
data class LogoutResponse(
    val ok: Boolean,
)

@Serializable
data class CourseProgress(
    val courseId: Long,
    val progressPercent: Int,
)

@kotlinx.serialization.Serializable
data class DeletionRequestResponse(val state: String, val confirmationRequired: Boolean)
