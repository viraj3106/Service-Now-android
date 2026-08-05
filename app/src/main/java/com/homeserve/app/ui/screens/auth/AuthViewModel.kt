package com.homeserve.app.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.homeserve.app.data.repository.MockDataRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel(private val repository: MockDataRepository) : ViewModel() {
    private val _name = MutableStateFlow("")
    val name = _name.asStateFlow()

    private val _phone = MutableStateFlow("")
    val phone = _phone.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _loginSuccess = MutableSharedFlow<Unit>()
    val loginSuccess = _loginSuccess.asSharedFlow()

    fun onNameChange(value: String) {
        _name.value = value
        _error.value = null
    }

    fun onPhoneChange(value: String) {
        _phone.value = value
        _error.value = null
    }

    fun onSubmit() {
        if (_name.value.trim().isEmpty()) {
            _error.value = "Please enter your name"
            return
        }
        if (_phone.value.trim().isEmpty() || _phone.value.length < 10) {
            _error.value = "Please enter a valid 10-digit phone number"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.login(_name.value, _phone.value)
                _loginSuccess.emit(Unit)
            } catch (e: Exception) {
                _error.value = e.message ?: "Authentication failed"
            } finally {
                _isLoading.value = false
            }
        }
    }
}
