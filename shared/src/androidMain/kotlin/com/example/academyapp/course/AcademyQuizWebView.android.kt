package com.example.academyapp.course

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.webkit.WebStorageCompat
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.example.academyapp.api.model.QuizWebLaunch
import io.ktor.http.Url
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import kotlin.coroutines.resume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

class AndroidQuizWebSessionController(
    context: Context,
    apiBaseUrl: String,
    preferencesName: String = QUIZ_WEB_SESSION_PREFERENCES,
) : QuizWebSessionController {
    private val appContext = context.applicationContext
    private val logoutUrlStore = QuizWebLogoutUrlStore(appContext, preferencesName)
    private val expectedLogoutUrl = expectedQuizWebLogoutUrl(apiBaseUrl)
    private var activeLogoutUrl: String? = logoutUrlStore.read().also { savedUrl ->
        if (savedUrl != null && savedUrl != expectedLogoutUrl) logoutUrlStore.clear()
    }.takeIf { it != null && it == expectedLogoutUrl }

    internal fun profileNameForWebView(): String = QUIZ_WEB_PROFILE

    override fun activate(logoutUrl: String) {
        val parsed = runCatching { Url(logoutUrl) }.getOrNull() ?: return
        if (logoutUrl == expectedLogoutUrl && parsed.host.isNotBlank()) {
            activeLogoutUrl = logoutUrl
            logoutUrlStore.write(logoutUrl)
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    override suspend fun revokeAndClear(): QuizWebSessionCleanupResult =
        withContext(Dispatchers.Main.immediate) {
        val logoutUrl = activeLogoutUrl ?: return@withContext QuizWebSessionCleanupResult.NoActiveSession
        if (!supportsIsolatedQuizProfile()) return@withContext QuizWebSessionCleanupResult.Failed
        val result = withTimeoutOrNull(5_000) {
            suspendCancellableCoroutine { continuation ->
                val webView = WebView(appContext)
                WebViewCompat.setProfile(webView, profileNameForWebView())
                var completed = false
                fun finish(result: QuizWebSessionCleanupResult) {
                    if (completed) return
                    completed = true
                    webView.stopLoading()
                    webView.destroy()
                    if (continuation.isActive) continuation.resume(result)
                }
                fun clearIsolatedProfile() {
                    clearQuizBrowsingData(webView) {
                        finish(QuizWebSessionCleanupResult.Cleared)
                    }
                }
                continuation.invokeOnCancellation { webView.destroy() }
                webView.settings.javaScriptEnabled = false
                webView.settings.allowFileAccess = false
                webView.settings.allowContentAccess = false
                webView.settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                webView.webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView, url: String) {
                        if (isSameQuizWebEndpoint(url, logoutUrl)) clearIsolatedProfile()
                    }
                    override fun onReceivedError(
                        view: WebView,
                        request: WebResourceRequest,
                        error: WebResourceError,
                    ) {
                        if (request.isForMainFrame) finish(QuizWebSessionCleanupResult.Failed)
                    }
                    override fun onReceivedHttpError(
                        view: WebView,
                        request: WebResourceRequest,
                        errorResponse: WebResourceResponse,
                    ) {
                        if (request.isForMainFrame && errorResponse.statusCode >= 400) {
                            finish(QuizWebSessionCleanupResult.Failed)
                        }
                    }
                }
                webView.postUrl(logoutUrl, ByteArray(0))
            }
        } ?: QuizWebSessionCleanupResult.Failed
        if (result == QuizWebSessionCleanupResult.Cleared) {
            activeLogoutUrl = null
            logoutUrlStore.clear()
        }
        result
    }
}

internal class QuizWebLogoutUrlStore(
    context: Context,
    preferencesName: String = QUIZ_WEB_SESSION_PREFERENCES,
) {
    private val preferences = context.applicationContext.getSharedPreferences(
        preferencesName,
        Context.MODE_PRIVATE,
    )

    fun read(): String? = preferences.getString(QUIZ_WEB_LOGOUT_URL_KEY, null)

    fun write(logoutUrl: String) {
        preferences.edit().putString(QUIZ_WEB_LOGOUT_URL_KEY, logoutUrl).apply()
    }

    fun clear() {
        preferences.edit().remove(QUIZ_WEB_LOGOUT_URL_KEY).apply()
    }
}

