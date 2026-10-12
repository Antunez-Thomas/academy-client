package com.example.academyapp.course

import android.content.res.Configuration
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.academyapp.MainActivity
import androidx.lifecycle.Lifecycle
import java.io.ByteArrayInputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class BunnyFullscreenHostAndroidTest {
    @Test
    fun rotationKeepsActivityAndCustomViewThenExitRestoresOrientationAndSystemBars() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            lateinit var initialActivity: MainActivity
            lateinit var player: SyntheticPlayer
            lateinit var host: BunnyFullscreenHost
            lateinit var customView: View
            lateinit var callback: RecordingCustomViewCallback
            var priorOrientation = Configuration.ORIENTATION_UNDEFINED
            var priorSystemUi = 0
            var priorWindowFullscreenFlag = false
            var fullscreenState: Boolean? = null

            scenario.onActivity { activity ->
                initialActivity = activity
                player = syntheticAuthorizedWebView(activity)
            }
            assertTrue(player.pageLoaded.await(5, TimeUnit.SECONDS), "synthetic page did not load")
            var loadedUrl: String? = null
            scenario.onActivity { loadedUrl = player.webView.url }
            assertEquals(AUTHORIZED_TEST_URL, loadedUrl)

            scenario.onActivity { activity ->
                priorOrientation = activity.requestedOrientation
                priorSystemUi = activity.window.decorView.systemUiVisibility
                priorWindowFullscreenFlag = activity.window.attributes.flags and
                    WindowManager.LayoutParams.FLAG_FULLSCREEN != 0
                host = BunnyFullscreenHost(activity, player.webView, AUTHORIZED_TEST_URL) {
                    fullscreenState = it
                }
                customView = View(activity)
                callback = RecordingCustomViewCallback()
                host.showCustomView(customView, callback)
            }

            awaitCondition { currentOrientation(scenario) == Configuration.ORIENTATION_LANDSCAPE }

            scenario.onActivity { activity ->
                assertSame(initialActivity, activity, "orientation change must not recreate MainActivity")
                assertTrue(customView.parent is ViewGroup)
                assertFalse(
                    customView.parent === activity.findViewById(android.R.id.content),
                    "fullscreen custom view must not mutate the Activity content FrameLayout",
                )
                assertTrue(host.isFullscreen)
                assertTrue(fullscreenState == true)

                host.hideCustomView()
                assertFalse(host.isFullscreen)
                assertEquals(priorOrientation, activity.requestedOrientation)
                assertEquals(priorSystemUi, activity.window.decorView.systemUiVisibility)
                assertEquals(
                    priorWindowFullscreenFlag,
                    activity.window.attributes.flags and WindowManager.LayoutParams.FLAG_FULLSCREEN != 0,
                )
                assertEquals(1, callback.hiddenCount)
                assertTrue(fullscreenState == false)
            }

            awaitCondition { currentOrientation(scenario) == Configuration.ORIENTATION_PORTRAIT }

            scenario.onActivity { activity ->
                assertSame(initialActivity, activity)
                host.release()
                player.dispose()
            }
        }
    }

    @Test
    fun unauthorizedCustomViewDuplicateExitBackLifecycleAndReleaseAreSafe() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val unauthorized = WebView(activity).apply {
                    loadDataWithBaseURL(
                        "https://example.test/player",
                        "<html></html>",
                        "text/html",
                        "UTF-8",
                        null,
                    )
                }
                val rejectedHost = BunnyFullscreenHost(activity, unauthorized, AUTHORIZED_TEST_URL) { }
                val rejectedCallback = RecordingCustomViewCallback()
                rejectedHost.showCustomView(View(activity), rejectedCallback)
                assertFalse(rejectedHost.isFullscreen)
                assertEquals(1, rejectedCallback.hiddenCount)
                rejectedHost.release()
                unauthorized.destroy()
            }

            lateinit var player: SyntheticPlayer
            lateinit var host: BunnyFullscreenHost
            lateinit var initialCallback: RecordingCustomViewCallback
            lateinit var duplicateCallback: RecordingCustomViewCallback
            lateinit var releaseCallback: RecordingCustomViewCallback
            var initialOrientation = Configuration.ORIENTATION_UNDEFINED
            scenario.onActivity { activity -> player = syntheticAuthorizedWebView(activity) }
            assertTrue(player.pageLoaded.await(5, TimeUnit.SECONDS), "synthetic page did not load")
            var loadedUrl: String? = null
            scenario.onActivity { loadedUrl = player.webView.url }
            assertEquals(AUTHORIZED_TEST_URL, loadedUrl)
            scenario.onActivity { activity ->
                initialOrientation = activity.requestedOrientation
                host = BunnyFullscreenHost(activity, player.webView, AUTHORIZED_TEST_URL) { }
                initialCallback = RecordingCustomViewCallback()
                duplicateCallback = RecordingCustomViewCallback()
                host.showCustomView(View(activity), initialCallback)
                host.showCustomView(View(activity), duplicateCallback)
            }
            scenario.onActivity { activity ->
                assertTrue(host.isFullscreen)
                assertEquals(1, duplicateCallback.hiddenCount)
                host.exitFromSystemBack()
                host.exitFromSystemBack()
                assertFalse(host.isFullscreen)
                assertEquals(initialOrientation, activity.requestedOrientation)
                assertEquals(1, initialCallback.hiddenCount)

                releaseCallback = RecordingCustomViewCallback()
                host.showCustomView(View(activity), releaseCallback)
                host.release()
                assertFalse(host.isFullscreen)
                assertEquals(initialOrientation, activity.requestedOrientation)
                assertEquals(1, releaseCallback.hiddenCount)
                player.dispose()
            }

            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            scenario.onActivity { activity ->
                assertEquals(
                    initialOrientation,
                    activity.requestedOrientation,
                    "an orientation request queued before exit must remain cancelled",
                )
            }
        }
    }

    @Test
    fun exitBeforeFirstLayoutCancelsDeferredLandscapeRequest() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            lateinit var player: SyntheticPlayer
            lateinit var host: BunnyFullscreenHost
            lateinit var callback: RecordingCustomViewCallback
            var initialOrientation = Configuration.ORIENTATION_UNDEFINED

            scenario.onActivity { activity -> player = syntheticAuthorizedWebView(activity) }
            assertTrue(player.pageLoaded.await(5, TimeUnit.SECONDS), "synthetic page did not load")

            scenario.onActivity { activity ->
                initialOrientation = activity.requestedOrientation
                host = BunnyFullscreenHost(activity, player.webView, AUTHORIZED_TEST_URL) { }
                callback = RecordingCustomViewCallback()
                val chromiumLikeCustomView = FrameLayout(activity).apply {
                    addView(
                        FrameLayout(activity),
                        FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.MATCH_PARENT,
                            FrameLayout.LayoutParams.MATCH_PARENT,
                        ),
                    )
                }

                host.showCustomView(chromiumLikeCustomView, callback)
                host.exitFromSystemBack()

                assertFalse(host.isFullscreen)
                assertEquals(1, callback.hiddenCount)
                assertEquals(initialOrientation, activity.requestedOrientation)
            }

            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            scenario.onActivity { activity ->
                assertEquals(
                    initialOrientation,
                    activity.requestedOrientation,
                    "deferred landscape must not run after the fullscreen view exits",
                )
                host.release()
                player.dispose()
            }
        }
    }

    @Test
    fun lifecycleStopExitsFullscreenAndRestoresPriorOrientation() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            lateinit var player: SyntheticPlayer
            lateinit var host: BunnyFullscreenHost
            lateinit var callback: RecordingCustomViewCallback
            var priorOrientation = Configuration.ORIENTATION_UNDEFINED
            scenario.onActivity { activity -> player = syntheticAuthorizedWebView(activity) }
            assertTrue(player.pageLoaded.await(5, TimeUnit.SECONDS), "synthetic page did not load")
            scenario.onActivity { activity ->
                priorOrientation = activity.requestedOrientation
                host = BunnyFullscreenHost(activity, player.webView, AUTHORIZED_TEST_URL) { }
                callback = RecordingCustomViewCallback()
                host.showCustomView(View(activity), callback)
                assertTrue(host.isFullscreen)
                assertEquals(0, callback.hiddenCount)
            }

            scenario.moveToState(Lifecycle.State.CREATED)

            assertFalse(host.isFullscreen, "ON_STOP lifecycle observer should exit fullscreen")
            assertEquals(1, callback.hiddenCount)
            scenario.onActivity { activity ->
                assertEquals(priorOrientation, activity.requestedOrientation)
                host.release()
                player.dispose()
            }
        }
    }

    private fun syntheticAuthorizedWebView(activity: MainActivity): SyntheticPlayer {
        val pageLoaded = CountDownLatch(1)
        val webView = WebView(activity).apply {
            webViewClient = object : WebViewClient() {
                override fun shouldInterceptRequest(
                    view: WebView,
                    request: android.webkit.WebResourceRequest,
                ): android.webkit.WebResourceResponse? {
                    if (request.url.toString() != AUTHORIZED_TEST_URL) return null
                    return android.webkit.WebResourceResponse(
                        "text/html",
                        "UTF-8",
                        ByteArrayInputStream(
                            "<html><body>Local synthetic player fixture</body></html>".toByteArray(),
                        ),
                    )
                }

                override fun onPageFinished(view: WebView, url: String) {
                    if (url == AUTHORIZED_TEST_URL) pageLoaded.countDown()
                }
            }
        }
        val container = FrameLayout(activity).apply {
            addView(webView, FrameLayout.LayoutParams(1, 1))
        }
        (activity.window.decorView as ViewGroup).addView(container, ViewGroup.LayoutParams(1, 1))
        webView.loadUrl(AUTHORIZED_TEST_URL)
        return SyntheticPlayer(webView, container, pageLoaded)
    }

    private fun awaitCondition(condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + 8_000
        while (!condition() && System.currentTimeMillis() < deadline) Thread.sleep(100)
        assertTrue(condition(), "timed out waiting for display configuration")
    }

    private fun currentOrientation(scenario: ActivityScenario<MainActivity>): Int {
        var orientation = Configuration.ORIENTATION_UNDEFINED
        scenario.onActivity { orientation = it.resources.configuration.orientation }
        return orientation
    }

    private data class SyntheticPlayer(
        val webView: WebView,
        private val container: FrameLayout,
        val pageLoaded: CountDownLatch,
    ) {
        fun dispose() {
            webView.stopLoading()
            container.removeView(webView)
            (container.parent as? ViewGroup)?.removeView(container)
            webView.destroy()
        }
    }

    private class RecordingCustomViewCallback : WebChromeClient.CustomViewCallback {
        var hiddenCount = 0
            private set

        override fun onCustomViewHidden() {
            hiddenCount++
        }
    }

    private companion object {
        const val AUTHORIZED_TEST_URL =
            "https://iframe.mediadelivery.net/embed/450904/00000000-0000-4000-8000-000000000001"
    }
}
