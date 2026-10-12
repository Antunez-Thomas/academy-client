package com.example.academyapp.network

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlin.test.Test
import kotlin.test.assertEquals

class AcademyJsonTest {
    @Serializable
    private data class ContractProbe(val id: Long, val title: String)

    @Test
    fun ignoresUnknownServerFields() {
        val decoded = AcademyJson.decodeFromString<ContractProbe>(
            """{"id":58356,"title":"Course","futureField":true}""",
        )

        assertEquals(58_356L, decoded.id)
        assertEquals("Course", decoded.title)
    }

    @Test
    fun createsPlatformClientWithSharedConfiguration() {
        createAcademyHttpClient(
            AcademyHttpClientConfig(enableDevelopmentLogging = true),
        ).close()
    }
}
