package com.example.academyapp.api

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AndroidAcademyApiEnvironmentTest {
    @Test
    fun selectsAndroidEmulatorLocalUrl() {
        val local = AcademyApiEnvironments.configurationFor(AcademyEnvironment.LOCAL)

        assertEquals("http://10.0.2.2:8080/wp-json/academy-mobile/v1/", local.baseUrl)
        assertTrue(local.isEnabled)
    }
}
