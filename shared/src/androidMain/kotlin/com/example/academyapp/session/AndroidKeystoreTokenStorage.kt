package com.example.academyapp.session

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.example.academyapp.network.AcademyJson
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString

class AndroidKeystoreTokenStorage(
    context: Context,
    private val keyAlias: String = "academy.session.tokens.v1",
) : SessionTokenStorage {
    private val preferences = context.applicationContext.getSharedPreferences(
        "academy_secure_session",
        Context.MODE_PRIVATE,
    )

    override suspend fun load(): SessionTokens? = withContext(Dispatchers.IO) {
        val encrypted = preferences.getString(CIPHERTEXT_KEY, null) ?: return@withContext null
        val iv = preferences.getString(IV_KEY, null) ?: return@withContext null
        try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(
                Cipher.DECRYPT_MODE,
                secretKey(),
                GCMParameterSpec(TAG_LENGTH_BITS, Base64.decode(iv, Base64.NO_WRAP)),
            )
            val plaintext = cipher.doFinal(Base64.decode(encrypted, Base64.NO_WRAP)).decodeToString()
            AcademyJson.decodeFromString<SessionTokens>(plaintext)
        } catch (cause: Throwable) {
            preferences.edit().clear().apply()
            throw cause
        }
    }

    override suspend fun save(tokens: SessionTokens) = withContext(Dispatchers.IO) {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        val encrypted = cipher.doFinal(AcademyJson.encodeToString(tokens).encodeToByteArray())
        check(
            preferences.edit()
                .putString(CIPHERTEXT_KEY, Base64.encodeToString(encrypted, Base64.NO_WRAP))
                .putString(IV_KEY, Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
                .commit(),
        ) { "Unable to persist the encrypted session." }
    }

    override suspend fun clear() = withContext(Dispatchers.IO) {
        check(preferences.edit().clear().commit()) { "Unable to clear the encrypted session." }
    }

    private fun secretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
        (keyStore.getKey(keyAlias, null) as? SecretKey)?.let { return it }

        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER).run {
            init(
                KeyGenParameterSpec.Builder(
                    keyAlias,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build(),
            )
            generateKey()
        }
    }

    private companion object {
        const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val TAG_LENGTH_BITS = 128
        const val CIPHERTEXT_KEY = "ciphertext"
        const val IV_KEY = "iv"
    }
}
