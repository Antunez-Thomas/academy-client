package com.example.academyapp

import com.example.academyapp.api.AcademyEnvironment
import com.example.academyapp.api.AcademyApiEnvironments
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AndroidAcademyEnvironmentTest {
    @Test
    fun debugBuildCanOptInToStaging() {
        val selectedEnvironment = androidAcademyEnvironment(isDebugBuild = true, useStaging = true)
        val configuration = AcademyApiEnvironments.configurationFor(selectedEnvironment)

        assertEquals(AcademyEnvironment.STAGING, selectedEnvironment)
        assertEquals(
            "https://api.example.com/wp-json/academy-mobile/v1/",
            configuration.baseUrl,
        )
        assertTrue(configuration.isEnabled)
    }

    @Test
    fun localRemainsDefaultAndReleaseCannotSelectStaging() {
        assertEquals(AcademyEnvironment.LOCAL, androidAcademyEnvironment(isDebugBuild = true, useStaging = false))
        assertEquals(AcademyEnvironment.LOCAL, androidAcademyEnvironment(isDebugBuild = false, useStaging = true))
    }
}
