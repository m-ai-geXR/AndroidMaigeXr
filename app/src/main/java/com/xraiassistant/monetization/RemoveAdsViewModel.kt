package com.xraiassistant.monetization

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Backs the Settings "Ads" section: the one purchase, Restore, and the standing
 * privacy-options control.
 */
@HiltViewModel
class RemoveAdsViewModel @Inject constructor(
    private val billing: BillingEntitlement,
    private val consentStore: AdConsentStore,
    private val adManager: AdManager
) : ViewModel() {

    val isEntitled = billing.isEntitled
    val product = billing.product
    val state = billing.state
    val privacyOptionsRequired = consentStore.privacyOptionsRequired

    /** Always from Play, never hardcoded: the currency is not ours to guess. */
    val displayPrice: String? get() = billing.displayPrice

    fun buy(activity: Activity) {
        billing.clearStatus()
        billing.purchase(activity)
    }

    fun restore() {
        billing.clearStatus()
        viewModelScope.launch { billing.restore() }
    }

    fun presentPrivacyOptions(activity: Activity) {
        viewModelScope.launch {
            adManager.updateConsent(consentStore.presentPrivacyOptions(activity))
        }
    }
}
