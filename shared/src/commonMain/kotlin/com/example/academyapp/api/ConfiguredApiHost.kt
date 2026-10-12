package com.example.academyapp.api

import io.ktor.http.Url

/** Extracts only the hostname from trusted app configuration; paths, schemes, and ports are discarded. */
internal fun configuredHostFromApiBaseUrl(baseUrl: String): String? {
    if (baseUrl.length > 2048 || baseUrl.any(Char::isWhitespace)) return null
    val authority = Regex("^https?://([^/?#]+)(?:[/?#].*)?$", RegexOption.IGNORE_CASE)
        .matchEntire(baseUrl)
        ?.groupValues
        ?.getOrNull(1)
        ?: return null
    if ('@' in authority || authority.count { it == ':' } > 1) return null

    val parsed = runCatching { Url(baseUrl) }.getOrNull() ?: return null
    if (parsed.protocol.name.lowercase() !in setOf("http", "https")) return null
    return parsed.host.lowercase().takeIf(String::isNotBlank)
}
