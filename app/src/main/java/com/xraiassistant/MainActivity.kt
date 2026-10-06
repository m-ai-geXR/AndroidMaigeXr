package com.xraiassistant

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.lifecycleScope
import com.xraiassistant.config.AppConfig
import com.xraiassistant.monetization.AdConsent
import com.xraiassistant.monetization.AdConsentStore
import com.xraiassistant.monetization.AdManager
import com.xraiassistant.monetization.BillingEntitlement
import kotlinx.coroutines.launch
import com.xraiassistant.ui.components.SplashScreen
import com.xraiassistant.ui.screens.MainScreen
import com.xraiassistant.ui.theme.AppearanceStore
import com.xraiassistant.ui.theme.XRAiAssistantTheme
import com.xraiassistant.ui.viewmodels.ChatViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * MainActivity - Entry point for XRAiAssistant Android
 *
 * Equivalent to ContentView.swift in the iOS version
 * Provides dual-pane interface: Chat + 3D Scene Playground
 *
 * Features:
 * - Three.js vaporwave splash screen on launch
 * - Auto-dismiss after 2.5 seconds or tap to skip
 * - Smooth fade transition to main app
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var adManager: AdManager
    @Inject lateinit var billing: BillingEntitlement
    @Inject lateinit var consentStore: AdConsentStore

    private var monetizationStarted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d("XRAiAssistant", "MainActivity onCreate started")

        try {
            super.onCreate(savedInstanceState)
            Log.d("XRAiAssistant", "super.onCreate completed")

            // Switch from splash theme to main theme
            setTheme(R.style.Theme_XRAiAssistant)

            setContent {
                Log.d("XRAiAssistant", "setContent started")

                // Splash screen state (matching iOS implementation)
                var showSplash by remember { mutableStateOf(true) }

                // Loaded before first composition so the app does not flash the
                // wrong theme while a ViewModel spins up.
                AppearanceStore.load(this)
                XRAiAssistantTheme {
                    Log.d("XRAiAssistant", "XRAiAssistantTheme started")

                    Box(modifier = Modifier.fillMaxSize()) {
                        // Main app (rendered behind splash, hidden initially)
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = MaterialTheme.colorScheme.background
                        ) {
                            Log.d("XRAiAssistant", "Surface started")

                            val chatViewModel: ChatViewModel = hiltViewModel()
                            Log.d("XRAiAssistant", "ChatViewModel created successfully")

                            MainScreen(chatViewModel = chatViewModel, adManager = adManager)
                            Log.d("XRAiAssistant", "MainScreen composed successfully")
                        }

                        // Animated splash overlay
                        AnimatedVisibility(
                            visible = showSplash,
                            exit = fadeOut(animationSpec = tween(durationMillis = 600))
                        ) {
                            SplashScreen(
                                onDismiss = {
                                    Log.d("XRAiAssistant", "Splash screen dismissed")
                                    showSplash = false
                                    // After the splash, so the consent form is
                                    // presented over the app rather than over it.
                                    startMonetization()
                                }
                            )
                        }
                    }
                }
            }

            Log.d("XRAiAssistant", "MainActivity onCreate completed successfully")
        } catch (e: Exception) {
            Log.e("XRAiAssistant", "Error in MainActivity onCreate", e)
            throw e
        }
    }

    /**
     * Entitlement first, then consent, then ads. A user who has paid is never
     * shown a consent form for ads they will not be served; for them consent
     * stays UNKNOWN, which is not the same as being refused.
     */
    private fun startMonetization() {
        if (monetizationStarted) return
        monetizationStarted = true

        lifecycleScope.launch {
            billing.start()

            if (!AppConfig.adsEnabled || billing.isEntitled.value) {
                adManager.start(AdConsent.UNKNOWN)
                return@launch
            }
            adManager.start(consentStore.resolve(this@MainActivity))
        }
    }
}
