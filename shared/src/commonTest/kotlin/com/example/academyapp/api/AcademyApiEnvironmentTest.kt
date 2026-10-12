package com.example.academyapp.api

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AcademyApiEnvironmentTest {
    @Test
    fun selectsStagingUrl() {
        val staging = AcademyApiEnvironments.configurationFor(AcademyEnvironment.STAGING)

        assertEquals(
            "https://api.example.com/wp-json/academy-mobile/v1/",
            staging.baseUrl,
        )
        assertTrue(staging.isEnabled)
    }

    @Test
    fun keepsProductionDisabledWithoutAUrl() {
        val production = AcademyApiEnvironments.configurationFor(AcademyEnvironment.PRODUCTION)

        assertFalse(production.isEnabled)
        assertNull(production.baseUrl)
        assertFailsWith<AcademyEnvironmentDisabledException> {
            production.requireBaseUrl()
        }
    }
}
