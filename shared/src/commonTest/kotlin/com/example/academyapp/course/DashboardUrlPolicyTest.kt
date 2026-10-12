package com.example.academyapp.course

import com.example.academyapp.api.configuredHostFromApiBaseUrl
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DashboardUrlPolicyTest {
    @Test
    fun acceptsOnlyHttpsHostsInTheDashboardAllowlist() {
        assertEquals("https://us02web.zoom.us/j/123", safeZoomJoinUrl("https://us02web.zoom.us/j/123"))
        assertEquals("https://academy.online/ayuda", safeSupportActionUrl("https://academy.online/ayuda"))
        assertEquals("https://wa.me/5491112345678", safeSupportActionUrl("https://wa.me/5491112345678"))
        assertEquals("https://api.whatsapp.com/send?phone=5491112345678", safeSupportActionUrl("https://api.whatsapp.com/send?phone=5491112345678"))
        assertEquals("https://events.example.test/join/123", safeEventUrl("https://events.example.test/join/123"))
        assertEquals("https://events.example.test:443/join/123", safeEventUrl("https://events.example.test:443/join/123"))
        assertEquals("https://events.example.test/join/123", safeEventJoinUrl(true, "https://events.example.test/join/123"))
        assertNull(safeEventJoinUrl(false, "https://events.example.test/join/123"))
        assertNull(safeEventJoinUrl(true, null))

        listOf(
            "http://academy.online/ayuda",
            "https://academy.online.evil.test/ayuda",
            "https://academy.online@evil.test/ayuda",
            "https://evil.test/ayuda",
            "https://wa.me:444/5491112345678",
            "https://academy.online:/ayuda",
        ).forEach { assertNull(safeSupportActionUrl(it), it) }
        listOf(
            "https://zoom.us.evil.test/j/123",
            "https://evil.test/j/123",
            "http://zoom.us/j/123",
            "https://zoom.us:444/j/123",
        ).forEach { assertNull(safeZoomJoinUrl(it), it) }
        listOf(
            "http://events.example.test/join/123",
            "https://events.example.test:8443/join/123",
            "https://user:secret@events.example.test/join/123",
            "https://events.example.test/path with space",
        ).forEach { assertNull(safeEventUrl(it), it) }
    }

    @Test
    fun acceptsSupportLinksOnTheConfiguredApiHostForLocalAndStaging() {
        val localHost = configuredHostFromApiBaseUrl("http://10.0.2.2:8080/wp-json/academy-mobile/v1/")
        val stagingHost = configuredHostFromApiBaseUrl("https://wordpress-stage.example.test/wp-json/academy-mobile/v1/")

        assertEquals("10.0.2.2", localHost)
        assertEquals("wordpress-stage.example.test", stagingHost)
        assertEquals("https://10.0.2.2/ayuda", safeSupportActionUrl("https://10.0.2.2/ayuda", localHost))
        assertEquals(
            "https://wordpress-stage.example.test/ayuda",
            safeSupportActionUrl("https://wordpress-stage.example.test/ayuda", stagingHost),
        )
    }

    @Test
    fun acceptsProductionHostAndRejectsUnrelatedHostWithConfiguredApiHost() {
        val stagingHost = configuredHostFromApiBaseUrl("https://wordpress-stage.example.test/wp-json/academy-mobile/v1/")

        assertEquals("https://academy.online/ayuda", safeSupportActionUrl("https://academy.online/ayuda", stagingHost))
        assertNull(safeSupportActionUrl("https://unrelated.example.test/ayuda", stagingHost))
        assertNull(safeSupportActionUrl("https://wordpress-stage.example.test:8443/ayuda", stagingHost))
        assertNull(safeSupportActionUrl("https://wordpress-stage.example.test@unrelated.example.test/ayuda", stagingHost))
    }

    @Test
    fun configuredHostExtractionReturnsOnlyValidatedHttpHost() {
        assertEquals("academy.online", configuredHostFromApiBaseUrl("https://academy.online/wp-json/academy-mobile/v1/"))
        assertNull(configuredHostFromApiBaseUrl("https://user:secret@academy.online/wp-json/"))
        assertNull(configuredHostFromApiBaseUrl("ftp://academy.online/api"))
        assertNull(configuredHostFromApiBaseUrl("not a URL"))
    }
}
