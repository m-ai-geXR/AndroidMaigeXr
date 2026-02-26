package com.xraiassistant.monetization

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xraiassistant.config.AppConfig

/**
 * AdBannerView — Compose wrapper for AdMob banner ads.
 * Mirrors iOS AdBannerView.swift (UIViewRepresentable pattern).
 *
 * Renders nothing when:
 *  - Ads are disabled (debug by default)
 *  - User has premium status
 *
 * Standard banner height is 50dp per AdMob spec.
 */
@Composable
fun AdBannerView(
    adManager: AdManager,
    modifier: Modifier = Modifier
) {
    if (!AppConfig.adsEnabled) return

    val isPremium by adManager.isPremiumUser.collectAsStateWithLifecycle()
    if (isPremium) return

    AndroidView(
        factory = { adManager.createBannerAdView() },
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)
    )
}
