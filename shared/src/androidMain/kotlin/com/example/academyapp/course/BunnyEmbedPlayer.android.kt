package com.example.academyapp.course

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.pm.ApplicationInfo
import android.graphics.Bitmap
import android.net.http.SslError
import android.util.Log
import android.view.View
import android.webkit.RenderProcessGoneDetail
import android.webkit.SslErrorHandler
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

@SuppressLint("SetJavaScriptEnabled")
@Composable
internal actual fun BunnyEmbedPlayer(
    playbackUrl: String,
    modifier: Modifier,
) {
    key(playbackUrl) {
        val isFullscreen = remember { mutableStateOf(false) }
        val hostReference = remember { mutableStateOf<BunnyFullscreenHost?>(null) }
        val fullscreen by isFullscreen
        BackHandler(enabled = fullscreen) {
            hostReference.value?.exitFromSystemBack()
        }
        AndroidView(
            modifier = modifier,
            factory = { context ->
                val diagnosticsEnabled =
                    context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
                val webView = WebView(context).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.allowFileAccess = false
                    settings.allowContentAccess = false
                    settings.javaScriptCanOpenWindowsAutomatically = false
                    settings.setSupportMultipleWindows(false)
                    settings.cacheMode = WebSettings.LOAD_NO_CACHE
                    settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                    webViewClient = BunnyEmbedWebViewClient(playbackUrl, diagnosticsEnabled)
                    val activity = context.findActivity()
                    if (activity != null) {
                        hostReference.value = BunnyFullscreenHost(
                            activity = activity,
                            webView = this,
                            authorizedUrl = playbackUrl,
                            onFullscreenChanged = { isFullscreen.value = it },
                        )
                        webChromeClient = object : WebChromeClient() {
                            override fun onShowCustomView(view: View, callback: CustomViewCallback) {
                                if (diagnosticsEnabled) Log.d(DIAGNOSTIC_TAG, "fullscreen_requested")
                                hostReference.value?.showCustomView(view, callback)
                                    ?: callback.onCustomViewHidden()
                            }

                            override fun onHideCustomView() {
                                if (diagnosticsEnabled) Log.d(DIAGNOSTIC_TAG, "fullscreen_hidden")
                                hostReference.value?.hideCustomView()
                            }
                        }
                    }
                    loadUrl(playbackUrl)
                }
                bunnyEmbedContainer(context, webView)
            },
            onRelease = { container ->
                val webView = container.getChildAt(0) as? WebView ?: return@AndroidView
                hostReference.value?.release()
                hostReference.value = null
                webView.stopLoading()
                webView.onPause()
                webView.webChromeClient = null
                webView.webViewClient = WebViewClient()
                webView.loadUrl("about:blank")
                webView.clearHistory()
                webView.removeAllViews()
                webView.destroy()
                container.removeAllViews()
            },
        )
    }
}

private fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is android.content.ContextWrapper -> baseContext.findActivity()
    else -> null
}

internal fun bunnyEmbedContainer(context: Context, webView: WebView): FrameLayout =
    FrameLayout(context).apply {
        addView(
            webView,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
            ),
        )
    }

private class BunnyEmbedWebViewClient(
    private val authorizedUrl: String,
    private val diagnosticsEnabled: Boolean,
) : WebViewClient() {
    override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
        diagnostic("page_started")
        super.onPageStarted(view, url, favicon)
    }

    override fun onPageFinished(view: WebView, url: String) {
        diagnostic(
            "page_finished progress=${view.progress} content_height=${view.contentHeight} " +
                "view_size=${view.width}x${view.height}",
        )
        super.onPageFinished(view, url)
    }

    override fun onReceivedError(
        view: WebView,
        request: WebResourceRequest,
        error: WebResourceError,
    ) {
        diagnostic(
            "resource_error main_frame=${request.isForMainFrame} code=${error.errorCode} " +
                "host_class=${request.url.safeHostClass()}",
        )
        super.onReceivedError(view, request, error)
    }

    override fun onReceivedHttpError(
        view: WebView,
        request: WebResourceRequest,
        errorResponse: WebResourceResponse,
    ) {
        diagnostic(
            "resource_http main_frame=${request.isForMainFrame} status=${errorResponse.statusCode} " +
                "host_class=${request.url.safeHostClass()} mime=${errorResponse.mimeType.safeMimeType()}",
        )
        super.onReceivedHttpError(view, request, errorResponse)
    }

    override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: SslError) {
        diagnostic("ssl_error primary=${error.primaryError}")
        super.onReceivedSslError(view, handler, error)
    }

    override fun onRenderProcessGone(view: WebView, detail: RenderProcessGoneDetail): Boolean {
        diagnostic(
            "renderer_gone crashed=${detail.didCrash()} priority=${detail.rendererPriorityAtExit()}",
        )
        return super.onRenderProcessGone(view, detail)
    }

    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
        if (!request.isForMainFrame) return false
        val blocked = !isAllowedBunnyEmbedNavigation(request.url.toString(), authorizedUrl)
        if (blocked) diagnostic("main_frame_navigation_blocked")
        return blocked
    }

    private fun diagnostic(message: String) {
        if (diagnosticsEnabled) Log.d(DIAGNOSTIC_TAG, message)
    }

}

private const val DIAGNOSTIC_TAG = "AcademyBunnyWebView"

private fun android.net.Uri.safeHostClass(): String = when {
    host.equals("iframe.mediadelivery.net", ignoreCase = true) -> "embed"
    host?.endsWith("bunnycdn.com", ignoreCase = true) == true -> "bunny_cdn"
    host?.endsWith("b-cdn.net", ignoreCase = true) == true -> "bunny_cdn"
    host?.endsWith("bunny.net", ignoreCase = true) == true -> "bunny_service"
    else -> "other"
}

private fun String?.safeMimeType(): String =
    this?.takeIf { it.matches(Regex("[A-Za-z0-9.+-]+/[A-Za-z0-9.+-]+")) } ?: "unknown"
