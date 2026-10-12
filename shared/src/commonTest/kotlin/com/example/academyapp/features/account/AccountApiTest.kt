package com.example.academyapp.features.account

import com.example.academyapp.api.*
import com.example.academyapp.api.model.*
import com.example.academyapp.features.account.data.AuthenticatedAccountRepository
import com.example.academyapp.network.*
import com.example.academyapp.session.*
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlin.test.*

class AccountApiTest {
    private val user = User(1, "learner@example.test", "Ana", "ana", "Ana", "García", "123456789")
    private val configuration = AcademyApiEnvironments.configurationFor(AcademyEnvironment.LOCAL)
    private val headers = headersOf(HttpHeaders.ContentType, "application/json")
    @Test fun additiveUserSupportsLegacyPayload() {
        val legacy = AcademyJson.decodeFromString<User>("""{"id":1,"email":"learner@example.test","displayName":"Ana"}""")
        assertEquals("", legacy.firstName); assertNull(legacy.avatarUrl)
        assertEquals(user, AcademyJson.decodeFromString<User>(AcademyJson.encodeToString(user)))
    }
    @Test fun currentAccountRequestsUseBearerAndNoIdentifiers() = runTest {
        val token = kotlin.random.Random.nextBytes(32).joinToString("") { (it.toInt() and 255).toString(16).padStart(2, '0') }
        var count = 0
        val client = HttpClient(MockEngine { request ->
            count++
            assertEquals("Bearer $token", request.headers[HttpHeaders.Authorization])
            assertFalse(request.url.parameters.names().any { it.contains("id", true) })
            when (count) {
                1 -> { assertEquals(HttpMethod.Post, request.method); assertTrue(request.url.encodedPath.endsWith("/me/profile")); assertEquals(ProfileUpdateRequest("Ana", "García", "123456789"), AcademyJson.decodeFromString(request.body.toByteArray().decodeToString())) }
                2 -> { assertEquals(HttpMethod.Post, request.method); assertTrue(request.body.contentType.toString().startsWith("multipart/form-data")); val body = request.body.toByteArray().decodeToString(); assertTrue(body.contains("name=photo") || body.contains("name=\"photo\"")); assertFalse(body.contains("userId")) }
                3 -> { assertEquals(HttpMethod.Delete, request.method); assertTrue(request.url.encodedPath.endsWith("/me/avatar")) }
            }
            respond(AcademyJson.encodeToString(user), headers = headers)
        }) { configureAcademyHttpClient() }
        try {
            val api = KtorAcademyApi(configuration, client, AccessTokenProvider { token })
            assertIs<ApiResult.Success<User>>(api.updateProfile(ProfileUpdateRequest("Ana", "García", "123456789")))
            assertIs<ApiResult.Success<User>>(api.uploadAvatar(byteArrayOf(1,2), "image/png"))
            assertIs<ApiResult.Success<User>>(api.removeAvatar())
            assertEquals(3, count)
        } finally { client.close() }
    }
    @Test fun noTokenAndExternalAvatarNeverSend() = runTest {
        val client = HttpClient(MockEngine { error("Must not send") }) { configureAcademyHttpClient() }
        try {
            val api = KtorAcademyApi(configuration, client)
            assertIs<ApiResult.Failure>(api.updateProfile(ProfileUpdateRequest("Ana", "García", "123456789")))
            assertIs<ApiResult.Failure>(api.uploadAvatar(byteArrayOf(1), "image/jpeg"))
            assertIs<ApiResult.Failure>(api.removeAvatar())
            assertNull(api.getAvatarImage("https://untrusted.example.test/photo.jpg"))
        } finally { client.close() }
    }
    @Test fun avatarReadsAreBoundedDoNotFollowRedirectsAndKeepApiClientAlive() = runTest {
        var calls = 0
        val base = configuration.requireBaseUrl()
        val client = HttpClient(MockEngine { request ->
            calls++
            assertNull(request.headers[HttpHeaders.Authorization])
            when (request.url.encodedPath.substringAfterLast('/')) {
                "redirect.jpg" -> respond("", HttpStatusCode.Found, headersOf(HttpHeaders.Location, "https://untrusted.example.test/image.jpg"))
                "large.jpg" -> respond(ByteArray(5 * 1024 * 1024 + 1), headers = headersOf(HttpHeaders.ContentType, "image/jpeg"))
                "photo.jpg" -> respond(byteArrayOf(1, 2, 3), headers = headersOf(HttpHeaders.ContentType, "image/jpeg"))
                else -> respond("{}", headers = headers)
            }
        }) { configureAcademyHttpClient() }
        try {
            val api = KtorAcademyApi(configuration, client)
            assertNull(api.getAvatarImage(base + "redirect.jpg")); assertEquals(1, calls)
            assertNull(api.getAvatarImage(base + "large.jpg"))
            assertContentEquals(byteArrayOf(1, 2, 3), api.getAvatarImage(base + "photo.jpg"))
            client.get(base) // Closing a configured image client must not close the API engine.
            assertEquals(4, calls)
        } finally { client.close() }
    }
    @Test fun repositoryRefreshesOnceAndClearsAfterSecondUnauthorized() = runTest {
        val old = kotlin.random.Random.nextBytes(32).joinToString("") { (it.toInt() and 255).toString(16).padStart(2, '0') }
        val replacement = old.reversed()
        var refreshes = 0; var writes = 0; var deny = false
        val client = HttpClient(MockEngine { request ->
            when {
                request.url.encodedPath.endsWith("/auth/refresh") -> { refreshes++; respond(AcademyJson.encodeToString(LoginResponse(user, replacement, replacement, "2099-01-01T00:00:00Z")), headers = headers) }
                request.url.encodedPath.endsWith("/me/profile") -> {
                    writes++
                    if (deny || request.headers[HttpHeaders.Authorization] == "Bearer $old") respond("""{"code":"unauthorized","message":"","data":{"status":401}}""", HttpStatusCode.Unauthorized, headers)
                    else respond(AcademyJson.encodeToString(user), headers = headers)
                }
                else -> respond(AcademyJson.encodeToString(user), headers = headers)
            }
        }) { configureAcademyHttpClient() }
        try {
            val provider = MutableAccessTokenProvider()
            val api = KtorAcademyApi(configuration, client, provider)
            val storage = object : SessionTokenStorage {
                var tokens: SessionTokens? = SessionTokens(old, old, "2099-01-01T00:00:00Z")
                override suspend fun load() = tokens
                override suspend fun save(tokens: SessionTokens) { this.tokens = tokens }
                override suspend fun clear() { tokens = null }
            }
            val session = AcademySessionManager(api, storage, provider); session.initialize()
            val repo = AuthenticatedAccountRepository(api, session)
            assertIs<ApiResult.Success<User>>(repo.save(ProfileUpdateRequest("Ana", "García", "123456789")))
            assertEquals(1, refreshes); assertEquals(2, writes)
            deny = true; assertIs<ApiResult.Failure>(repo.save(ProfileUpdateRequest("Ana", "García", "123456789")))
            assertEquals(SessionState.Expired, session.state.value); assertNull(storage.tokens)
        } finally { client.close() }
    }
}
