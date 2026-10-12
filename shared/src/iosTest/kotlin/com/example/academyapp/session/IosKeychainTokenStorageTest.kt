package com.example.academyapp.session

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class IosKeychainTokenStorageTest {
    private val storage = IosKeychainTokenStorage(
        service = "online.academy.app.session.tests",
        account = "mobile-api-tokens-runtime",
    )

    @Test
    fun savesLoadsAndClearsTokenPair() = runTest {
        val expected = SessionTokens(
            accessToken = "ios-test-access",
            refreshToken = "ios-test-refresh",
            accessTokenExpiresAt = "2026-09-23T12:00:00Z",
        )

        storage.clear()
        try {
            assertNull(storage.load())
            storage.save(expected)
            assertEquals(expected, storage.load())
            storage.clear()
            assertNull(storage.load())
        } finally {
            storage.clear()
        }
    }
}
