package com.example.academyapp.api

import com.example.academyapp.api.model.Course
import com.example.academyapp.api.model.CourseContent
import com.example.academyapp.api.model.Lesson
import com.example.academyapp.api.model.Quiz

private val htmlEntityPattern = Regex(
    "&(#(?:[xX][0-9a-fA-F]+|[0-9]+)|amp|lt|gt|quot|apos|nbsp|ndash|mdash|hellip|lsquo|rsquo|ldquo|rdquo);",
)

internal fun String.decodeHtmlEntities(): String = htmlEntityPattern.replace(this) { match ->
    val entity = match.groupValues[1]
    val codePoint = when {
        entity.startsWith("#x", ignoreCase = true) -> entity.drop(2).toIntOrNull(16)
        entity.startsWith('#') -> entity.drop(1).toIntOrNull()
        else -> null
    }
    if (codePoint != null) return@replace codePoint.toUnicodeString()

    when (entity) {
        "amp" -> "&"
        "lt" -> "<"
        "gt" -> ">"
        "quot" -> "\""
        "apos" -> "'"
        "nbsp" -> " "
        "ndash" -> "–"
        "mdash" -> "—"
        "hellip" -> "…"
        "lsquo" -> "‘"
        "rsquo" -> "’"
        "ldquo" -> "“"
        "rdquo" -> "”"
        else -> match.value
    }
}

private fun Int.toUnicodeString(): String {
    if (this !in 0..0x10FFFF || this in 0xD800..0xDFFF) return "\uFFFD"
    if (this <= 0xFFFF) return toChar().toString()
    val codePoint = this - 0x10000
    val highSurrogate = 0xD800 + (codePoint shr 10)
    val lowSurrogate = 0xDC00 + (codePoint and 0x3FF)
    return highSurrogate.toChar().toString() + lowSurrogate.toChar()
}

internal fun Course.withDecodedText(): Course = copy(
    title = title.decodeHtmlEntities(),
    categories = categories.map(String::decodeHtmlEntities),
)

internal fun Lesson.withDecodedText(): Lesson = copy(title = title.decodeHtmlEntities())

internal fun Quiz.withDecodedText(): Quiz = copy(title = title.decodeHtmlEntities())

internal fun CourseContent.withDecodedText(): CourseContent = copy(
    course = course.withDecodedText(),
    steps = steps.map(Lesson::withDecodedText),
    quizzes = quizzes.map(Quiz::withDecodedText),
    sectionHeadings = sectionHeadings.map { it.copy(title = it.title.decodeHtmlEntities()) },
)
