package com.homeserve.app.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.revenuecat.purchases.*
import com.revenuecat.purchases.interfaces.ReceiveOfferingsListener
import com.revenuecat.purchases.models.Period
import com.revenuecat.purchases.models.Price
import com.revenuecat.purchases.models.PricingPhase
import com.revenuecat.purchases.models.RecurrenceMode
import com.revenuecat.purchases.models.StoreProduct
import com.revenuecat.purchases.models.SubscriptionOptions
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

class RevenueCatManager(private val context: Context) {
    private val TAG = "RevenueCatManager"
    private var isSimulated = false

    private val _premiumActive = MutableStateFlow(false)
    val premiumActive = _premiumActive.asStateFlow()

    private val _eliteActive = MutableStateFlow(false)
    val eliteActive = _eliteActive.asStateFlow()

    fun initialize(apiKey: String) {
        if (apiKey == "goog_placeholder_api_key_homeserve" || apiKey.isBlank()) {
            Log.d(TAG, "Using Simulated Billing Mode (Placeholder Key)")
            isSimulated = true
        } else {
            try {
                Log.d(TAG, "Configuring RevenueCat with real API key: $apiKey")
                Purchases.configure(
                    PurchasesConfiguration.Builder(context, apiKey).build()
                )
                // Listen to customer info changes
                Purchases.sharedInstance.updatedCustomerInfoListener = UpdatedCustomerInfoListener { customerInfo ->
                    updateEntitlementStates(customerInfo)
                }
                isSimulated = false
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize RevenueCat, falling back to simulated billing: ${e.message}")
                isSimulated = true
            }
        }
    }

    private fun updateEntitlementStates(customerInfo: CustomerInfo) {
        _premiumActive.value = customerInfo.entitlements["premium_user"]?.isActive == true
        _eliteActive.value = customerInfo.entitlements["elite_user"]?.isActive == true
    }

    suspend fun getOfferingsList(): List<MockPackage> {
        if (isSimulated) {
            delay(500) // Mock API latency
            return listOf(
                MockPackage(
                    identifier = "premium_monthly",
                    title = "Premium Tier",
                    priceString = "₹399/mo",
                    description = "Book up to 5 times per month",
                    entitlementId = "premium_user"
                ),
                MockPackage(
                    identifier = "elite_monthly",
                    title = "Elite Tier",
                    priceString = "₹799/mo",
                    description = "Unlimited monthly bookings",
                    entitlementId = "elite_user"
                )
            )
        }

        return suspendCancellableCoroutine { continuation ->
            Purchases.sharedInstance.getOfferings(object : ReceiveOfferingsListener {
                override fun onReceived(offerings: Offerings) {
                    val currentOffering = offerings.current
                    val list = mutableListOf<MockPackage>()
                    currentOffering?.availablePackages?.forEach { pkg ->
                        list.add(
                            MockPackage(
                                identifier = pkg.identifier,
                                title = pkg.product.title,
                                priceString = pkg.product.price.formatted,
                                description = pkg.product.description,
                                entitlementId = if (pkg.identifier.contains("elite")) "elite_user" else "premium_user",
                                realPackage = pkg
                            )
                        )
                    }
                    continuation.resume(list)
                }

                override fun onError(error: PurchasesError) {
                    continuation.resumeWithException(Exception(error.message))
                }
            })
        }
    }

    suspend fun purchasePackage(activity: Activity, mockPackage: MockPackage): Boolean {
        if (isSimulated) {
            delay(1000) // Mock transaction delay
            if (mockPackage.entitlementId == "elite_user") {
                _eliteActive.value = true
                _premiumActive.value = false
            } else {
                _premiumActive.value = true
                _eliteActive.value = false
            }
            return true
        }

        val realPkg = mockPackage.realPackage ?: return false
        return suspendCancellableCoroutine { continuation ->
            Purchases.sharedInstance.purchase(
                PurchaseParams.Builder(activity, realPkg).build(),
                onError = { error, userCancelled ->
                    continuation.resume(false)
                },
                onSuccess = { _, customerInfo ->
                    updateEntitlementStates(customerInfo)
                    continuation.resume(true)
                }
            )
        }
    }

    suspend fun restorePurchases(): Boolean {
        if (isSimulated) {
            delay(800)
            // Simulated restore: upgrade user to Premium
            _premiumActive.value = true
            return true
        }

        return suspendCancellableCoroutine { continuation ->
            Purchases.sharedInstance.restorePurchases(
                onError = {
                    continuation.resume(false)
                },
                onSuccess = { customerInfo ->
                    updateEntitlementStates(customerInfo)
                    continuation.resume(true)
                }
            )
        }
    }

    fun syncEntitlementsWithRepository(onSync: (String) -> Unit) {
        // Sync active level: Elite takes priority over Premium
        if (_eliteActive.value) {
            onSync("elite")
        } else if (_premiumActive.value) {
            onSync("premium")
        } else {
            onSync("free")
        }
    }

    // Refresh entitlement status from cache
    fun checkCachedEntitlements() {
        if (isSimulated) return
        try {
            Purchases.sharedInstance.getCustomerInfo(
                onError = { /* ignore cache load errors */ },
                onSuccess = { customerInfo ->
                    updateEntitlementStates(customerInfo)
                }
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

// Common wrapper model that works with both mock packages and real RevenueCat Packages
data class MockPackage(
    val identifier: String,
    val title: String,
    val priceString: String,
    val description: String,
    val entitlementId: String,
    val realPackage: Package? = null
)
