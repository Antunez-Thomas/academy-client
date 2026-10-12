package com.example.academyapp.course

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.academyapp.api.model.QuizWebLaunch

interface QuizWebSessionController {
    fun activate(logoutUrl: String)
    suspend fun revokeAndClear(): QuizWebSessionCleanupResult
}

enum class QuizWebSessionCleanupResult {
    Cleared,
    NoActiveSession,
    Failed,
}

internal object NoOpQuizWebSessionController : QuizWebSessionController {
    override fun activate(logoutUrl: String) = Unit
    override suspend fun revokeAndClear() = QuizWebSessionCleanupResult.NoActiveSession
}

@Composable
internal expect fun AcademyQuizWebView(
    launch: QuizWebLaunch,
    sessionController: QuizWebSessionController,
    modifier: Modifier,
    onPageReady: () -> Unit,
    onPageError: () -> Unit,
)

internal fun isAllowedQuizWebNavigation(candidate: String, launch: QuizWebLaunch): Boolean {
    val requested = runCatching { io.ktor.http.Url(candidate) }.getOrNull() ?: return false
    val redeem = runCatching { io.ktor.http.Url(launch.redeemUrl) }.getOrNull() ?: return false
    val quiz = runCatching { io.ktor.http.Url(launch.quizUrl) }.getOrNull() ?: return false
    val approvedScheme = quiz.protocol.name == "https" ||
        (quiz.protocol.name == "http" && quiz.host in setOf("10.0.2.2", "127.0.0.1", "localhost"))
    if (!approvedScheme || requested.protocol != quiz.protocol || requested.host != quiz.host || requested.port != quiz.port) {
        return false
    }
    return requested.encodedPath == redeem.encodedPath || requested.encodedPath == quiz.encodedPath
}

internal fun isSameQuizWebEndpoint(candidate: String, approved: String): Boolean {
    val candidateUrl = runCatching { io.ktor.http.Url(candidate) }.getOrNull() ?: return false
    val approvedUrl = runCatching { io.ktor.http.Url(approved) }.getOrNull() ?: return false
    return candidateUrl.protocol == approvedUrl.protocol && candidateUrl.host == approvedUrl.host &&
        candidateUrl.port == approvedUrl.port && candidateUrl.encodedPath == approvedUrl.encodedPath
}
