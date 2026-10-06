package com.xraiassistant.monetization

import com.xraiassistant.config.AppConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Where "has the user paid?" comes from. Mirrors iOS Entitlement.swift.
 *
 * Kept behind an interface so [AdManager] does not touch Play Billing and can be
 * unit tested without it. The app uses [BillingEntitlement].
 */
interface EntitlementSource {
    /**
     * True when the user owns "Remove Ads". Changes on a purchase here or on
     * another device, a refund, or a restore.
     */
    val isEntitled: StateFlow<Boolean>
}

/**
 * A stand-in for tests and previews. Reports **not entitled** unless forced, so a
 * test gets the free tier by default and has to opt into the paid one.
 *
 * It deliberately does not read the old `ad_prefs/is_premium` SharedPreferences
 * flag. That was a plain local boolean with no purchase behind it — trivially
 * settable, not an entitlement.
 */
class UnpurchasedEntitlement(
    entitled: Boolean = AppConfig.forcePremiumMode
) : EntitlementSource {
    override val isEntitled = MutableStateFlow(entitled)
}
