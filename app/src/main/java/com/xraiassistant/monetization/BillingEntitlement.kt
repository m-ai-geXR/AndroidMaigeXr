package com.xraiassistant.monetization

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.BillingClient.ProductType
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.xraiassistant.config.AppConfig
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * The real entitlement, from Play Billing. Mirrors iOS StoreEntitlement.swift.
 *
 * One non-consumable: removing ads. No subscription, no feature gates.
 *
 * Verification is on-device only. There is no backend to check purchase tokens
 * against the Play Developer API, so a rooted device can spoof ownership. That is
 * accepted for v1 because the purchase unlocks nothing but ad removal; server
 * verification is the fix if that ever changes.
 */
class BillingEntitlement(context: Context) : EntitlementSource {

    companion object {
        /** Must match the in-app product in Play Console exactly. Same as iOS. */
        const val REMOVE_ADS_PRODUCT_ID = "studio.seacloud9.maigexr.removeads"
        private const val PREFS = "maigexr_billing"
        private const val CACHE_KEY = "removeAds.entitled"
    }

    sealed interface State {
        data object Idle : State
        data object LoadingProduct : State
        data object Purchasing : State
        data object Restoring : State
        /**
         * A payment awaiting approval (cash, family approval). Not a failure: it
         * may complete much later and arrives through the purchases listener.
         */
        data object Pending : State
        data class Failed(val message: String) : State
    }

    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    // Cached so the first frame and offline launches are right. Never
    // authoritative: Play overrules this the moment it answers.
    private val _isEntitled = MutableStateFlow(
        AppConfig.forcePremiumMode || prefs.getBoolean(CACHE_KEY, false)
    )
    override val isEntitled: StateFlow<Boolean> = _isEntitled.asStateFlow()

    /** The product, once Play has described it. The price must come from here. */
    private val _product = MutableStateFlow<ProductDetails?>(null)
    val product: StateFlow<ProductDetails?> = _product.asStateFlow()

    private val _state = MutableStateFlow<State>(State.Idle)
    val state: StateFlow<State> = _state.asStateFlow()

    /** The formatted local price, or null until the product has loaded. */
    val displayPrice: String?
        get() = _product.value?.oneTimePurchaseOfferDetails?.formattedPrice

    private val client: BillingClient = BillingClient.newBuilder(context.applicationContext)
        .setListener { result, purchases -> onPurchasesUpdated(result, purchases) }
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .enableAutoServiceReconnection()
        .build()

    private var connected: CompletableDeferred<Boolean>? = null

    // ---------------------------------------------------------------------
    // Lifecycle
    // ---------------------------------------------------------------------

    /** Call once at launch. Loads the product and reconciles the cache. */
    suspend fun start() {
        if (!connect()) return
        _state.value = State.LoadingProduct
        loadProduct()
        refreshPurchases()
        if (_state.value == State.LoadingProduct) _state.value = State.Idle
    }

    private suspend fun connect(): Boolean {
        if (client.isReady) return true
        connected?.let { return it.await() }

        val deferred = CompletableDeferred<Boolean>()
        connected = deferred
        client.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                val ok = result.responseCode == BillingResponseCode.OK
                if (!ok) log("setup failed: ${result.debugMessage}")
                deferred.complete(ok)
                connected = null
            }

