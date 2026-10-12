package com.example.academyapp.course

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitInteropProperties
import androidx.compose.ui.viewinterop.UIKitView
import com.example.academyapp.api.model.QuizWebLaunch
import io.ktor.http.Url
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCSignatureOverride
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSError
import platform.Foundation.NSDate
import platform.Foundation.NSMutableURLRequest
import platform.Foundation.NSHTTPURLResponse
import platform.Foundation.NSData
import platform.Foundation.NSURL
import platform.Foundation.NSURLRequest
import platform.Foundation.create
import platform.Foundation.setHTTPBody
import platform.Foundation.setHTTPMethod
import platform.Foundation.setValue
import platform.WebKit.WKNavigation
import platform.WebKit.WKNavigationAction
import platform.WebKit.WKNavigationActionPolicy
import platform.WebKit.WKNavigationDelegateProtocol
import platform.WebKit.WKNavigationResponse
import platform.WebKit.WKNavigationResponsePolicy
import platform.WebKit.WKWebView
import platform.WebKit.WKWebViewConfiguration
import platform.WebKit.WKWebsiteDataStore
import platform.darwin.NSObject
import kotlin.coroutines.resume

@OptIn(ExperimentalForeignApi::class)
class IosQuizWebSessionController : QuizWebSessionController {
    internal val dataStore: WKWebsiteDataStore = WKWebsiteDataStore.nonPersistentDataStore()
    private var activeLogoutUrl: String? = null
    private var logoutDelegate: QuizLogoutNavigationDelegate? = null

    override fun hasPendingCleanup(): Boolean = activeLogoutUrl != null

    override fun activate(logoutUrl: String) {
        val parsed = runCatching { Url(logoutUrl) }.getOrNull() ?: return
        if (parsed.protocol.name == "https" ||
            (parsed.protocol.name == "http" && parsed.host in setOf("127.0.0.1", "localhost"))
        ) {
            activeLogoutUrl = logoutUrl
        }
    }

    override suspend fun revokeAndClear(): QuizWebSessionCleanupResult = withContext(Dispatchers.Main) {
        val logoutUrl = activeLogoutUrl ?: return@withContext QuizWebSessionCleanupResult.NoActiveSession
        val result = withTimeoutOrNull(5_000) {
            suspendCancellableCoroutine { continuation ->
                val configuration = WKWebViewConfiguration().apply { websiteDataStore = dataStore }
                val webView = WKWebView(
                    frame = CGRectMake(0.0, 0.0, 0.0, 0.0),
                    configuration = configuration,
                )
                val delegate = QuizLogoutNavigationDelegate(logoutUrl) { succeeded ->
                    webView.stopLoading()
                    webView.navigationDelegate = null
                    logoutDelegate = null
                    if (continuation.isActive) {
                        continuation.resume(
                            if (succeeded) QuizWebSessionCleanupResult.Cleared
                            else QuizWebSessionCleanupResult.Failed,
                        )
                    }
                }
                logoutDelegate = delegate
                webView.navigationDelegate = delegate
                webView.loadRequest(postRequest(logoutUrl, ""))
                continuation.invokeOnCancellation {
                    webView.stopLoading()
                    webView.navigationDelegate = null
                    logoutDelegate = null
                }
            }
        } ?: QuizWebSessionCleanupResult.Failed
        if (result == QuizWebSessionCleanupResult.Cleared) {
            clearIsolatedWebsiteData(dataStore)
            activeLogoutUrl = null
        }
        result
    }
}

