package com.example.inventory.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.inventory.data.AuditLogEntry
import com.example.inventory.data.InventoryItem
import com.example.inventory.data.InventoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class InventoryUiState(
    val items: List<InventoryItem> = emptyList(),
    val searchQuery: String = "",
    val auditLog: List<AuditLogEntry> = emptyList(),
) {
    val filteredItems: List<InventoryItem>
        get() = if (searchQuery.isBlank()) {
            items
        } else {
            items.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                    it.sku.contains(searchQuery, ignoreCase = true) ||
                    it.category.contains(searchQuery, ignoreCase = true)
            }
        }

    val lowStockItems: List<InventoryItem>
        get() = items.filter { it.isLowStock }
}

@HiltViewModel
class InventoryViewModel @Inject constructor(
    private val repository: InventoryRepository,
) : ViewModel() {

    private val searchQuery = MutableStateFlow("")

    val uiState: StateFlow<InventoryUiState> = combine(
        repository.items,
        searchQuery,
        repository.auditLog,
    ) { items, query, auditLog ->
        InventoryUiState(items = items, searchQuery = query, auditLog = auditLog)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = InventoryUiState(),
    )

    fun onSearchQueryChange(query: String) {
        searchQuery.value = query
    }

    fun addItem(
        name: String,
        sku: String,
        category: String,
        quantity: Int,
        lowStockThreshold: Int,
        unitPrice: Double,
    ) {
        viewModelScope.launch {
            repository.addItem(
                InventoryItem(
                    name = name,
                    sku = sku,
                    category = category,
                    quantity = quantity,
                    lowStockThreshold = lowStockThreshold,
                    unitPrice = unitPrice,
                )
            )
        }
    }

    fun updateItem(previous: InventoryItem, updated: InventoryItem) {
        viewModelScope.launch {
            repository.updateItem(previous, updated)
        }
    }

    fun deleteItem(item: InventoryItem) {
        viewModelScope.launch {
            repository.deleteItem(item)
        }
    }
}
