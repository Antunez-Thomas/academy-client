package com.example.academyapp.course

import android.webkit.WebView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import kotlinx.coroutines.runBlocking
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotSame
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AcademyQuizWebViewAndroidTest {
    @Test
    fun logoutEndpointStoreSurvivesRecreationAndCanBeCleared() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val preferencesName = "academy_quiz_web_session_test_${System.nanoTime()}"
        val firstControllerStore = QuizWebLogoutUrlStore(context, preferencesName)
        val logoutUrl = "http://10.0.2.2:8080/wp-json/academy-mobile/v1/quiz/web-logout"

        firstControllerStore.write(logoutUrl)

        val recreatedControllerStore = QuizWebLogoutUrlStore(context, preferencesName)
        assertEquals(logoutUrl, recreatedControllerStore.read())
        recreatedControllerStore.clear()
        assertNull(firstControllerStore.read())
    }

    @Test
    fun logoutEndpointFromAnotherApiEnvironmentIsDiscardedWithoutARequest() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val preferencesName = "academy_quiz_web_environment_test_${System.nanoTime()}"
        QuizWebLogoutUrlStore(context, preferencesName).write(
            "http://10.0.2.2:8080/wp-json/academy-mobile/v1/quiz/web-logout",
        )

        val controller = AndroidQuizWebSessionController(
            context = context,
            apiBaseUrl = "https://example.test/wp-json/academy-mobile/v1/",
            preferencesName = preferencesName,
        )

        assertEquals(QuizWebSessionCleanupResult.NoActiveSession, controller.revokeAndClear())
    }

    @Test
    fun quizWebViewUsesAProfileSeparateFromTheDefaultBrowserStore() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val testProfile = "academy_quiz_web_test_${System.nanoTime()}"
        lateinit var defaultWebView: WebView
        lateinit var quizWebView: WebView
        instrumentation.runOnMainSync {
            assertTrue(WebViewFeature.isFeatureSupported(WebViewFeature.MULTI_PROFILE))
            assertTrue(WebViewFeature.isFeatureSupported(WebViewFeature.DELETE_BROWSING_DATA))
            val context = instrumentation.targetContext
            defaultWebView = WebView(context)
            quizWebView = WebView(context)
            WebViewCompat.setProfile(quizWebView, testProfile)

            val defaultProfile = WebViewCompat.getProfile(defaultWebView)
            val isolatedProfile = WebViewCompat.getProfile(quizWebView)

            assertNotEquals(defaultProfile.name, isolatedProfile.name)
            assertNotSame(defaultProfile.cookieManager, isolatedProfile.cookieManager)

        }

        val cookieWritten = CountDownLatch(1)
        val defaultCookieWritten = CountDownLatch(1)
        instrumentation.runOnMainSync {
            WebViewCompat.getProfile(quizWebView).cookieManager.setCookie(
                "https://example.test",
                "academy_quiz_test=present; Secure",
            ) { cookieWritten.countDown() }
            WebViewCompat.getProfile(defaultWebView).cookieManager.setCookie(
                "https://example.test",
                "default_profile_test=present; Secure",
            ) { defaultCookieWritten.countDown() }
        }
        assertTrue(cookieWritten.await(5, TimeUnit.SECONDS))
        assertTrue(defaultCookieWritten.await(5, TimeUnit.SECONDS))

        val dataCleared = CountDownLatch(1)
        instrumentation.runOnMainSync {
            val cookie = WebViewCompat.getProfile(quizWebView).cookieManager
                .getCookie("https://example.test")
            assertTrue(cookie?.contains("academy_quiz_test=present") == true)
            clearQuizBrowsingData(quizWebView) { dataCleared.countDown() }
        }
        assertTrue(dataCleared.await(5, TimeUnit.SECONDS))

        instrumentation.runOnMainSync {
            val cookie = WebViewCompat.getProfile(quizWebView).cookieManager
                .getCookie("https://example.test")
            val defaultCookie = WebViewCompat.getProfile(defaultWebView).cookieManager
                .getCookie("https://example.test")
            assertTrue(cookie?.contains("academy_quiz_test=present") != true)
            assertTrue(defaultCookie?.contains("default_profile_test=present") == true)
            defaultWebView.destroy()
            quizWebView.destroy()
        }
    }
}
