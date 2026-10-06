package com.xraiassistant.monetization

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Places whatever banner the current provider offers. Mirrors iOS AdBannerView.swift.
 *
 * Renders nothing, and reserves no space, unless a provider is serving: paid,
 * consent-denied and ads-disabled all arrive here as "not serving", so this view
 * never decides entitlement itself.
 */
@Composable
fun AdBannerView(
    adManager: AdManager,
    modifier: Modifier = Modifier
) {
    val serving by adManager.adsAreServing.collectAsStateWithLifecycle()
    if (!serving) return

    // Keyed on serving so a provider rebuild yields a fresh banner.
    val banner = remember(serving) { adManager.banner() } ?: return
    banner(modifier)
}
