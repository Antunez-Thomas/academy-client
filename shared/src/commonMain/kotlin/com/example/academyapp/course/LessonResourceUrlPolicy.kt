package com.example.academyapp.course

internal fun isLessonResourceUrlOpenable(value: String): Boolean {
    val match = Regex("^(https?)://([^/?#]+)(?:[/?#].*)?$", RegexOption.IGNORE_CASE).matchEntire(value)
        ?: return false
    val scheme = match.groupValues[1].lowercase()
    val authority = match.groupValues[2]
    if (authority.isBlank() || authority.contains('@')) return false
    if (scheme == "https") return true

    return Regex("^(localhost|127\\.0\\.0\\.1|10\\.0\\.2\\.2)(:\\d+)?$", RegexOption.IGNORE_CASE)
        .matches(authority)
        || Regex("^\\[::1\\](:\\d+)?$", RegexOption.IGNORE_CASE).matches(authority)
}
