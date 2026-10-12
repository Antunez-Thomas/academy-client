package com.example.academyapp.session

import kotlinx.serialization.Serializable

@Serializable
data class SessionTokens(
    val accessToken: String,
    val refreshToken: String,
    val accessTokenExpiresAt: String,
)

interface SessionTokenStorage {
    suspend fun load(): SessionTokens?
    suspend fun save(tokens: SessionTokens)
    suspend fun clear()
}
