package com.example.academyapp.course

import android.app.Activity
import android.app.Dialog
import android.content.pm.ActivityInfo
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.widget.FrameLayout
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner

/** Owns the HTML5 custom view only for the authorized Bunny player WebView. */
@Suppress("DEPRECATION")
class BunnyFullscreenHost(
    private val activity: Activity,
    private val webView: WebView,
    private val authorizedUrl: String,
    private val onFullscreenChanged: (Boolean) -> Unit,
) {
    private val lifecycleOwner = activity as? LifecycleOwner
    private val lifecycleObserver = LifecycleEventObserver { _, event ->
        if (event == Lifecycle.Event.ON_STOP) exitForLifecycleStop()
    }

    private var customView: View? = null
    private var customViewCallback: WebChromeClient.CustomViewCallback? = null
    private var fullscreenDialog: Dialog? = null
    private var priorRequestedOrientation: Int? = null
    private var pendingOrientationView: View? = null
    private var pendingOrientationLayoutListener: View.OnLayoutChangeListener? = null
    private var pendingOrientationRunnable: Runnable? = null
    private var orientationRequestApplied = false
    private var released = false

    init {
        lifecycleOwner?.lifecycle?.addObserver(lifecycleObserver)
    }

    val isFullscreen: Boolean
        get() = customView != null

    fun showCustomView(
        view: View,
        callback: WebChromeClient.CustomViewCallback,
    ) {
        if (released || isFullscreen || !isAuthorizedPlayerDocument()) {
            callback.onCustomViewHidden()
            return
        }

        priorRequestedOrientation = activity.requestedOrientation
        customView = view
        customViewCallback = callback

        val dialog = Dialog(activity, android.R.style.Theme_Black_NoTitleBar_Fullscreen).apply {
            setCancelable(false)
            setOnKeyListener { _, keyCode, event ->
                if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
                    exitFromSystemBack()
                    true
                } else {
                    false
                }
            }
            setContentView(
                view,
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT,
                ),
            )
            show()
        }
        fullscreenDialog = dialog
        view.requestFocus()

        dialog.window?.apply {
            setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
            )
            addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        }?.decorView?.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            )
        scheduleLandscapeAfterFirstLayout(view)
        onFullscreenChanged(true)
    }

    /** Called for Bunny's onHideCustomView signal. Safe to call more than once. */
    fun hideCustomView() = finishFullscreen(notifyWebView = true)

    /** Compose routes system back here while the custom view is active. */
    fun exitFromSystemBack() = finishFullscreen(notifyWebView = true)

    /** Avoid retaining a fullscreen overlay while the app is backgrounded. */
    fun exitForLifecycleStop() = finishFullscreen(notifyWebView = true)

    fun release() {
        if (released) return
        released = true
        finishFullscreen(notifyWebView = true)
        lifecycleOwner?.lifecycle?.removeObserver(lifecycleObserver)
    }

    private fun finishFullscreen(notifyWebView: Boolean) {
        val view = customView ?: return
        cancelPendingOrientation(view)
        customView = null
        val callback = customViewCallback
        customViewCallback = null

        (view.parent as? ViewGroup)?.removeView(view)
        fullscreenDialog?.apply {
            setOnKeyListener(null)
            runCatching { dismiss() }
        }
        fullscreenDialog = null
        if (orientationRequestApplied) {
            priorRequestedOrientation?.let { orientation ->
                runCatching { activity.requestedOrientation = orientation }
            }
        }
        orientationRequestApplied = false
        priorRequestedOrientation = null
        onFullscreenChanged(false)

        if (notifyWebView) runCatching { callback?.onCustomViewHidden() }
    }

    private fun isAuthorizedPlayerDocument(): Boolean =
        isAllowedBunnyEmbedNavigation(webView.url.orEmpty(), authorizedUrl)

    /**
     * Chromium's fullscreen view can still mutate its internal hierarchy during its first measure.
     * Rotating in the same frame as attachment can therefore make FrameLayout observe a stale child
     * count. Wait for a completed layout and one more frame before requesting landscape.
     */
    private fun scheduleLandscapeAfterFirstLayout(view: View) {
        cancelPendingOrientation()
        pendingOrientationView = view

        val listener = View.OnLayoutChangeListener { laidOutView, _, _, _, _, _, _, _, _ ->
            val activeListener = pendingOrientationLayoutListener
            if (activeListener != null) laidOutView.removeOnLayoutChangeListener(activeListener)
            pendingOrientationLayoutListener = null

            val orientationRunnable = Runnable {
                pendingOrientationRunnable = null
                pendingOrientationView = null
                if (released || customView !== view || !view.isAttachedToWindow) return@Runnable

                val applied = runCatching {
                    activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                }.isSuccess
                orientationRequestApplied = applied
            }
            pendingOrientationRunnable = orientationRunnable
            laidOutView.postOnAnimation(orientationRunnable)
        }
        pendingOrientationLayoutListener = listener
        view.addOnLayoutChangeListener(listener)
        view.requestLayout()
    }

    private fun cancelPendingOrientation(view: View? = pendingOrientationView) {
        val pendingView = view ?: return
        pendingOrientationLayoutListener?.let(pendingView::removeOnLayoutChangeListener)
        pendingOrientationRunnable?.let(pendingView::removeCallbacks)
        pendingOrientationLayoutListener = null
        pendingOrientationRunnable = null
        pendingOrientationView = null
    }
}