private fun expectedQuizWebLogoutUrl(apiBaseUrl: String): String? = runCatching {
    val api = Url(apiBaseUrl)
    if (api.protocol.name !in setOf("http", "https") || api.host.isBlank()) return null
    buildString {
        append(api.protocol.name)
        append("://")
        append(api.host)
        if (api.port != api.protocol.defaultPort) append(":${api.port}")
        append(api.encodedPath.trimEnd('/'))
        append("/quiz/web-logout")
    }
}.getOrNull()

private const val QUIZ_WEB_PROFILE = "academy_quiz_web"
private const val QUIZ_WEB_SESSION_PREFERENCES = "academy_quiz_web_session"
private const val QUIZ_WEB_LOGOUT_URL_KEY = "logout_url"

internal fun clearQuizBrowsingData(webView: WebView, onComplete: () -> Unit) {
    val profile = WebViewCompat.getProfile(webView)
    webView.clearFormData()
    webView.clearHistory()
    profile.geolocationPermissions.clearAll()
    WebStorageCompat.deleteBrowsingData(profile.webStorage) {
        onComplete()
    }
}

private fun supportsIsolatedQuizProfile(): Boolean =
    WebViewFeature.isFeatureSupported(WebViewFeature.MULTI_PROFILE) &&
        WebViewFeature.isFeatureSupported(WebViewFeature.DELETE_BROWSING_DATA)

@SuppressLint("SetJavaScriptEnabled")
@Composable
internal actual fun AcademyQuizWebView(
    launch: QuizWebLaunch,
    sessionController: QuizWebSessionController,
    modifier: Modifier,
    onPageReady: () -> Unit,
    onPageError: () -> Unit,
) {
    DisposableEffect(launch.logoutUrl) {
        sessionController.activate(launch.logoutUrl)
        onDispose { }
    }
    key(launch.quizId, launch.expiresAt) {
        AndroidView(
            modifier = modifier,
            factory = { context ->
                val container = FrameLayout(context)
                if (!supportsIsolatedQuizProfile()) {
                    onPageError()
                    return@AndroidView container
                }
                val androidController = sessionController as? AndroidQuizWebSessionController
                if (androidController == null) {
                    onPageError()
                    return@AndroidView container
                }
                val webView = WebView(context)
                WebViewCompat.setProfile(webView, androidController.profileNameForWebView())
                webView.apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.allowFileAccess = false
                    settings.allowContentAccess = false
                    settings.javaScriptCanOpenWindowsAutomatically = false
                    settings.setSupportMultipleWindows(false)
                    settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                    webViewClient = QuizWebViewClient(launch, onPageReady, onPageError)
                    postUrl(launch.redeemUrl, quizLaunchBody(launch))
                }
                container.apply {
                    addView(
                        webView,
                        FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.MATCH_PARENT,
                            FrameLayout.LayoutParams.MATCH_PARENT,
                        ),
                    )
                }
            },
            onRelease = { container ->
                val webView = container.getChildAt(0) as? WebView ?: return@AndroidView
                webView.stopLoading()
                webView.onPause()
                webView.webViewClient = WebViewClient()
                webView.loadUrl("about:blank")
                webView.clearHistory()
                webView.destroy()
                container.removeAllViews()
            },
        )
    }
}

private class QuizWebViewClient(
    private val launch: QuizWebLaunch,
    private val onPageReady: () -> Unit,
    private val onPageError: () -> Unit,
) : WebViewClient() {
    override fun onPageFinished(view: WebView, url: String) {
        if (isSameQuizWebEndpoint(url, launch.quizUrl)) onPageReady()
    }

    override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
        if (request.isForMainFrame) onPageError()
    }

    override fun onReceivedHttpError(
        view: WebView,
        request: WebResourceRequest,
        errorResponse: WebResourceResponse,
    ) {
        if (request.isForMainFrame && errorResponse.statusCode >= 400) onPageError()
    }

    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean =
        request.isForMainFrame && !isAllowedQuizWebNavigation(request.url.toString(), launch)
}

private fun quizLaunchBody(launch: QuizWebLaunch): ByteArray = listOf(
    "ticket" to launch.ticket,
    "userId" to launch.userId.toString(),
    "courseId" to launch.courseId.toString(),
    "quizId" to launch.quizId.toString(),
).joinToString("&") { (name, value) ->
    "${urlEncode(name)}=${urlEncode(value)}"
}.toByteArray(StandardCharsets.UTF_8)

private fun urlEncode(value: String): String =
    URLEncoder.encode(value, StandardCharsets.UTF_8.toString())
