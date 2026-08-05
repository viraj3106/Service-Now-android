package com.homeserve.app.ui.screens.provider

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.homeserve.app.data.model.ServiceProvider
import com.homeserve.app.data.repository.MockDataRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ProviderDetailViewModel(
    private val repository: MockDataRepository,
    val providerId: String
) : ViewModel() {
    private val _provider = MutableStateFlow<ServiceProvider?>(null)
    val provider = _provider.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading = _isLoading.asStateFlow()

    init {
        loadProvider()
    }

    fun loadProvider() {
        viewModelScope.launch {
            _isLoading.value = true
            repository.getProviderById(providerId).collectLatest { p ->
                _provider.value = p
                _isLoading.value = false
            }
        }
    }
}
