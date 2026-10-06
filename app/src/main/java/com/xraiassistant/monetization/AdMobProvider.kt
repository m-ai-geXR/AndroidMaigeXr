package com.xraiassistant.monetization

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.ads.mediation.admob.AdMobAdapter
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.xraiassistant.config.AppConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

/**
 * AdMob behind [AdProvider]. The only file that imports com.google.android.gms.ads;
 * if that stops being true the abstraction has leaked. Mirrors iOS AdMobProvider.swift.
 */
class AdMobProvider(context: Context) : AdProvider {

    private val appContext = context.applicationContext

    override val name = "admob"

    override var isAvailable = false
        private set

    private var consent = AdConsent.UNKNOWN
    private var interstitial: InterstitialAd? = null
    private var loadingInterstitial = false

    override suspend fun initialize(consent: AdConsent) {
        this.consent = consent
        if (!consent.allowsAds) {
            isAvailable = false
            return
        }
        if (isAvailable) return

        // Google recommends initializing off the main thread; it does disk and
        // network work. Safe to call repeatedly.
        withContext(Dispatchers.IO) {
            suspendCancellableCoroutine { cont ->
                MobileAds.initialize(appContext) { status ->
                    log("initialized: ${status.adapterStatusMap.keys}")
                    cont.resume(Unit)
                }
            }
        }
        isAvailable = true
    }

    override fun banner(): (@Composable (Modifier) -> Unit)? {
        if (!isAvailable) return null
        return { modifier ->
            AndroidView(
                factory = { context ->
                    AdView(context).apply {
                        setAdSize(AdSize.BANNER)
                        adUnitId = AppConfig.admobBannerId
                        loadAd(request())
                    }
                },
                onRelease = { it.destroy() },
                modifier = modifier.fillMaxWidth().height(50.dp)
            )
        }
    }

    override fun preload(format: AdFormat) {
        if (format != AdFormat.INTERSTITIAL || !isAvailable) return
        if (loadingInterstitial || interstitial != null) return
        loadingInterstitial = true

        InterstitialAd.load(
            appContext,
            AppConfig.admobInterstitialId,
            request(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitial = ad
                    loadingInterstitial = false
                    log("interstitial loaded")
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitial = null
                    loadingInterstitial = false
                    log("interstitial failed to load: ${error.message}")
                }
            }
        )
    }

    override suspend fun showInterstitial(activity: Activity?): Boolean {
        val ad = interstitial
        if (activity == null || ad == null) {
            preload(AdFormat.INTERSTITIAL)
            return false
        }
        interstitial = null

        return suspendCancellableCoroutine { cont ->
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdShowedFullScreenContent() {
                    if (cont.isActive) cont.resume(true)
                }

                override fun onAdFailedToShowFullScreenContent(error: AdError) {
                    log("interstitial failed to show: ${error.message}")
                    if (cont.isActive) cont.resume(false)
                    preload(AdFormat.INTERSTITIAL)
                }

                override fun onAdDismissedFullScreenContent() {
                    preload(AdFormat.INTERSTITIAL)
                }
            }
            ad.show(activity)
        }
    }

    /** Asks for non-personalised ads explicitly when consent says so. */
    private fun request(): AdRequest {
        val builder = AdRequest.Builder()
        if (consent == AdConsent.NON_PERSONALISED) {
            builder.addNetworkExtrasBundle(AdMobAdapter::class.java, Bundle().apply { putString("npa", "1") })
        }
        return builder.build()
    }

    private fun log(message: String) {
        if (AppConfig.showAdDebugLogs) Log.d("AdMob", message)
    }
}
