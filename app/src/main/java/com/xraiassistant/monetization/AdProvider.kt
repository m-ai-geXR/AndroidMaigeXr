package com.xraiassistant.monetization

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * The seam between the app and whichever ad network is in use.
 * Mirrors iOS AdProvider.swift.
 *
 * Nothing network-specific may appear in this file. The moment an AdView or an
 * AdRequest appears in one of these signatures the abstraction has leaked and a
 * network swap stops being cheap. A banner is an opaque composable the app
 * places; it is not an AdMob object.
 */

/**
 * Which ad format a placement is asking for.
 *
 * Rewarded ads are deliberately absent. They were cut from v1 because the rewards
 * the old code offered (premium model access, GLB/FBX/USD export, cloud sync,
 * unlimited favourites) pointed at features that are not built. Bringing them
 * back means a case here plus a method on the interface, which is the point: it
 * should be a deliberate change, not dormant code.
 */
enum class AdFormat { BANNER, INTERSTITIAL }

/**
 * What the user has agreed to. Resolved once, above the provider, and passed down.
 *
 * Android has no ATT equivalent: once UMP allows ads, the SDK reads the stored
 * TCF consent string itself to decide personalisation, so the app only ever
 * resolves [UNKNOWN], [DENIED] or [PERSONALISED]. [NON_PERSONALISED] stays for
 * parity with iOS and for a network that needs to be told explicitly.
 */
enum class AdConsent {
    /** Consent not gathered yet. Providers must not load anything. */
    UNKNOWN,
    /** Ads refused, or required consent was not given. No ads at all. */
    DENIED,
    /** Ads allowed, but without a tracking identifier. */
    NON_PERSONALISED,
    /** Ads allowed; personalisation follows the stored consent. */
    PERSONALISED;

    /**
     * The only question a provider needs to ask of consent. [UNKNOWN] and
     * [DENIED] both mean "load nothing", but are kept apart: one is a decision,
     * the other is the absence of one.
     */
    val allowsAds: Boolean
        get() = this == PERSONALISED || this == NON_PERSONALISED
}

/**
 * One ad network, behind a stable interface.
 *
 * Implementations must fail soft. A provider that cannot start serves no ads and
 * reports `isAvailable == false`; it must never crash or block the UI, because
 * this is third-party code on the startup path.
 *
 * Pacing is not here on purpose. How often an ad may appear is a product rule and
 * lives in [AdManager], so every network inherits it.
 */
interface AdProvider {
    /** For logs and diagnostics. Not shown to users. */
    val name: String

    /** False when the SDK failed to start, or there is nothing to serve. */
    val isAvailable: Boolean

    /** Start the SDK. Must be safe to call more than once. */
    suspend fun initialize(consent: AdConsent)

    /** A banner to place, or null when none is available. */
    fun banner(): (@Composable (Modifier) -> Unit)?

    /** Preload, so a later show is instant. Failure is not an error. */
    fun preload(format: AdFormat)

    /**
     * Show an interstitial. Returns false when none was shown, for any reason;
     * the caller continues regardless. An ad is never worth blocking a user.
     */
    suspend fun showInterstitial(activity: Activity?): Boolean
}

/**
 * The provider used when the user has paid, when consent forbids ads, or when ads
 * are switched off. The paid tier is a provider rather than a branch, so
 * entitlement is a single decision — which provider to build.
 */
object NoAdsProvider : AdProvider {
    override val name = "none"
    override val isAvailable = false
    override suspend fun initialize(consent: AdConsent) {}
    override fun banner(): (@Composable (Modifier) -> Unit)? = null
    override fun preload(format: AdFormat) {}
    override suspend fun showInterstitial(activity: Activity?): Boolean = false
}
