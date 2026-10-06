package com.xraiassistant.di

import android.content.Context
import com.xraiassistant.monetization.AdConsentStore
import com.xraiassistant.monetization.AdManager
import com.xraiassistant.monetization.AdMobProvider
import com.xraiassistant.monetization.BillingEntitlement
import com.xraiassistant.monetization.NoAdsProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

/**
 * Wires the monetization layer. Swapping ad networks is a change to the
 * provider built here plus one adapter file.
 */
@Module
@InstallIn(SingletonComponent::class)
object MonetizationModule {

    @Provides
    @Singleton
    fun provideBillingEntitlement(@ApplicationContext context: Context) = BillingEntitlement(context)

    @Provides
    @Singleton
    fun provideAdConsentStore(@ApplicationContext context: Context) = AdConsentStore(context)

    @Provides
    @Singleton
    fun provideAdManager(
        @ApplicationContext context: Context,
        entitlement: BillingEntitlement
    ) = AdManager(
        entitlement = entitlement,
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate),
        buildProvider = { serveAds -> if (serveAds) AdMobProvider(context) else NoAdsProvider }
    )
}
