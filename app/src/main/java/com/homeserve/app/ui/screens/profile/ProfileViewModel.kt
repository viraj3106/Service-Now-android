package com.homeserve.app.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.homeserve.app.billing.RevenueCatManager
import com.homeserve.app.data.model.User
import com.homeserve.app.data.repository.MockDataRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

sealed class ProfileUiState {
    object Idle : ProfileUiState()
    object Loading : ProfileUiState()
    data class Success(val message: String) : ProfileUiState()
    data class Error(val message: String) : ProfileUiState()
}

class ProfileViewModel(
    private val repository: MockDataRepository,
    private val billingManager: RevenueCatManager
) : ViewModel() {
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser = _currentUser.asStateFlow()

    private val _uiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Idle)
    val uiState = _uiState.asStateFlow()

    private val _logoutSuccess = MutableSharedFlow<Unit>()
    val logoutSuccess = _logoutSuccess.asSharedFlow()

    init {
        loadUser()
        syncBillingEntitlements()
    }

    private fun loadUser() {
        viewModelScope.launch {
            repository.getCurrentUser().collectLatest { user ->
                _currentUser.value = user
            }
        }
    }

    private fun syncBillingEntitlements() {
        viewModelScope.launch {
            // Keep billing manager and local repository in sync
            billingManager.syncEntitlementsWithRepository { updatedTier ->
                viewModelScope.launch {
                    repository.updateSubscription(updatedTier)
                }
            }
        }
    }

    fun restorePurchases() {
        viewModelScope.launch {
            _uiState.value = ProfileUiState.Loading
            try {
                val success = billingManager.restorePurchases()
                if (success) {
                    // Update entitlement states
                    billingManager.syncEntitlementsWithRepository { updatedTier ->
                        viewModelScope.launch {
                            repository.updateSubscription(updatedTier)
                        }
                    }
                    _uiState.value = ProfileUiState.Success("Purchases successfully restored!")
                } else {
                    _uiState.value = ProfileUiState.Error("No previous purchases found.")
                }
            } catch (e: Exception) {
                _uiState.value = ProfileUiState.Error(e.message ?: "Restoration failed")
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            repository.logout()
            _logoutSuccess.emit(Unit)
        }
    }

    fun resetState() {
        _uiState.value = ProfileUiState.Idle
    }

    fun forceIncrementUsage() {
        viewModelScope.launch {
            repository.forceIncrementBookings()
        }
    }
}
