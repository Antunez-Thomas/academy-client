package com.example.academyapp.course

import android.webkit.WebView
import android.widget.FrameLayout
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BunnyEmbedPlayerAndroidTest {
    @Test
    fun webViewIsAttachedToAMatchParentNativeContainer() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.runOnMainSync {
            val context = instrumentation.targetContext
            val webView = WebView(context)

            val container = bunnyEmbedContainer(context, webView)
            val layoutParams = webView.layoutParams as FrameLayout.LayoutParams

            assertSame(webView, container.getChildAt(0))
            assertEquals(FrameLayout.LayoutParams.MATCH_PARENT, layoutParams.width)
            assertEquals(FrameLayout.LayoutParams.MATCH_PARENT, layoutParams.height)

            container.removeAllViews()
            webView.destroy()
        }
    }

}
