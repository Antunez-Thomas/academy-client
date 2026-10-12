package com.example.academyapp.design.components

import kotlin.test.Test
import kotlin.test.assertEquals

class AcademyProgressIndicatorTest {
    @Test
    fun progressValueUsesClampedRange() {
        assertEquals(0, normalizeProgress(-10))
        assertEquals(45, normalizeProgress(45))
        assertEquals(100, normalizeProgress(140))
    }
}
