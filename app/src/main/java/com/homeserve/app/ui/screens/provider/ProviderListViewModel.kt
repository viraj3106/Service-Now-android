package com.homeserve.app.ui.screens.provider

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.homeserve.app.data.model.ServiceProvider
import com.homeserve.app.data.repository.MockDataRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ProviderListViewModel(
    private val repository: MockDataRepository,
    val categoryId: String,
    val city: String
) : ViewModel() {
    private val _providers = MutableStateFlow<List<ServiceProvider>>(emptyList())
    val providers = _providers.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    init {
        loadProviders()
    }

    fun loadProviders() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                repository.getProviders(categoryId, city).collectLatest { list ->
                    _providers.value = list
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Failed to load providers"
                _isLoading.value = false
            }
        }
    }
}
