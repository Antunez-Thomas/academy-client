package com.example.academyapp.course

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LessonResourceUrlPolicyTest {
    @Test
    fun acceptsHttpsResourceLinks() {
        assertTrue(isLessonResourceUrlOpenable("https://academy.test/wp-content/uploads/handout.pdf"))
    }

    @Test
    fun acceptsOnlyLocalDevelopmentHttpHosts() {
        assertTrue(isLessonResourceUrlOpenable("http://localhost:8080/wp-content/uploads/handout.pdf"))
        assertTrue(isLessonResourceUrlOpenable("http://10.0.2.2:8080/wp-content/uploads/handout.pdf"))
        assertFalse(isLessonResourceUrlOpenable("http://academy.test/wp-content/uploads/handout.pdf"))
        assertFalse(isLessonResourceUrlOpenable("http://localhost.example.test/handout.pdf"))
        assertFalse(isLessonResourceUrlOpenable("http://localhost:not-a-port/handout.pdf"))
    }

    @Test
    fun rejectsMalformedOrUnsafeSchemesAndUserInfo() {
        assertFalse(isLessonResourceUrlOpenable("javascript:alert(1)"))
        assertFalse(isLessonResourceUrlOpenable("https://user@academy.test/handout.pdf"))
        assertFalse(isLessonResourceUrlOpenable("https://"))
        assertFalse(isLessonResourceUrlOpenable("file:///data/handout.pdf"))
    }
}
