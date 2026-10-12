package com.example.academyapp.course

import com.example.academyapp.api.model.QuizWebLaunch
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AcademyQuizWebViewTest {
    private val launch = QuizWebLaunch(
        userId = 42,
        courseId = 10,
        quizId = 30,
        ticket = "d".repeat(64),
        redeemUrl = "https://academy.example.test/wp-json/academy-mobile/v1/quiz/web-redeem",
        logoutUrl = "https://academy.example.test/wp-json/academy-mobile/v1/quiz/web-logout",
        quizUrl = "https://academy.example.test/quizzes/knowledge-check/?course=10",
        expiresAt = "2026-10-02T13:05:00Z",
    )

    @Test
    fun allowsOnlyTheRedeemAndExactFirstPartyQuizPaths() {
        assertTrue(isAllowedQuizWebNavigation(launch.redeemUrl, launch))
        assertTrue(isAllowedQuizWebNavigation("https://academy.example.test/quizzes/knowledge-check/?attempt=2", launch))
        assertFalse(isAllowedQuizWebNavigation("https://academy.example.test/wp-admin/", launch))
        assertFalse(isAllowedQuizWebNavigation("https://example.org/quizzes/knowledge-check/", launch))
        assertFalse(isAllowedQuizWebNavigation("http://academy.example.test/quizzes/knowledge-check/", launch))
    }

    @Test
    fun permitsCleartextOnlyForTheExplicitLocalEmulatorOrigins() {
        val local = QuizWebLaunch(
            userId = launch.userId,
            courseId = launch.courseId,
            quizId = launch.quizId,
            ticket = launch.ticket,
            redeemUrl = "http://10.0.2.2:8080/wp-json/academy-mobile/v1/quiz/web-redeem",
            logoutUrl = "http://10.0.2.2:8080/wp-json/academy-mobile/v1/quiz/web-logout",
            quizUrl = "http://10.0.2.2:8080/quizzes/knowledge-check/",
            expiresAt = launch.expiresAt,
        )

        assertTrue(isAllowedQuizWebNavigation(local.quizUrl, local))
        assertFalse(
            isAllowedQuizWebNavigation(
                "http://192.168.1.8:8080/quizzes/knowledge-check/",
                local,
            ),
        )
    }

    @Test
    fun cleanupCompletionRequiresTheExactLogoutEndpoint() {
        assertTrue(isSameQuizWebEndpoint(launch.logoutUrl, launch.logoutUrl))
        assertTrue(isSameQuizWebEndpoint("${launch.logoutUrl}?result=ok", launch.logoutUrl))
        assertFalse(isSameQuizWebEndpoint("about:blank", launch.logoutUrl))
        assertFalse(isSameQuizWebEndpoint(launch.quizUrl, launch.logoutUrl))
        assertFalse(
            isSameQuizWebEndpoint(
                "https://example.org/wp-json/academy-mobile/v1/quiz/web-logout",
                launch.logoutUrl,
            ),
        )
    }
}
