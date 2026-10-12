package com.example.academyapp.course

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LessonCompletionPresentationTest {
    @Test
    fun savedAndPreviouslyCompletedStatesProduceOneNoticeVariant() {
        assertEquals(LessonCompletionNotice.Saved, lessonCompletionNotice(LessonCompletionUiState.Saved))
        assertEquals(
            LessonCompletionNotice.AlreadyCompleted,
            lessonCompletionNotice(LessonCompletionUiState.AlreadyCompleted),
        )
    }

    @Test
    fun actionAndFailureStatesDoNotProduceCompletionNotice() {
        assertNull(lessonCompletionNotice(LessonCompletionUiState.Hidden))
        assertNull(lessonCompletionNotice(LessonCompletionUiState.Available))
        assertNull(lessonCompletionNotice(LessonCompletionUiState.Saving))
        assertNull(lessonCompletionNotice(LessonCompletionUiState.Error("No se pudo guardar")))
    }
}
