package com.example.academyapp.session

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.security.KeyStore
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertNull

@RunWith(AndroidJUnit4::class)
class AndroidKeystoreTokenStorageTest {
    private val keyAlias = "academy.session.instrumentation.test"
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val storage = AndroidKeystoreTokenStorage(context, keyAlias)

    @After
    fun cleanUp() {
        runBlocking {
            storage.clear()
            KeyStore.getInstance("AndroidKeyStore").apply {
                load(null)
                if (containsAlias(keyAlias)) deleteEntry(keyAlias)
            }
        }
    }

    @Test
    fun savesLoadsAndClearsEncryptedTokenPair() = runBlocking {
        val expected = SessionTokens(
            accessToken = "instrumentation-access",
            refreshToken = "instrumentation-refresh",
            accessTokenExpiresAt = "2026-09-23T12:00:00Z",
        )

        storage.clear()
        assertNull(storage.load())
        storage.save(expected)
        assertEquals(expected, storage.load())
        storage.clear()
        assertNull(storage.load())
    }
}
