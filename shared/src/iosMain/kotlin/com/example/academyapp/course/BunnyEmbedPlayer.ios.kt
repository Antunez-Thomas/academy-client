package com.example.academyapp.course

import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitInteropProperties
import androidx.compose.ui.viewinterop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSURL
import platform.Foundation.NSURLRequest
import platform.WebKit.WKNavigationAction
import platform.WebKit.WKNavigationActionPolicy
import platform.WebKit.WKNavigationDelegateProtocol
import platform.WebKit.WKWebView
import platform.WebKit.WKWebViewConfiguration
import platform.darwin.NSObject

@OptIn(ExperimentalForeignApi::class)
@Composable
internal actual fun BunnyEmbedPlayer(
    playbackUrl: String,
    modifier: Modifier,
) {
    key(playbackUrl) {
        val navigationDelegate = remember(playbackUrl) {
            BunnyEmbedNavigationDelegate(playbackUrl)
        }
        UIKitView(
            modifier = modifier,
            factory = {
                val configuration = WKWebViewConfiguration().apply {
                    allowsInlineMediaPlayback = true
                    defaultWebpagePreferences.allowsContentJavaScript = true
                }
                WKWebView(frame = CGRectMake(0.0, 0.0, 0.0, 0.0), configuration = configuration).apply {
                    this.navigationDelegate = navigationDelegate
                    loadPlaybackUrl(playbackUrl)
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
private fun WKWebView.loadPlaybackUrl(playbackUrl: String) {
    val url = NSURL(string = playbackUrl)
    loadRequest(NSURLRequest(uRL = url))
}

@OptIn(ExperimentalForeignApi::class)
private class BunnyEmbedNavigationDelegate(
    private val authorizedUrl: String,
) : NSObject(), WKNavigationDelegateProtocol {
    override fun webView(
        webView: WKWebView,
        decidePolicyForNavigationAction: WKNavigationAction,
        decisionHandler: (platform.WebKit.WKNavigationActionPolicy) -> Unit,
    ) {
        val targetFrame = decidePolicyForNavigationAction.targetFrame
        val isSubframe = targetFrame != null && !targetFrame.mainFrame
        val requestedUrl = decidePolicyForNavigationAction.request.URL?.absoluteString
        val isAllowed = isSubframe || (
            targetFrame?.mainFrame == true &&
                requestedUrl != null &&
                isAllowedBunnyEmbedNavigation(requestedUrl, authorizedUrl)
            )
        decisionHandler(
            if (isAllowed) {
                WKNavigationActionPolicy.WKNavigationActionPolicyAllow
            } else {
                WKNavigationActionPolicy.WKNavigationActionPolicyCancel
            },
        )
    }
}
