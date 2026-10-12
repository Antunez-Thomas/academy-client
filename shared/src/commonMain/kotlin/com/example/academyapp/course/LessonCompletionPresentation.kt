package com.example.academyapp.course

/** App-owned completion notice displayed after lesson playback content. */
enum class LessonCompletionNotice {
    Saved,
    AlreadyCompleted,
}

fun lessonCompletionNotice(state: LessonCompletionUiState): LessonCompletionNotice? = when (state) {
    LessonCompletionUiState.Saved -> LessonCompletionNotice.Saved
    LessonCompletionUiState.AlreadyCompleted -> LessonCompletionNotice.AlreadyCompleted
    else -> null
}
