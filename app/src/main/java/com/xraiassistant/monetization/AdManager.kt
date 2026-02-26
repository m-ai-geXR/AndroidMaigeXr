package com.xraiassistant.monetization

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.xraiassistant.config.AppConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AdManager — centralized ad lifecycle controller.
 * Mirrors iOS AdManager.swift (singleton pattern, same reward types, same frequency logic).
 *
 * Manages:
 *  - Banner ads (returned as AdView for Compose AndroidView)
 *  - Interstitial ads with frequency capping (scene-count + time-based)
 *  - Rewarded video ads tied to premium feature unlocks
 *  - Premium user state (persisted in SharedPreferences)
 */
@Singleton
class AdManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    // -------------------------------------------------------------------------
    // Reward types — mirrors iOS AdManager.RewardType enum
    // -------------------------------------------------------------------------
    enum class RewardType(val displayName: String) {
        PREMIUM_MODEL_ACCESS("Premium AI Model Access"),
        ADVANCED_EXPORT("Advanced Scene Export (GLB/FBX/USD)"),
        CLOUD_SYNC("Cloud Sync — 24 hours"),
        UNLIMITED_FAVORITES_24H("Unlimited Favorites — 24 hours")
    }

    // -------------------------------------------------------------------------
    // State
    // -------------------------------------------------------------------------
    private val _isPremiumUser = MutableStateFlow(false)
    val isPremiumUser: StateFlow<Boolean> = _isPremiumUser.asStateFlow()

    private var interstitialAd: InterstitialAd? = null
    private var rewardedAd: RewardedAd? = null
    private var isLoadingInterstitial = false
    private var isLoadingRewarded = false

    // Frequency tracking
    private var sceneRunCount = 0
    private var lastInterstitialShownMs = 0L

    private val prefs by lazy {
        context.getSharedPreferences("ad_prefs", Context.MODE_PRIVATE)
    }

    // -------------------------------------------------------------------------
    // Initialization
    // -------------------------------------------------------------------------
    init {
        val savedPremium = prefs.getBoolean("is_premium", false)
        _isPremiumUser.value = AppConfig.forcePremiumMode || savedPremium
        if (AppConfig.adsEnabled) {
            preloadInterstitial()
            preloadRewardedAd()
        }
    }

    // -------------------------------------------------------------------------
    // Banner
    // -------------------------------------------------------------------------

    /**
     * Creates and loads a banner AdView.
     * Called from AdBannerView composable via AndroidView factory.
     */
    fun createBannerAdView(): AdView {
        return AdView(context).apply {
            setAdSize(AdSize.BANNER)
            adUnitId = AppConfig.admobBannerId
            loadAd(AdRequest.Builder().build())
            if (AppConfig.showAdDebugLogs) {
                Log.d("AdManager", "Banner loading: $adUnitId")
            }
        }
    }

    // -------------------------------------------------------------------------
    // Interstitial
    // -------------------------------------------------------------------------

    fun preloadInterstitial() {
        if (isLoadingInterstitial || interstitialAd != null) return
        isLoadingInterstitial = true

        InterstitialAd.load(
            context,
            AppConfig.admobInterstitialId,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    isLoadingInterstitial = false
                    log("Interstitial loaded")
                    ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                        override fun onAdDismissedFullScreenContent() {
                            interstitialAd = null
                            preloadInterstitial()  // Preload next
                        }
                        override fun onAdFailedToShowFullScreenContent(error: AdError) {
                            interstitialAd = null
                            preloadInterstitial()
                            log("Interstitial failed to show: ${error.message}")
                        }
                        override fun onAdShowedFullScreenContent() {
                            log("Interstitial shown")
                        }
                    }
                }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitialAd = null
                    isLoadingInterstitial = false
                    log("Interstitial failed to load: ${error.message}")
                }
            }
        )
    }

    /**
     * Called each time a scene is run. Shows an interstitial when:
     *  - Ads are enabled AND user is not premium
     *  - Scene run count >= scenesBeforeInterstitial threshold
     *  - Enough time has passed since the last interstitial
     */
    fun onSceneRun(activity: Activity) {
        if (!AppConfig.adsEnabled || _isPremiumUser.value) return

        sceneRunCount++
        val nowMs = System.currentTimeMillis()
        val elapsedSeconds = (nowMs - lastInterstitialShownMs) / 1000

        log("Scene run #$sceneRunCount, elapsed=${elapsedSeconds}s, threshold=${AppConfig.scenesBeforeInterstitial}")

        if (sceneRunCount >= AppConfig.scenesBeforeInterstitial &&
            elapsedSeconds >= AppConfig.interstitialMinIntervalSeconds
        ) {
            showInterstitial(activity)
            sceneRunCount = 0
            lastInterstitialShownMs = nowMs
        }
    }

    private fun showInterstitial(activity: Activity) {
        val ad = interstitialAd
        if (ad != null) {
            log("Showing interstitial")
            ad.show(activity)
        } else {
            log("Interstitial not ready — preloading")
            preloadInterstitial()
        }
    }

    // -------------------------------------------------------------------------
    // Rewarded
    // -------------------------------------------------------------------------

    fun preloadRewardedAd() {
        if (isLoadingRewarded || rewardedAd != null) return
        isLoadingRewarded = true

        RewardedAd.load(
            context,
            AppConfig.admobRewardedId,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                    isLoadingRewarded = false
                    log("Rewarded ad loaded")
                    ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                        override fun onAdDismissedFullScreenContent() {
                            rewardedAd = null
                            preloadRewardedAd()
                        }
                        override fun onAdFailedToShowFullScreenContent(error: AdError) {
                            rewardedAd = null
                            preloadRewardedAd()
                            log("Rewarded failed to show: ${error.message}")
                        }
                    }
                }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    rewardedAd = null
                    isLoadingRewarded = false
                    log("Rewarded failed to load: ${error.message}")
                }
            }
        )
    }

    /**
     * Shows a rewarded video ad. On successful reward, calls [onRewarded] with the [RewardType].
     * If ad is not ready, calls [onNotReady] so the caller can show a message to the user.
     */
    fun showRewardedAd(
        activity: Activity,
        rewardType: RewardType,
        onRewarded: (RewardType) -> Unit,
        onNotReady: () -> Unit = {}
    ) {
        val ad = rewardedAd
        if (ad == null) {
            log("Rewarded ad not ready")
            onNotReady()
            preloadRewardedAd()
            return
        }
        ad.show(activity) { _ ->
            log("User earned reward: ${rewardType.displayName}")
            onRewarded(rewardType)
        }
    }

    val isRewardedAdReady: Boolean get() = rewardedAd != null

    // -------------------------------------------------------------------------
    // Premium
    // -------------------------------------------------------------------------

    fun setPremiumUser(isPremium: Boolean) {
        _isPremiumUser.value = isPremium
        prefs.edit().putBoolean("is_premium", isPremium).apply()
        log("Premium user set to $isPremium")
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private fun log(message: String) {
        if (AppConfig.showAdDebugLogs) {
            Log.d("AdManager", message)
        }
    }
}
