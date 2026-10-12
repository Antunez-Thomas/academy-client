package com.example.academyapp.api

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class IosAcademyApiEnvironmentTest {
    @Test
    fun selectsIosSimulatorLocalUrl() {
        val local = AcademyApiEnvironments.configurationFor(AcademyEnvironment.LOCAL)

        assertEquals("http://127.0.0.1:8080/wp-json/academy-mobile/v1/", local.baseUrl)
        assertTrue(local.isEnabled)
    }
}
