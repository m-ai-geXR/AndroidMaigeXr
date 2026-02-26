package com.xraiassistant.config

import android.util.Log
import com.xraiassistant.BuildConfig

/**
 * Centralized app configuration — mirrors iOS AppConfig.swift.
 * All values are sourced from BuildConfig fields set per build type in build.gradle.kts,
 * which serves the same role as iOS .env.development / .env.production files.
 *
 * Debug build:   ADS_ENABLED=false, test IDs, fast frequency caps (30s / 1 scene)
 * Release build: ADS_ENABLED=true, production IDs, user-friendly caps (8 min / 3 scenes)
 */
object AppConfig {

    // -------------------------------------------------------------------------
    // Monetization
    // -------------------------------------------------------------------------

    /** Master ad switch. False in debug, true in release. */
    val adsEnabled: Boolean get() = BuildConfig.ADS_ENABLED

    /** Bypass all ads (for testing premium UX). Never true in release. */
    val forcePremiumMode: Boolean get() = BuildConfig.FORCE_PREMIUM

    /** Log ad lifecycle events to Logcat. */
    val showAdDebugLogs: Boolean get() = BuildConfig.AD_DEBUG_LOGS

    // -------------------------------------------------------------------------
    // Ad Unit IDs
    // -------------------------------------------------------------------------

    val admobBannerId: String get() = BuildConfig.ADMOB_BANNER_ID
    val admobInterstitialId: String get() = BuildConfig.ADMOB_INTERSTITIAL_ID
    val admobRewardedId: String get() = BuildConfig.ADMOB_REWARDED_ID

    // -------------------------------------------------------------------------
    // Frequency Caps
    // -------------------------------------------------------------------------

    /** Minimum seconds between interstitial ads. Debug: 30s, Release: 480s (8 min). */
    val interstitialMinIntervalSeconds: Int get() = BuildConfig.INTERSTITIAL_INTERVAL_SECONDS

    /** Number of scene runs before showing an interstitial. Debug: 1, Release: 3. */
    val scenesBeforeInterstitial: Int get() = BuildConfig.SCENES_BEFORE_INTERSTITIAL

    // -------------------------------------------------------------------------
    // Feature Flags
    // -------------------------------------------------------------------------

    /** Premium subscription available to unlock ad-free experience. */
    val premiumSubscriptionEnabled: Boolean = true

    /** Cloud sync available in release only (infrastructure not yet implemented). */
    val cloudSyncEnabled: Boolean = !BuildConfig.DEBUG

    // -------------------------------------------------------------------------
    // Diagnostics
    // -------------------------------------------------------------------------

    fun printConfiguration() {
        if (BuildConfig.DEBUG) {
            Log.d("AppConfig", "=== AppConfig ===")
            Log.d("AppConfig", "adsEnabled         = $adsEnabled")
            Log.d("AppConfig", "forcePremiumMode    = $forcePremiumMode")
            Log.d("AppConfig", "showAdDebugLogs     = $showAdDebugLogs")
            Log.d("AppConfig", "bannerId            = $admobBannerId")
            Log.d("AppConfig", "interstitialId      = $admobInterstitialId")
            Log.d("AppConfig", "rewardedId          = $admobRewardedId")
            Log.d("AppConfig", "interstitialInterval= ${interstitialMinIntervalSeconds}s")
            Log.d("AppConfig", "scenesBeforeAd      = $scenesBeforeInterstitial")
            Log.d("AppConfig", "cloudSyncEnabled    = $cloudSyncEnabled")
            Log.d("AppConfig", "=================")
        }
    }
}
