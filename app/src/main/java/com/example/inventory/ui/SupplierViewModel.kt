package com.example.inventory.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.inventory.data.Supplier
import com.example.inventory.data.SupplierRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SupplierViewModel @Inject constructor(
    private val repository: SupplierRepository,
) : ViewModel() {

    val suppliers: StateFlow<List<Supplier>> = repository.suppliers.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    fun createSupplier(name: String, phone: String, notes: String, onResult: (Supplier) -> Unit) {
        viewModelScope.launch {
            onResult(repository.createSupplier(name, phone, notes))
        }
    }

    fun updateSupplier(supplier: Supplier) {
        viewModelScope.launch {
            repository.updateSupplier(supplier)
        }
    }

    fun deleteSupplier(supplier: Supplier) {
        viewModelScope.launch {
            repository.deleteSupplier(supplier)
        }
    }
}
