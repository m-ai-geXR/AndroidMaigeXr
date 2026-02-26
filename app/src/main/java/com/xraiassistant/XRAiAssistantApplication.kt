package com.xraiassistant

import android.app.Application
import com.google.android.gms.ads.MobileAds
import com.xraiassistant.BuildConfig
import com.xraiassistant.config.AppConfig
import dagger.hilt.android.HiltAndroidApp
import android.util.Log

/**
 * XRAiAssistant Application
 * 
 * AI-powered Extended Reality development platform for Android
 * Ported from iOS Swift to Kotlin with Jetpack Compose
 */
@HiltAndroidApp
class XRAiAssistantApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()

        Log.d("XRAiAssistant", "Application onCreate called")

        // Initialize AdMob SDK — must happen before any ad requests
        MobileAds.initialize(this) { initializationStatus ->
            AppConfig.printConfiguration()
            if (AppConfig.showAdDebugLogs) {
                Log.d("AdMob", "MobileAds initialized: $initializationStatus")
            }
        }

        try {
            // Initialize WebView early to create cache directories and prevent Crashpad errors
            // This prevents the "opendir /cache/WebView/Crashpad/attachments: No such file or directory" error
            initializeWebView()

            // Initialize WebView debugging in debug builds
            if (BuildConfig.DEBUG) {
                android.webkit.WebView.setWebContentsDebuggingEnabled(true)
                Log.d("XRAiAssistant", "WebView debugging enabled")
            }
            Log.d("XRAiAssistant", "Application onCreate completed successfully")
        } catch (e: Exception) {
            Log.e("XRAiAssistant", "Error in Application onCreate", e)
            throw e
        }
    }

    /**
     * Initialize WebView cache directories
     * Prevents Crashpad errors: "opendir /cache/WebView/Crashpad/attachments: No such file or directory"
     *
     * NOTE: We only create directories, NOT instantiate WebView here to avoid memory conflicts
     */
    private fun initializeWebView() {
        try {
            // Create WebView cache directory structure
            val webViewCache = java.io.File(cacheDir, "WebView")
            val crashpadCache = java.io.File(webViewCache, "Crashpad")
            val crashpadAttachments = java.io.File(crashpadCache, "attachments")
            val crashpadPending = java.io.File(crashpadCache, "pending")
            val crashpadCompleted = java.io.File(crashpadCache, "completed")

            // Create all Crashpad directories
            val dirsToCreate = listOf(
                webViewCache,
                crashpadCache,
                crashpadAttachments,
                crashpadPending,
                crashpadCompleted
            )

            var directoriesCreated = 0
            dirsToCreate.forEach { dir ->
                if (!dir.exists() && dir.mkdirs()) {
                    directoriesCreated++
                    Log.d("XRAiAssistant", "Created: ${dir.name}")
                }
            }

            if (directoriesCreated > 0) {
                Log.d("XRAiAssistant", "Created $directoriesCreated WebView cache directories")
            } else {
                Log.d("XRAiAssistant", "WebView cache directories already exist")
            }

            // IMPORTANT: Do NOT create WebView instance here - it can cause memory conflicts
            // WebView will be created lazily when needed in SceneScreen

        } catch (e: Exception) {
            // Non-critical - log but don't crash
            Log.w("XRAiAssistant", "WebView cache directory setup: ${e.message}")
        }
    }
}