@OptIn(ExperimentalForeignApi::class)
private suspend fun clearIsolatedWebsiteData(store: WKWebsiteDataStore) {
    suspendCancellableCoroutine { continuation ->
        store.removeDataOfTypes(
            dataTypes = WKWebsiteDataStore.allWebsiteDataTypes(),
            modifiedSince = NSDate(timeIntervalSinceReferenceDate = -10_000_000_000.0),
        ) {
            if (continuation.isActive) continuation.resume(Unit)
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
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
        val iosController = sessionController as? IosQuizWebSessionController
        val delegate = remember(launch.expiresAt) {
            QuizNavigationDelegate(launch, onPageReady, onPageError)
        }
        UIKitView(
            modifier = modifier,
            factory = {
                val configuration = WKWebViewConfiguration().apply {
                    websiteDataStore = iosController?.dataStore ?: WKWebsiteDataStore.nonPersistentDataStore()
                    defaultWebpagePreferences.allowsContentJavaScript = true
                }
                WKWebView(frame = CGRectMake(0.0, 0.0, 0.0, 0.0), configuration = configuration).apply {
                    navigationDelegate = delegate
                    loadRequest(postRequest(launch.redeemUrl, quizLaunchFormBody(launch)))
                }
            },
            onRelease = { webView ->
                webView.stopLoading()
                webView.navigationDelegate = null
                webView.loadHTMLString("", baseURL = null)
            },
            properties = UIKitInteropProperties(
                isInteractive = true,
                isNativeAccessibilityEnabled = true,
            ),
        )
    }
}

@OptIn(ExperimentalForeignApi::class)
private class QuizNavigationDelegate(
    private val launch: QuizWebLaunch,
    private val onPageReady: () -> Unit,
    private val onPageError: () -> Unit,
) : NSObject(), WKNavigationDelegateProtocol {
    override fun webView(
        webView: WKWebView,
        decidePolicyForNavigationAction: WKNavigationAction,
        decisionHandler: (WKNavigationActionPolicy) -> Unit,
    ) {
        val target = decidePolicyForNavigationAction.targetFrame
        val subframe = target != null && !target.mainFrame
        val requested = decidePolicyForNavigationAction.request.URL?.absoluteString
        val allowed = subframe || (target?.mainFrame == true && requested != null &&
            isAllowedQuizWebNavigation(requested, launch))
        decisionHandler(if (allowed) WKNavigationActionPolicy.WKNavigationActionPolicyAllow else WKNavigationActionPolicy.WKNavigationActionPolicyCancel)
    }

    override fun webView(webView: WKWebView, didFinishNavigation: WKNavigation?) {
        val current = webView.URL?.absoluteString ?: return
        if (isSameQuizWebEndpoint(current, launch.quizUrl)) onPageReady()
    }

    @ObjCSignatureOverride
    override fun webView(webView: WKWebView, didFailNavigation: WKNavigation?, withError: NSError) {
        onPageError()
    }

    @ObjCSignatureOverride
    override fun webView(webView: WKWebView, didFailProvisionalNavigation: WKNavigation?, withError: NSError) {
        onPageError()
    }
}

@OptIn(ExperimentalForeignApi::class)
private class QuizLogoutNavigationDelegate(
    private val logoutUrl: String,
    private val finished: (Boolean) -> Unit,
) : NSObject(), WKNavigationDelegateProtocol {
    private var acceptedResponse = false
    private var completed = false

    private fun complete(succeeded: Boolean) {
        if (completed) return
        completed = true
        finished(succeeded)
    }

    override fun webView(
        webView: WKWebView,
        decidePolicyForNavigationResponse: WKNavigationResponse,
        decisionHandler: (WKNavigationResponsePolicy) -> Unit,
    ) {
        val status = (decidePolicyForNavigationResponse.response as? NSHTTPURLResponse)?.statusCode ?: 0
        val responseUrl = decidePolicyForNavigationResponse.response.URL?.absoluteString
        val expectedSuccess = decidePolicyForNavigationResponse.forMainFrame &&
            status in 200..299 && responseUrl != null && isSameQuizWebEndpoint(responseUrl, logoutUrl)
        val failed = decidePolicyForNavigationResponse.forMainFrame && !expectedSuccess
        if (expectedSuccess) acceptedResponse = true
        decisionHandler(
            if (failed) WKNavigationResponsePolicy.WKNavigationResponsePolicyCancel
            else WKNavigationResponsePolicy.WKNavigationResponsePolicyAllow,
        )
        if (failed) complete(false)
    }

    override fun webView(webView: WKWebView, didFinishNavigation: WKNavigation?) = complete(acceptedResponse)
    @ObjCSignatureOverride
    override fun webView(webView: WKWebView, didFailNavigation: WKNavigation?, withError: NSError) = complete(false)
    @ObjCSignatureOverride
    override fun webView(webView: WKWebView, didFailProvisionalNavigation: WKNavigation?, withError: NSError) = complete(false)
}

@OptIn(ExperimentalForeignApi::class)
private fun postRequest(url: String, body: String): NSURLRequest {
    val request: NSMutableURLRequest = NSMutableURLRequest(uRL = NSURL(string = url))
    request.setHTTPMethod("POST")
    request.setValue("application/x-www-form-urlencoded", forHTTPHeaderField = "Content-Type")
    request.setHTTPBody(body.encodeToByteArray().toNSData())
    return request
}

@OptIn(ExperimentalForeignApi::class)
private fun quizLaunchFormBody(launch: QuizWebLaunch): String = listOf(
    "ticket" to launch.ticket,
    "userId" to launch.userId.toString(),
    "courseId" to launch.courseId.toString(),
    "quizId" to launch.quizId.toString(),
).joinToString("&") { (name, value) -> "$name=$value" }

@OptIn(ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)
private fun ByteArray.toNSData(): NSData = usePinned {
    NSData.create(bytes = it.addressOf(0), length = size.toULong())
}
