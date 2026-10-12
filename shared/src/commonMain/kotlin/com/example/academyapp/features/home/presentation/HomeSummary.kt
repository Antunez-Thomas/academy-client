package com.example.academyapp.features.home.presentation

import com.example.academyapp.api.model.Course

/** Display data only; Home does not own Learning state or navigation. */
internal data class HomeSummary(
    val greetingName: String,
    val hasCourseContent: Boolean,
    val heroCourse: Course?,
    val courses: List<Course>,
)

/** Called by composition with stable shared course data, never Learning screen state. */
internal fun homeSummary(displayName: String, courses: List<Course>?): HomeSummary = HomeSummary(
    greetingName = displayName.trim().takeWhile { !it.isWhitespace() }.ifBlank { displayName },
    hasCourseContent = courses != null,
    heroCourse = courses.orEmpty().filter { it.progressPercent in 1..99 }.singleOrNull(),
    courses = courses.orEmpty().take(3),
)
