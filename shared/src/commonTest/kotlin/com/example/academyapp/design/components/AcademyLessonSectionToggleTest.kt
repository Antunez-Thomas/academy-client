package com.example.academyapp.design.components

import kotlin.test.Test
import kotlin.test.assertEquals

class AcademyLessonSectionToggleTest {
    @Test
    fun expandedSectionsUseUpChevronAndCollapsedSectionsUseRightChevron() {
        assertEquals(AcademySectionToggleDirection.ExpandedUp, sectionToggleDirection(expanded = true))
        assertEquals(AcademySectionToggleDirection.CollapsedRight, sectionToggleDirection(expanded = false))
    }
}
