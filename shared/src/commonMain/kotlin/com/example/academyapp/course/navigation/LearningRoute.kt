package com.example.academyapp.course.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

@Serializable
sealed interface LearningRoute : NavKey {
    @Serializable data object Dashboard : LearningRoute
    @Serializable data object Courses : LearningRoute
    @Serializable data class Curriculum(val courseId: Long) : LearningRoute
    @Serializable data class LessonDetail(val courseId: Long, val lessonId: Long) : LearningRoute
    @Serializable data class QuizWeb(val courseId: Long, val quizId: Long) : LearningRoute
}

/** Explicit open-polymorphic registration: identical on Android and iOS, no reflection. */
internal val learningSerializers = SerializersModule {
    polymorphic(NavKey::class) {
        subclass(LearningRoute.Dashboard::class, LearningRoute.Dashboard.serializer())
        subclass(LearningRoute.Courses::class, LearningRoute.Courses.serializer())
        subclass(LearningRoute.Curriculum::class, LearningRoute.Curriculum.serializer())
        subclass(LearningRoute.LessonDetail::class, LearningRoute.LessonDetail.serializer())
        subclass(LearningRoute.QuizWeb::class, LearningRoute.QuizWeb.serializer())
    }
}
internal val learningJson = Json { serializersModule = learningSerializers }
internal val learningKeysSerializer = ListSerializer(PolymorphicSerializer(NavKey::class))

@Serializable
internal data class SavedLearningNavigation(
    val accountId: Long,
    val routes: String,
    val cleanupRequired: Boolean = false,
)

internal data class RestoredLearningNavigation(
    val routes: List<LearningRoute>,
    val interruptedQuiz: Boolean,
)

internal fun saveLearningNavigation(
    accountId: Long,
    routes: List<LearningRoute>,
    cleanupRequired: Boolean = false,
): String = learningJson.encodeToString(
    SavedLearningNavigation(accountId, learningJson.encodeToString(learningKeysSerializer, routes), cleanupRequired),
)

/** Validate history and discard an interrupted quiz before any UI can host its WebView. */
internal fun restoreLearningNavigation(accountId: Long, saved: String): RestoredLearningNavigation? =
    runCatching {
        val snapshot = learningJson.decodeFromString<SavedLearningNavigation>(saved)
        require(snapshot.accountId == accountId)
        val routes = learningJson.decodeFromString(learningKeysSerializer, snapshot.routes)
            .map { it as LearningRoute }
        require(routes.firstOrNull() == LearningRoute.Dashboard && routes.size <= 5)
        var index = 1
        if (routes.getOrNull(index) == LearningRoute.Courses) index++
        val curriculum = routes.getOrNull(index) as? LearningRoute.Curriculum
        if (index < routes.size) {
            require(curriculum != null && curriculum.courseId > 0)
            index++
        }
        when (val child = routes.getOrNull(index)) {
            is LearningRoute.LessonDetail -> {
                require(child.courseId == curriculum?.courseId && child.lessonId > 0)
                index++
            }
            is LearningRoute.QuizWeb -> {
                require(child.courseId == curriculum?.courseId && child.quizId > 0)
                index++
            }
            null -> Unit
            else -> error("Invalid child history")
        }
        require(index == routes.size)
        val interrupted = routes.lastOrNull() is LearningRoute.QuizWeb
        RestoredLearningNavigation(if (interrupted) routes.dropLast(1) else routes, interrupted || snapshot.cleanupRequired)
    }.getOrNull()
