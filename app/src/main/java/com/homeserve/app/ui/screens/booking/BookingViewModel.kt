package com.homeserve.app.ui.screens.booking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.homeserve.app.data.model.ServiceProvider
import com.homeserve.app.data.repository.MockDataRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

sealed class BookingUiState {
    object Idle : BookingUiState()
    object Loading : BookingUiState()
    object Success : BookingUiState()
    data class Error(val message: String) : BookingUiState()
    object GatedRedirect : BookingUiState()
}

class BookingViewModel(
    private val repository: MockDataRepository,
    val providerId: String
) : ViewModel() {
    private val _provider = MutableStateFlow<ServiceProvider?>(null)
    val provider = _provider.asStateFlow()

    private val _uiState = MutableStateFlow<BookingUiState>(BookingUiState.Idle)
    val uiState = _uiState.asStateFlow()

    private val _date = MutableStateFlow("")
    val date = _date.asStateFlow()

    private val _timeSlot = MutableStateFlow("")
    val timeSlot = _timeSlot.asStateFlow()

    private val _notes = MutableStateFlow("")
    val notes = _notes.asStateFlow()

    val timeSlots = listOf("09:00 AM - 11:00 AM", "12:00 PM - 02:00 PM", "03:00 PM - 05:00 PM", "06:00 PM - 08:00 PM")

    init {
        loadProvider()
    }

    private fun loadProvider() {
        viewModelScope.launch {
            repository.getProviderById(providerId).collectLatest { p ->
                _provider.value = p
            }
        }
    }

    fun onDateChange(value: String) {
        _date.value = value
    }

    fun onTimeSlotSelect(value: String) {
        _timeSlot.value = value
    }

    fun onNotesChange(value: String) {
        _notes.value = value
    }

    fun submitBooking() {
        if (_date.value.trim().isEmpty()) {
            _uiState.value = BookingUiState.Error("Please select a date")
            return
        }
        if (_timeSlot.value.isEmpty()) {
            _uiState.value = BookingUiState.Error("Please select a time slot")
            return
        }

        viewModelScope.launch {
            _uiState.value = BookingUiState.Loading
            val serviceName = _provider.value?.category?.replace("_", " ")?.replaceFirstChar { it.uppercase() } ?: "Home Service"
            val result = repository.createBooking(
                providerId = providerId,
                service = serviceName,
                date = _date.value,
                timeSlot = _timeSlot.value,
                notes = _notes.value
            )

            result.fold(
                onSuccess = {
                    _uiState.value = BookingUiState.Success
                },
                onFailure = { throwable ->
                    if (throwable.message == "GATED_LIMIT_EXCEEDED") {
                        _uiState.value = BookingUiState.GatedRedirect
                    } else {
                        _uiState.value = BookingUiState.Error(throwable.message ?: "Booking failed")
                    }
                }
            )
        }
    }

    fun resetState() {
        _uiState.value = BookingUiState.Idle
    }
}
