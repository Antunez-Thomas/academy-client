package com.example.academyapp.course

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.ktor.http.URLProtocol
import io.ktor.http.Url

@Composable
internal expect fun BunnyEmbedPlayer(
    playbackUrl: String,
    modifier: Modifier,
)

internal fun isExpectedBunnyEmbedUrl(
    value: String,
    libraryId: Long,
    videoId: String,
): Boolean {
    val url = runCatching { Url(value) }.getOrNull() ?: return false
    return url.protocol == URLProtocol.HTTPS &&
        url.host.equals(BUNNY_EMBED_HOST, ignoreCase = true) &&
        url.port == URLProtocol.HTTPS.defaultPort &&
        url.segments == listOf("embed", libraryId.toString(), videoId) &&
        url.fragment.isEmpty()
}

internal fun isAllowedBunnyEmbedNavigation(candidate: String, authorizedUrl: String): Boolean {
    val authorized = runCatching { Url(authorizedUrl) }.getOrNull() ?: return false
    val requested = runCatching { Url(candidate) }.getOrNull() ?: return false
    val authorizedLocationIsValid = authorized.protocol == URLProtocol.HTTPS &&
        authorized.host.equals(BUNNY_EMBED_HOST, ignoreCase = true) &&
        authorized.port == URLProtocol.HTTPS.defaultPort &&
        authorized.segments.size == 3 &&
        authorized.segments.firstOrNull() == "embed" &&
        authorized.fragment.isEmpty()
    return authorizedLocationIsValid &&
        requested.protocol == URLProtocol.HTTPS &&
        requested.host.equals(BUNNY_EMBED_HOST, ignoreCase = true) &&
        requested.port == URLProtocol.HTTPS.defaultPort &&
        requested.segments == authorized.segments &&
        requested.fragment.isEmpty()
}

private const val BUNNY_EMBED_HOST = "iframe.mediadelivery.net"
