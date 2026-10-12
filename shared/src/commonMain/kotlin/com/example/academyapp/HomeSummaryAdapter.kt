package com.example.academyapp

import com.example.academyapp.api.model.Course
import com.example.academyapp.course.CourseListUiState
import com.example.academyapp.features.home.presentation.homeSummary

/** Cross-feature adaptation belongs to app composition, not Home presentation. */
internal fun CourseListUiState.toHomeSummary(displayName: String) =
    homeSummary(displayName, (this as? CourseListUiState.Content)?.courses)

/** Resolve IDs against current data before navigating; ignore a stale Home card. */
internal fun openHomeCourse(
    courseId: Long,
    state: CourseListUiState,
    onNavigate: () -> Unit,
    onOpenCourse: (Course) -> Unit,
) {
    val course = (state as? CourseListUiState.Content)?.courses
        ?.firstOrNull { it.id == courseId } ?: return
    onNavigate()
    onOpenCourse(course)
}