            override fun onBillingServiceDisconnected() {
                // Auto reconnection handles later calls; this only unblocks a
                // connect() still waiting on the first answer.
                deferred.complete(false)
                connected = null
            }
        })
        return deferred.await()
    }

    private suspend fun loadProduct() {
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(REMOVE_ADS_PRODUCT_ID)
                        .setProductType(ProductType.INAPP)
                        .build()
                )
            )
            .build()

        _product.value = suspendCancellableCoroutine { cont ->
            client.queryProductDetailsAsync(params) { result, details ->
                if (result.responseCode != BillingResponseCode.OK) {
                    log("product query failed: ${result.debugMessage}")
                }
                cont.resume(details.productDetailsList.firstOrNull())
            }
        }
    }

    /**
     * Ask Play what the user owns. A failed query leaves the cache alone rather
     * than revoking: offline is not the same as refunded.
     */
    private suspend fun refreshPurchases() {
        val params = QueryPurchasesParams.newBuilder().setProductType(ProductType.INAPP).build()
        val purchases = suspendCancellableCoroutine<List<Purchase>?> { cont ->
            client.queryPurchasesAsync(params) { result, list ->
                cont.resume(if (result.responseCode == BillingResponseCode.OK) list else null)
            }
        } ?: return

        handle(purchases, fromQuery = true)
    }

    // ---------------------------------------------------------------------
    // Purchase and restore
    // ---------------------------------------------------------------------

    fun purchase(activity: Activity) {
        val details = _product.value
        if (details == null) {
            _state.value = State.Failed("Remove Ads is not available right now. Try again later.")
            return
        }
        _state.value = State.Purchasing
        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(details).build())
            )
            .build()
        val result = client.launchBillingFlow(activity, params)
        if (result.responseCode != BillingResponseCode.OK) {
            _state.value = failure(result)
        }
    }

    /**
     * Re-query ownership. Play syncs purchases across devices on its own, so this
     * is the same check launch does, made visible for a user who wants to press it.
     */
    suspend fun restore() {
        _state.value = State.Restoring
        if (!connect()) {
            _state.value = State.Failed("Could not reach Google Play. Check your connection.")
            return
        }
        refreshPurchases()
        if (_state.value == State.Restoring) {
            _state.value = if (_isEntitled.value) State.Idle
            else State.Failed("No previous Remove Ads purchase was found for this Google account.")
        }
    }

    fun clearStatus() {
        if (_state.value is State.Failed || _state.value == State.Pending) _state.value = State.Idle
    }

    private fun onPurchasesUpdated(result: BillingResult, purchases: List<Purchase>?) {
        when (result.responseCode) {
            BillingResponseCode.OK -> handle(purchases.orEmpty(), fromQuery = false)
            BillingResponseCode.USER_CANCELED -> _state.value = State.Idle
            BillingResponseCode.ITEM_ALREADY_OWNED -> {
                setEntitled(true)
                _state.value = State.Idle
            }
            else -> _state.value = failure(result)
        }
    }

    private fun handle(purchases: List<Purchase>, fromQuery: Boolean) {
        val ours = purchases.filter { REMOVE_ADS_PRODUCT_ID in it.products }
        val owned = ours.firstOrNull { it.purchaseState == Purchase.PurchaseState.PURCHASED }

        when {
            owned != null -> {
                setEntitled(true)
                if (!owned.isAcknowledged) acknowledge(owned)
                _state.value = State.Idle
            }
            ours.any { it.purchaseState == Purchase.PurchaseState.PENDING } -> {
                _state.value = State.Pending
            }
            // Only a full ownership query can revoke. A purchases-updated event
            // without our product says nothing about what the user already owns.
            fromQuery -> setEntitled(AppConfig.forcePremiumMode)
        }
    }

    /**
     * Play refunds a purchase that is not acknowledged within three days, so
     * this is not optional. A failure here is retried on the next launch, where
     * the query sees the purchase unacknowledged again.
     */
    private fun acknowledge(purchase: Purchase) {
        val params = AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchase.purchaseToken).build()
        client.acknowledgePurchase(params) { result ->
            if (result.responseCode != BillingResponseCode.OK) log("acknowledge failed: ${result.debugMessage}")
        }
    }

    private fun setEntitled(entitled: Boolean) {
        prefs.edit().putBoolean(CACHE_KEY, entitled).apply()
        _isEntitled.value = entitled
    }

    private fun failure(result: BillingResult): State.Failed = State.Failed(
        when (result.responseCode) {
            BillingResponseCode.SERVICE_UNAVAILABLE,
            BillingResponseCode.SERVICE_DISCONNECTED,
            BillingResponseCode.NETWORK_ERROR -> "Could not reach Google Play. Check your connection."
            BillingResponseCode.BILLING_UNAVAILABLE -> "Purchases are not available on this device or account."
            else -> "The purchase did not complete. Please try again."
        }.also { log("billing ${result.responseCode}: ${result.debugMessage}") }
    )

    private fun log(message: String) {
        if (AppConfig.showAdDebugLogs) Log.d("Billing", message)
    }
}
