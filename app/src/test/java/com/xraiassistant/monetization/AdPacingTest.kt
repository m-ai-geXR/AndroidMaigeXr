package com.xraiassistant.monetization

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The product rules in [AdManager]: pacing, restraint and the single entitlement
 * decision. Mirrors iOS AdPacingTests.swift, against a provider that never
 * touches a network.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AdPacingTest {

    /** Records what it was asked to do. Serves whenever it is built to. */
    private class FakeProvider(
        private val serves: Boolean,
        var hasInterstitial: Boolean = true
    ) : AdProvider {
        override val name = if (serves) "fake" else "none"
        override var isAvailable = false
        var interstitialsShown = 0

        override suspend fun initialize(consent: AdConsent) {
            isAvailable = serves && consent.allowsAds
        }

        override fun banner(): (@Composable (Modifier) -> Unit)? =
            if (isAvailable) { _ -> } else null

        override fun preload(format: AdFormat) {}

        override suspend fun showInterstitial(activity: Activity?): Boolean {
            if (!isAvailable || !hasInterstitial) return false
            interstitialsShown++
            return true
        }
    }

    private var clockMs = 1_000_000L
    private val built = mutableListOf<FakeProvider>()
    private val provider get() = built.last()

    private fun TestScope.makeManager(
        scenesBeforeInterstitial: Int = 3,
        minimumIntervalMs: Long = 0,
        entitlement: EntitlementSource = UnpurchasedEntitlement(entitled = false),
        adsEnabled: Boolean = true,
        hasInterstitial: Boolean = true
    ) = AdManager(
        entitlement = entitlement,
        scope = CoroutineScope(backgroundScope.coroutineContext + UnconfinedTestDispatcher(testScheduler)),
        adsEnabled = adsEnabled,
        pacing = AdPacing(scenesBeforeInterstitial, minimumIntervalMs),
        buildProvider = { serveAds -> FakeProvider(serveAds, hasInterstitial).also { built += it } },
        now = { clockMs },
        log = {}
    )

    // -- Scene threshold ---------------------------------------------------

    @Test
    fun noInterstitialBeforeTheSceneThreshold() = runTest {
        val ads = makeManager(scenesBeforeInterstitial = 3)
        ads.start(AdConsent.PERSONALISED)

        assertFalse(ads.onSceneRun(null))
        assertFalse(ads.onSceneRun(null))
        assertEquals(0, provider.interstitialsShown)
    }

    @Test
    fun interstitialAtTheSceneThreshold() = runTest {
        val ads = makeManager(scenesBeforeInterstitial = 3)
        ads.start(AdConsent.PERSONALISED)

        repeat(2) { ads.onSceneRun(null) }
        assertTrue(ads.onSceneRun(null))
        assertEquals(1, provider.interstitialsShown)
    }

    @Test
    fun counterResetsAfterAnInterstitialSoAdsDoNotRepeatEveryRun() = runTest {
        val ads = makeManager(scenesBeforeInterstitial = 2)
        ads.start(AdConsent.PERSONALISED)

        repeat(4) { ads.onSceneRun(null) }
        assertEquals(2, provider.interstitialsShown)
    }

    @Test
    fun counterSurvivesAProviderWithNothingToShow() = runTest {
        val ads = makeManager(scenesBeforeInterstitial = 2, hasInterstitial = false)
        ads.start(AdConsent.PERSONALISED)

        repeat(2) { ads.onSceneRun(null) }
        provider.hasInterstitial = true

        // Due already, so the very next scene shows rather than waiting a cycle.
        assertTrue(ads.onSceneRun(null))
    }

    @Test
    fun cooldownBlocksASecondInterstitial() = runTest {
        val ads = makeManager(scenesBeforeInterstitial = 1, minimumIntervalMs = 60_000)
        ads.start(AdConsent.PERSONALISED)

        assertTrue(ads.onSceneRun(null))
        clockMs += 30_000
        assertFalse(ads.onSceneRun(null))
        clockMs += 30_000
        assertTrue(ads.onSceneRun(null))
    }

    // -- Restraint ---------------------------------------------------------

    @Test
    fun noInterstitialWhileAGenerationIsInFlight() = runTest {
        val ads = makeManager(scenesBeforeInterstitial = 1)
        ads.start(AdConsent.PERSONALISED)

        ads.setGenerationInFlight(true)
        assertFalse(ads.onSceneRun(null))
    }

    @Test
    fun interstitialResumesOnceTheGenerationFinishes() = runTest {
        val ads = makeManager(scenesBeforeInterstitial = 1)
        ads.start(AdConsent.PERSONALISED)

        ads.setGenerationInFlight(true)
        ads.onSceneRun(null)
        ads.setGenerationInFlight(false)
        assertTrue(ads.onSceneRun(null))
    }

    @Test
    fun noInterstitialOverAVisibleError() = runTest {
        val ads = makeManager(scenesBeforeInterstitial = 1)
        ads.start(AdConsent.PERSONALISED)

        ads.setErrorVisible(true)
        assertFalse(ads.onSceneRun(null))
        ads.setErrorVisible(false)
        assertTrue(ads.onSceneRun(null))
    }

    // -- The entitlement decision -------------------------------------------

    @Test
    fun paidUserGetsNoProviderAndNoBanner() = runTest {
        val ads = makeManager(scenesBeforeInterstitial = 1, entitlement = UnpurchasedEntitlement(entitled = true))
        ads.start(AdConsent.PERSONALISED)

        assertFalse(ads.adsAreServing.value)
        assertNull(ads.banner())
        assertFalse(ads.onSceneRun(null))
    }

    @Test
    fun refusedConsentServesNothing() = runTest {
        val ads = makeManager()
        ads.start(AdConsent.DENIED)
        assertFalse(ads.adsAreServing.value)
    }

    @Test
    fun unresolvedConsentServesNothing() = runTest {
        val ads = makeManager()
        ads.start(AdConsent.UNKNOWN)
        assertFalse(ads.adsAreServing.value)
    }

    @Test
    fun nonPersonalisedConsentStillServesAds() = runTest {
        val ads = makeManager()
        ads.start(AdConsent.NON_PERSONALISED)
        assertTrue(ads.adsAreServing.value)
    }

    @Test
    fun masterSwitchOffServesNothing() = runTest {
        val ads = makeManager(adsEnabled = false)
        ads.start(AdConsent.PERSONALISED)
        assertFalse(ads.adsAreServing.value)
    }

    @Test
    fun nothingLoadsBeforeStart() = runTest {
        val entitlement = UnpurchasedEntitlement(entitled = false)
        makeManager(entitlement = entitlement)

        // An entitlement change before consent must not build a provider.
        entitlement.isEntitled.value = true
        entitlement.isEntitled.value = false
        assertTrue(built.isEmpty())
    }

    @Test
    fun buyingRemovesAdsWithoutARelaunch() = runTest {
        val entitlement = UnpurchasedEntitlement(entitled = false)
        val ads = makeManager(entitlement = entitlement)
        ads.start(AdConsent.PERSONALISED)
        assertTrue(ads.adsAreServing.value)

        entitlement.isEntitled.value = true
        assertFalse(ads.adsAreServing.value)
        assertNull(ads.banner())
    }

    @Test
    fun losingEntitlementRestoresAds() = runTest {
        val entitlement = UnpurchasedEntitlement(entitled = true)
        val ads = makeManager(entitlement = entitlement)
        ads.start(AdConsent.PERSONALISED)
        assertFalse(ads.adsAreServing.value)

        entitlement.isEntitled.value = false
        assertTrue(ads.adsAreServing.value)
    }

    @Test
    fun revisitingConsentToDenyStopsAds() = runTest {
        val ads = makeManager()
        ads.start(AdConsent.PERSONALISED)
        ads.updateConsent(AdConsent.DENIED)
        assertFalse(ads.adsAreServing.value)
    }
}
