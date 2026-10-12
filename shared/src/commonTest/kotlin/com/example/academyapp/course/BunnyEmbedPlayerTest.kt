package com.example.academyapp.course

import com.example.academyapp.api.model.Lesson
import com.example.academyapp.api.model.LessonType
import com.example.academyapp.api.model.PlaybackResponse
import com.example.academyapp.api.model.Video
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BunnyEmbedPlayerTest {
    @Test
    fun acceptsOnlyExpectedHttpsEmbedLocation() {
        assertTrue(isExpectedBunnyEmbedUrl(VALID_URL, LIBRARY_ID, VIDEO_ID))
        assertFalse(isExpectedBunnyEmbedUrl("http://iframe.mediadelivery.net/embed/123/$VIDEO_ID", LIBRARY_ID, VIDEO_ID))
        assertFalse(isExpectedBunnyEmbedUrl("https://iframe.mediadelivery.net.example/embed/123/$VIDEO_ID", LIBRARY_ID, VIDEO_ID))
        assertFalse(isExpectedBunnyEmbedUrl("https://iframe.mediadelivery.net/embed/124/$VIDEO_ID", LIBRARY_ID, VIDEO_ID))
        assertFalse(isExpectedBunnyEmbedUrl("https://iframe.mediadelivery.net/embed/123/other", LIBRARY_ID, VIDEO_ID))
    }

    @Test
    fun topLevelNavigationStaysOnAuthorizedEmbedPath() {
        assertTrue(isAllowedBunnyEmbedNavigation("$VALID_URL?autoplay=true", VALID_URL))
        assertFalse(isAllowedBunnyEmbedNavigation("https://example.test/", VALID_URL))
        assertFalse(isAllowedBunnyEmbedNavigation("https://iframe.mediadelivery.net/", VALID_URL))
    }

    @Test
    fun playbackAuthorizationRequiresMatchingNonExpiredTokenAndExpiry() {
        assertEquals(SIGNED_URL, PLAYBACK.validatedEmbedUrlFor(VIDEO_LESSON, NOW))

        val invalidResponses = listOf(
            PLAYBACK.copy(token = ""),
            PLAYBACK.copy(playbackUrl = "$EMBED_URL?expires=$EXPIRY"),
            PLAYBACK.copy(playbackUrl = "$EMBED_URL?token=&expires=$EXPIRY"),
            PLAYBACK.copy(playbackUrl = "$EMBED_URL?token=other&expires=$EXPIRY"),
            PLAYBACK.copy(playbackUrl = "$EMBED_URL?token=$TOKEN"),
            PLAYBACK.copy(playbackUrl = "$EMBED_URL?token=$TOKEN&expires="),
            PLAYBACK.copy(playbackUrl = "$EMBED_URL?token=$TOKEN&expires=${EXPIRY + 1}"),
            PLAYBACK.copy(playbackUrl = "$SIGNED_URL&token=$TOKEN"),
            PLAYBACK.copy(playbackUrl = "$SIGNED_URL&expires=$EXPIRY"),
            PLAYBACK.copy(
                expires = NOW,
                playbackUrl = "$EMBED_URL?token=$TOKEN&expires=$NOW",
            ),
        )

        invalidResponses.forEach { response ->
            assertNull(response.validatedEmbedUrlFor(VIDEO_LESSON, NOW))
        }
    }

    private companion object {
        const val NOW = 1_900_000_000L
        const val EXPIRY = 2_000_000_000L
        const val TOKEN = "synthetic-playback-token"
        const val LIBRARY_ID = 123L
        const val VIDEO_ID = "018f47ac-38d7-7e45-a5d9-82018f4b87fb"
        const val VALID_URL = "https://iframe.mediadelivery.net/embed/123/$VIDEO_ID"
        const val EMBED_URL = VALID_URL
        const val SIGNED_URL = "$EMBED_URL?token=$TOKEN&expires=$EXPIRY"

        val VIDEO_LESSON = Lesson(
            id = 20,
            courseId = 10,
            parentLessonId = null,
            type = LessonType.TOPIC,
            title = "Video topic",
            position = 1,
            durationSeconds = null,
            completed = false,
            positionSeconds = 0,
            video = Video(videoId = VIDEO_ID, libraryId = LIBRARY_ID),
        )
        val PLAYBACK = PlaybackResponse(
            lessonId = VIDEO_LESSON.id,
            videoId = VIDEO_ID,
            libraryId = LIBRARY_ID,
            token = TOKEN,
            expires = EXPIRY,
            tokenScheme = "embed_sha256_hex",
            playbackUrl = SIGNED_URL,
            expiresAt = "2033-05-18T03:33:20Z",
        )
    }
}
