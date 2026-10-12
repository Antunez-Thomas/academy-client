package com.example.academyapp.network

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.accept
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

data class AcademyHttpClientConfig(
    val connectTimeoutMillis: Long = 15_000,
    val requestTimeoutMillis: Long = 30_000,
    val socketTimeoutMillis: Long = 30_000,
    val enableDevelopmentLogging: Boolean = false,
)

val AcademyJson = Json {
    ignoreUnknownKeys = true
    isLenient = false
    encodeDefaults = true
    explicitNulls = true
}

fun createAcademyHttpClient(
    settings: AcademyHttpClientConfig = AcademyHttpClientConfig(),
): HttpClient = createPlatformHttpClient {
    configureAcademyHttpClient(settings)
}

internal fun HttpClientConfig<*>.configureAcademyHttpClient(
    settings: AcademyHttpClientConfig = AcademyHttpClientConfig(),
) {
    expectSuccess = false

    install(ContentNegotiation) {
        json(AcademyJson)
    }
    install(HttpTimeout) {
        connectTimeoutMillis = settings.connectTimeoutMillis
        requestTimeoutMillis = settings.requestTimeoutMillis
        socketTimeoutMillis = settings.socketTimeoutMillis
    }
    if (settings.enableDevelopmentLogging) {
        install(Logging) {
            level = LogLevel.HEADERS
            logger = object : Logger {
                override fun log(message: String) {
                    println("AcademyHttp: $message")
                }
            }
            sanitizeHeader { header -> header.equals(HttpHeaders.Authorization, ignoreCase = true) }
        }
    }
    defaultRequest {
        accept(ContentType.Application.Json)
    }
}

internal expect fun createPlatformHttpClient(
    config: HttpClientConfig<*>.() -> Unit,
): HttpClient
