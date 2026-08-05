package com.homeserve.app.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.homeserve.app.data.model.ServiceCategory
import com.homeserve.app.data.model.User
import com.homeserve.app.data.repository.MockDataRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class HomeViewModel(private val repository: MockDataRepository) : ViewModel() {
    private val _categories = MutableStateFlow<List<ServiceCategory>>(emptyList())
    val categories = _categories.asStateFlow()

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser = _currentUser.asStateFlow()

    private val _selectedCity = MutableStateFlow("Chennai")
    val selectedCity = _selectedCity.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading = _isLoading.asStateFlow()

    val cities = listOf("Chennai", "Coimbatore", "Madurai", "Trichy")

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            _isLoading.value = true
            // Load user profile
            launch {
                repository.getCurrentUser().collectLatest { user ->
                    _currentUser.value = user
                }
            }
            // Load categories
            launch {
                repository.getCategories().collectLatest { list ->
                    _categories.value = list
                    _isLoading.value = false
                }
            }
        }
    }

    fun onCitySelect(city: String) {
        _selectedCity.value = city
    }
}
