package com.example.academyapp.course

import com.example.academyapp.api.model.Course

const val COURSE_DASHBOARD_PREVIEW_LIMIT = 3

/** Shows active learning first while preserving API order within each progress group. */
fun dashboardPreviewCourses(courses: List<Course>): List<Course> =
    coursesForLearning(courses).take(COURSE_DASHBOARD_PREVIEW_LIMIT)

fun coursesForLearning(courses: List<Course>): List<Course> =
    courses.withIndex()
        .sortedWith(compareBy<IndexedValue<Course>> { (_, course) ->
            if (course.progressPercent in 1..99) 0 else 1
        }.thenBy { it.index })
        .map { it.value }
