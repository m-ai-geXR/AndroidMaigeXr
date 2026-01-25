package com.xraiassistant.ui.components

import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView

/**
 * SplashScreen - Three.js Vaporwave Animation
 *
 * Displays an animated Three.js vaporwave scene with:
 * - Animated grid scrolling effect
 * - Particle system with neon colors (cyan/pink/purple)
 * - Bloom + chromatic aberration post-processing
 * - 2.5s auto-dismiss
 * - Tap-to-skip functionality
 *
 * Ported from iOS SplashWebView.swift implementation
 * Uses WebView with JavaScript interface for native bridge communication
 */
@Composable
fun SplashScreen(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    // Create the JavaScript bridge
    val splashBridge = remember { SplashBridge(onDismiss) }

    // WebView instance
    val webView = remember {
        WebView(context).apply {
            // Configure WebView settings
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                allowFileAccess = true
                allowContentAccess = true
                mediaPlaybackRequiresUserGesture = false
                cacheMode = WebSettings.LOAD_NO_CACHE

                // Enable hardware acceleration for WebGL
                setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)
            }

            // Set background to match the splash scene
            setBackgroundColor(android.graphics.Color.parseColor("#0A0A0A"))

            // Add JavaScript interface for dismiss callback
            addJavascriptInterface(splashBridge, "Android")

            // Set WebViewClient to handle page loading
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    println("Splash WebView loaded: $url")
                }
            }

            // Load splash.html from assets
            loadUrl("file:///android_asset/splash.html")
        }
    }

    // Cleanup when disposed
    DisposableEffect(Unit) {
        onDispose {
            webView.destroy()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0A))
    ) {
        AndroidView(
            factory = { webView },
            modifier = Modifier.fillMaxSize()
        )
    }
}

/**
 * JavaScript Bridge for Splash Screen
 *
 * Handles communication between JavaScript and Kotlin
 * Called from splash.html via Android.dismissSplash()
 */
class SplashBridge(private val onDismiss: () -> Unit) {

    /**
     * Called from JavaScript when splash should be dismissed
     * Posts to main thread since WebView callbacks are on WebCore thread
     */
    @JavascriptInterface
    fun dismissSplash() {
        println("SplashBridge: dismissSplash called from JavaScript")
        Handler(Looper.getMainLooper()).post {
            onDismiss()
        }
    }
}
