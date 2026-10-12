package com.example.academyapp.network

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.android.Android

internal actual fun createPlatformHttpClient(
    config: HttpClientConfig<*>.() -> Unit,
): HttpClient = HttpClient(Android) {
    config(this)
}
