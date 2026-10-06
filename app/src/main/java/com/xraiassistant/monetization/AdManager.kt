package com.xraiassistant.monetization

import android.app.Activity
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.xraiassistant.config.AppConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * How often an interstitial may appear. Data rather than scattered [AppConfig]
 * reads, so the rules can be stated and tested directly.
 */
data class AdPacing(
    /** Scene runs that must happen before an interstitial is considered. */
    val scenesBeforeInterstitial: Int,
    /** Minimum time between two interstitials. */
    val minimumIntervalMs: Long
) {
    companion object {
        fun fromAppConfig() = AdPacing(
            scenesBeforeInterstitial = AppConfig.scenesBeforeInterstitial,
            minimumIntervalMs = AppConfig.interstitialMinIntervalSeconds * 1000L
        )
    }
}

/**
 * Decides whether an ad may appear, and how often. Not how to fetch one — that is
 * the provider's job, behind [AdProvider]. Mirrors iOS AdManager.swift.
 *
 * This file must not import an ad SDK. [AdMobProvider] is the only place
 * com.google.android.gms.ads appears; if that stops being true the abstraction
 * has leaked.
 *
 * Three things live here rather than in a provider, because they must survive a
 * network swap unchanged:
 *
 * 1. **Which provider to build.** Paid, consent-denied and ads-disabled all
 *    collapse to [NoAdsProvider], so there is exactly one decision about
 *    entitlement in the app.
 * 2. **Pacing.** Scene counts and cooldowns.
 * 3. **Restraint.** Never over a generation in flight, never over an error.
 *
 * Provided as a singleton by `MonetizationModule`. Call from the main thread.
 */
class AdManager(
    private val entitlement: EntitlementSource,
    private val scope: CoroutineScope,
    private val adsEnabled: Boolean = AppConfig.adsEnabled,
    private val pacing: AdPacing = AdPacing.fromAppConfig(),
    private val buildProvider: (serveAds: Boolean) -> AdProvider,
    private val now: () -> Long = System::currentTimeMillis,
    private val log: (String) -> Unit = ::debugLog
) {
    /** True when a provider is live and may serve. */
    private val _adsAreServing = MutableStateFlow(false)
    val adsAreServing: StateFlow<Boolean> = _adsAreServing.asStateFlow()

    private var provider: AdProvider = NoAdsProvider
    private var consent = AdConsent.UNKNOWN
    private var started = false

    private var lastInterstitialAtMs: Long? = null
    private var scenesSinceInterstitial = 0
    private var generationInFlight = false
    private var isShowingError = false

    init {
        // A purchase, restore or refund after launch swaps the provider. Changes
        // before start() are ignored: rebuilding then would load ads before
        // consent, and start() reads the current value anyway.
        scope.launch {
            entitlement.isEntitled.collect { entitled ->
                if (started) {
                    log(if (entitled) "entitled — ads off" else "entitlement state: not entitled")
                    rebuildProvider()
                }
            }
        }
    }

    // ---------------------------------------------------------------------
    // Lifecycle
    // ---------------------------------------------------------------------

    /**
     * Start ads once consent has been resolved. Safe to call more than once.
     * Nothing loads before this, and passing [AdConsent.UNKNOWN] keeps it that
     * way: the consent decision gates the first request, not the first impression.
     */
    suspend fun start(consent: AdConsent) {
        this.consent = consent
        started = true
        rebuildProvider()
    }

    /** The user revisited the privacy options form. */
    suspend fun updateConsent(consent: AdConsent) {
        if (consent == this.consent) return
        this.consent = consent
        rebuildProvider()
    }

    private suspend fun rebuildProvider() {
        // The single decision about entitlement in the whole app.
        val serveAds = adsEnabled && !entitlement.isEntitled.value && consent.allowsAds

        provider = buildProvider(serveAds)
        provider.initialize(consent)

        _adsAreServing.value = provider.isAvailable
        log("provider=${provider.name} serving=${provider.isAvailable} consent=$consent")

        if (provider.isAvailable) provider.preload(AdFormat.INTERSTITIAL)
    }

    // ---------------------------------------------------------------------
    // Banner
    // ---------------------------------------------------------------------

    /** The banner to place, or null when there is none. */
    fun banner(): (@Composable (Modifier) -> Unit)? = provider.banner()

    // ---------------------------------------------------------------------
    // Restraint
    // ---------------------------------------------------------------------

    /**
     * An interstitial landing on someone waiting on a slow reasoning model reads
     * as a broken app, not as an ad. A flag rather than the iOS counter: Android
     * has one generation at a time, driven by `ChatViewModel.isLoading`.
     */
    fun setGenerationInFlight(inFlight: Boolean) {
        generationInFlight = inFlight
    }

    /** An ad must never cover the explanation of what just went wrong. */
    fun setErrorVisible(visible: Boolean) {
        isShowingError = visible
    }

    // ---------------------------------------------------------------------
    // Interstitials
    // ---------------------------------------------------------------------

    /**
     * Count a completed scene, and show an interstitial if every pacing rule
     * allows it. Called on the way out of a scene, so the user has already seen
     * the result they asked for.
     *
     * @return true only when an ad was actually shown.
     */
    suspend fun onSceneRun(activity: Activity?): Boolean {
        if (!_adsAreServing.value) return false

        scenesSinceInterstitial++
        if (!isInterstitialDue()) {
            log("scene $scenesSinceInterstitial/${pacing.scenesBeforeInterstitial}")
            return false
        }

        val shown = provider.showInterstitial(activity)
        if (shown) {
            lastInterstitialAtMs = now()
            scenesSinceInterstitial = 0
            log("interstitial shown")
        } else {
            // Not ready, or nothing to serve. The counter deliberately stays up
            // so the next scene tries again instead of waiting a full cycle.
            log("interstitial due but none available")
        }
        return shown
    }

    /** Every rule that has to hold before an interstitial may appear. */
    private fun isInterstitialDue(): Boolean {
        if (generationInFlight || isShowingError) return false
        if (scenesSinceInterstitial < pacing.scenesBeforeInterstitial) return false
        val last = lastInterstitialAtMs ?: return true
        return now() - last >= pacing.minimumIntervalMs
    }
}

private fun debugLog(message: String) {
    if (AppConfig.showAdDebugLogs) Log.d("AdManager", message)
}
