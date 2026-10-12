package com.example.academyapp.course.navigation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LearningNavigationTest {
    @Test
    fun ordinaryRoutesRestoreWithExplicitParentHistory() {
        for (picker in listOf(false, true)) {
            val routes = buildList {
                add(LearningRoute.Dashboard)
                if (picker) add(LearningRoute.Courses)
                add(LearningRoute.Curriculum(42))
                add(LearningRoute.LessonDetail(42, 7))
            }
            val restored = restoreLearningNavigation(1, saveLearningNavigation(1, routes))!!
            assertEquals(routes, restored.routes)
            assertFalse(restored.interruptedQuiz)
        }
    }

    @Test
    fun interruptedQuizRestoresOnlyCurriculumAndCleanupMarker() {
        val routes = listOf(LearningRoute.Dashboard, LearningRoute.Curriculum(42), LearningRoute.QuizWeb(42, 9))
        val saved = saveLearningNavigation(1, routes)
        val restored = restoreLearningNavigation(1, saved)!!
        assertEquals(routes.dropLast(1), restored.routes)
        assertTrue(restored.interruptedQuiz)
        assertFalse(saved.contains("http"))
        assertFalse(saved.contains("token"))
    }

    @Test
    fun repeatedProcessDeathPreservesPendingCleanupWithoutQuizRoute() {
        val routes = listOf(LearningRoute.Dashboard, LearningRoute.Curriculum(42))
        val restored = restoreLearningNavigation(1, saveLearningNavigation(1, routes, cleanupRequired = true))!!
        assertEquals(routes, restored.routes)
        assertTrue(restored.interruptedQuiz)
    }

    @Test
    fun foreignAccountCorruptAndInvalidHistoryCannotRestore() {
        val saved = saveLearningNavigation(1, listOf(LearningRoute.Dashboard, LearningRoute.Curriculum(42)))
        assertNull(restoreLearningNavigation(2, saved))
        assertNull(restoreLearningNavigation(1, "invalid"))
        for (routes in listOf(
            emptyList(), listOf(LearningRoute.Courses),
            listOf(LearningRoute.Dashboard, LearningRoute.Curriculum(-1)),
            listOf(LearningRoute.Dashboard, LearningRoute.Curriculum(42), LearningRoute.LessonDetail(99, 7)),
            listOf(LearningRoute.Dashboard, LearningRoute.Curriculum(42), LearningRoute.QuizWeb(42, 9), LearningRoute.Courses),
        )) assertNull(restoreLearningNavigation(1, saveLearningNavigation(1, routes)))
    }
}
