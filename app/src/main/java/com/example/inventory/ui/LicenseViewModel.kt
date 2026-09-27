package com.example.inventory.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.inventory.data.LicenseChecker
import com.example.inventory.data.LicenseStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LicenseViewModel @Inject constructor(
    private val licenseChecker: LicenseChecker,
) : ViewModel() {

    private val _status = MutableStateFlow(licenseChecker.currentStatus())
    val status: StateFlow<LicenseStatus> = _status.asStateFlow()

    private val _daysUntilDue = MutableStateFlow(licenseChecker.daysUntilDue())
    val daysUntilDue: StateFlow<Long> = _daysUntilDue.asStateFlow()

    private val _businessName = MutableStateFlow(licenseChecker.businessName())
    val businessName: StateFlow<String?> = _businessName.asStateFlow()

    /** Non-blocking heads-up when other devices already share this name — null when there's nothing to flag. */
    private val _businessNameWarning = MutableStateFlow<String?>(null)
    val businessNameWarning: StateFlow<String?> = _businessNameWarning.asStateFlow()

    init {
        refresh()
    }

    /** Re-fetches from Firestore if reachable, then recomputes status from the (possibly newly refreshed) cache. */
    fun refresh() {
        viewModelScope.launch {
            licenseChecker.refresh()
            _status.value = licenseChecker.currentStatus()
            _daysUntilDue.value = licenseChecker.daysUntilDue()
        }
    }

    /** Saves the bar/club name entered at setup and immediately pushes it to Firestore. */
    fun setBusinessName(name: String) {
        licenseChecker.setBusinessName(name)
        _businessName.value = name
        _businessNameWarning.value = null
        viewModelScope.launch {
            refresh()
            val otherCount = licenseChecker.countOtherDevicesWithBusinessName(name)
            if (otherCount != null && otherCount > 0) {
                _businessNameWarning.value =
                    "$otherCount other device${if (otherCount == 1) " is" else "s are"} already registered as \"$name\". " +
                        "Fine if that's this same bar's other tablets/phones — worth a second look in Firebase if not."
            }
        }
    }

    fun dismissBusinessNameWarning() {
        _businessNameWarning.value = null
    }
}
