package com.example.academyapp.api

import com.example.academyapp.api.model.ApiError
import com.example.academyapp.api.model.CourseContent
import com.example.academyapp.api.model.DashboardResponse
import com.example.academyapp.api.model.DashboardEventStatus
import com.example.academyapp.api.model.Entitlement
import com.example.academyapp.api.model.Lesson
import com.example.academyapp.api.model.LessonType
import com.example.academyapp.api.model.LoginResponse
import com.example.academyapp.api.model.PlaybackResponse
import com.example.academyapp.api.model.ProgressResponse
import com.example.academyapp.network.AcademyJson
import kotlinx.serialization.decodeFromString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class AcademyApiModelsTest {
    @Test
    fun decodesFrozenContractFixtures() {
        val login = AcademyJson.decodeFromString<LoginResponse>(TestFixtures.LOGIN_RESPONSE)
        val content = AcademyJson.decodeFromString<CourseContent>(TestFixtures.COURSE_CONTENT)
        val lesson = AcademyJson.decodeFromString<Lesson>(TestFixtures.LESSON)
        val progress = AcademyJson.decodeFromString<ProgressResponse>(TestFixtures.PROGRESS_RESPONSE)
        val playback = AcademyJson.decodeFromString<PlaybackResponse>(TestFixtures.PLAYBACK_RESPONSE)
        val entitlements = AcademyJson.decodeFromString<List<Entitlement>>(TestFixtures.ENTITLEMENTS)
        val error = AcademyJson.decodeFromString<ApiError>(TestFixtures.API_ERROR)
        val dashboard = AcademyJson.decodeFromString<DashboardResponse>(TestFixtures.DASHBOARD)

        assertEquals(42L, login.user.id)
        assertEquals(1001L, content.course.id)
        assertEquals(LessonType.TOPIC, lesson.type)
        assertEquals(2001L, lesson.parentLessonId)
        assertEquals(120, progress.positionSeconds)
        assertEquals("embed_sha256_hex", playback.tokenScheme)
        assertNotNull(lesson.video)
        assertEquals(12345L, lesson.video.libraryId)
        assertEquals("active", entitlements.single().status)
        assertEquals(401, error.data.status)
        assertEquals(1001L, dashboard.courses.single().id)
        assertEquals("ready", dashboard.upcomingLiveClasses.status.name.lowercase())
        assertEquals(DashboardEventStatus.STARTING, dashboard.events.items.single().status)
        assertEquals("Evento &amp; especial", dashboard.events.items.single().title)
        assertEquals("https://events.example.test/join/123", dashboard.events.items.single().joinUrl)
        assertEquals("https://wa.me/5491112345678", dashboard.supportActions.items.single().url)
    }
}
