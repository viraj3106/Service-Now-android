package com.homeserve.app.ui.screens.paywall

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.homeserve.app.billing.MockPackage
import com.homeserve.app.billing.RevenueCatManager
import com.homeserve.app.data.repository.MockDataRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class PaywallUiState {
    object Idle : PaywallUiState()
    object Loading : PaywallUiState()
    object Success : PaywallUiState()
    data class Error(val message: String) : PaywallUiState()
}

class PaywallViewModel(
    private val repository: MockDataRepository,
    private val billingManager: RevenueCatManager
) : ViewModel() {
    private val _packages = MutableStateFlow<List<MockPackage>>(emptyList())
    val packages = _packages.asStateFlow()

    private val _uiState = MutableStateFlow<PaywallUiState>(PaywallUiState.Idle)
    val uiState = _uiState.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading = _isLoading.asStateFlow()

    init {
        loadOfferings()
    }

    fun loadOfferings() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val list = billingManager.getOfferingsList()
                _packages.value = list
            } catch (e: Exception) {
                // If it fails, fallback to defaults
                _packages.value = listOf(
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
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun purchasePackage(activity: Activity, pkg: MockPackage) {
        viewModelScope.launch {
            _uiState.value = PaywallUiState.Loading
            try {
                val success = billingManager.purchasePackage(activity, pkg)
                if (success) {
                    val newTier = if (pkg.entitlementId == "elite_user") "elite" else "premium"
                    repository.updateSubscription(newTier)
                    _uiState.value = PaywallUiState.Success
                } else {
                    _uiState.value = PaywallUiState.Error("Purchase failed or was cancelled")
                }
            } catch (e: Exception) {
                _uiState.value = PaywallUiState.Error(e.message ?: "Purchase error occurred")
            }
        }
    }

    fun resetState() {
        _uiState.value = PaywallUiState.Idle
    }
}
