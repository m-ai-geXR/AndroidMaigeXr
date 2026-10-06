package com.xraiassistant.monetization

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import com.xraiassistant.BuildConfig
import com.xraiassistant.config.AppConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Resolves what the user has agreed to, once, above the provider.
 * Mirrors iOS AdConsentStore.swift, minus ATT, which has no Android equivalent.
 *
 * UMP (Google's consent platform) covers EEA/UK and regulated US states and
 * decides whether ads may be requested at all. Declining is a normal outcome,
 * not an error.
 */
class AdConsentStore(context: Context) {

    private val consentInformation: ConsentInformation =
        UserMessagingPlatform.getConsentInformation(context.applicationContext)

    /** `UNKNOWN` until [resolve] has run, which keeps providers from loading. */
    private val _consent = MutableStateFlow(AdConsent.UNKNOWN)
    val consent: StateFlow<AdConsent> = _consent.asStateFlow()

    /**
     * True when Settings must carry a standing way for the user to change their
     * mind. A one-time prompt at launch does not satisfy that requirement.
     */
    private val _privacyOptionsRequired = MutableStateFlow(false)
    val privacyOptionsRequired: StateFlow<Boolean> = _privacyOptionsRequired.asStateFlow()

    /**
     * Gather consent and return the result. Call once the activity's UI is up:
     * the form is presented over it.
     */
    suspend fun resolve(activity: Activity): AdConsent {
        if (!requestConsentInfoUpdate(activity)) {
            // Without an answer we must not assume permission. No ads this
            // session; the next launch tries again.
            _consent.value = AdConsent.UNKNOWN
            return AdConsent.UNKNOWN
        }

        suspendCancellableCoroutine { cont ->
            // Calls back even when no form is required, so this cannot hang.
            UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { error ->
                error?.let { log("form: ${it.message}") }
                cont.resume(Unit)
            }
        }

        return refresh()
    }

    /** Present the form that lets a user change a decision they already made. */
    suspend fun presentPrivacyOptions(activity: Activity): AdConsent {
        suspendCancellableCoroutine { cont ->
            UserMessagingPlatform.showPrivacyOptionsForm(activity) { error ->
                error?.let { log("privacy options: ${it.message}") }
                cont.resume(Unit)
            }
        }
        return refresh()
    }

    private fun refresh(): AdConsent {
        _privacyOptionsRequired.value = consentInformation.privacyOptionsRequirementStatus ==
            ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED

        // The SDK reads the stored TCF string itself for personalisation, so
        // the app only needs to know whether requesting is allowed.
        val resolved = if (consentInformation.canRequestAds()) AdConsent.PERSONALISED else AdConsent.DENIED
        _consent.value = resolved
        log("resolved $resolved")
        return resolved
    }

    private suspend fun requestConsentInfoUpdate(activity: Activity): Boolean =
        suspendCancellableCoroutine { cont ->
            consentInformation.requestConsentInfoUpdate(
                activity,
                requestParameters(activity),
                { cont.resume(true) },
                { error ->
                    log("consent info update failed: ${error.message}")
                    cont.resume(false)
                }
            )
        }

    /**
     * Lets a debug build see the EEA form without travelling: set
     * `maigexr.ump.debugGeography=eea` (or `us`, `other`) and
     * `maigexr.ump.testDeviceId=<id>` in local.properties. The id is printed by
     * the UMP SDK in Logcat on first run. Both fields are empty in release, so
     * no debug geography can ship.
     */
    private fun requestParameters(context: Context): ConsentRequestParameters {
        val builder = ConsentRequestParameters.Builder()
        val geography = when (BuildConfig.UMP_DEBUG_GEOGRAPHY.lowercase()) {
            "eea" -> ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA
            "us" -> ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_REGULATED_US_STATE
            "other" -> ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_OTHER
            else -> null
        }
        if (BuildConfig.DEBUG && geography != null) {
            val debug = ConsentDebugSettings.Builder(context).setDebugGeography(geography)
            BuildConfig.UMP_TEST_DEVICE_ID.takeIf { it.isNotBlank() }?.let { debug.addTestDeviceHashedId(it) }
            builder.setConsentDebugSettings(debug.build())
        }
        return builder.build()
    }

    private fun log(message: String) {
        if (AppConfig.showAdDebugLogs) Log.d("Consent", message)
    }
}
