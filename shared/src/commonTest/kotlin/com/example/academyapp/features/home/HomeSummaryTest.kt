package com.example.academyapp.features.home

import com.example.academyapp.api.model.Course
import com.example.academyapp.course.CourseListUiState
import com.example.academyapp.features.home.presentation.homeSummary
import com.example.academyapp.openHomeCourse
import com.example.academyapp.toHomeSummary
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HomeSummaryTest {
    @Test
    fun nonContentStatesPreserveFallback() {
        for (state in listOf(
            CourseListUiState.SignedOut, CourseListUiState.Loading,
            CourseListUiState.Empty, CourseListUiState.Error("synthetic error"),
        )) {
            val summary = state.toHomeSummary("Ana García")
            assertFalse(summary.hasCourseContent)
            assertTrue(summary.courses.isEmpty())
            assertNull(summary.heroCourse)
        }
    }

    @Test
    fun emptyContentRemainsLoaded() {
        val summary = CourseListUiState.Content(emptyList()).toHomeSummary("Ana García")
        assertTrue(summary.hasCourseContent)
        assertTrue(summary.courses.isEmpty())
        assertNull(summary.heroCourse)
    }

    @Test
    fun greetingPreservesFirstNameAndBlankFallback() {
        assertEquals("Ana", homeSummary("  Ana García  ", null).greetingName)
        assertEquals("Ana", homeSummary("Ana\tGarcía", null).greetingName)
        assertEquals("   ", homeSummary("   ", null).greetingName)
        assertEquals("", homeSummary("", null).greetingName)
    }

    @Test
    fun heroUsesExactlyOneInProgressCourseAcrossEntireList() {
        val courses = listOf(course(1, 0), course(2, 100), course(3, 0), course(4, 42))
        assertEquals(courses[3], homeSummary("Ana", courses).heroCourse)
        assertNull(homeSummary("Ana", courses + course(5, 50)).heroCourse)
        assertNull(homeSummary("Ana", courses.take(3)).heroCourse)
    }

    @Test
    fun heroIncludesProgressBoundariesAndExcludesInvalidValues() {
        for (progress in listOf(1, 99)) {
            assertEquals(progress, homeSummary("Ana", listOf(course(1, progress))).heroCourse?.progressPercent)
        }
        for (progress in listOf(-1, 0, 100, 101)) {
            assertNull(homeSummary("Ana", listOf(course(1, progress))).heroCourse)
        }
    }

    @Test
    fun courseCardsRetainOrderAndThreeCourseLimit() {
        val courses = listOf(course(4), course(2), course(3), course(1))
        assertEquals(courses.take(3), homeSummary("Ana", courses).courses)
        assertEquals(courses.take(1), homeSummary("Ana", courses.take(1)).courses)
    }

    @Test
    fun courseTapNavigatesBeforeOpeningCurrentCourseData() {
        val current = course(7, 99)
        val actions = mutableListOf<String>()
        openHomeCourse(7, CourseListUiState.Content(listOf(current)),
            onNavigate = { actions += "my-learning" },
            onOpenCourse = { assertEquals(current, it); actions += "open-course" },
        )
        assertEquals(listOf("my-learning", "open-course"), actions)
    }

    @Test
    fun staleCourseTapCannotNavigateOrOpenAnotherCourse() {
        for (state in listOf(CourseListUiState.Loading, CourseListUiState.Content(listOf(course(2))))) {
            openHomeCourse(7, state,
                onNavigate = { error("unexpected navigation") },
                onOpenCourse = { error("unexpected course open") },
            )
        }
    }

    private fun course(id: Long, progress: Int = 0) =
        Course(id, "Curso $id", "", null, progress, emptyList())
}
