package com.example.inventory.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.inventory.data.AuditLogEntry
import com.example.inventory.data.InventoryItem
import com.example.inventory.data.InventoryRepository
import com.example.inventory.data.SessionManager
import com.example.inventory.data.UserProfile
import com.example.inventory.data.UserRepository
import com.example.inventory.data.UserRole
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class SortOption(val label: String) {
    NAME("Name (A-Z)"),
    QUANTITY_LOW_HIGH("Quantity (Low-High)"),
    QUANTITY_HIGH_LOW("Quantity (High-Low)"),
    PRICE_HIGH_LOW("Unit Price (High-Low)"),
    RECENTLY_UPDATED("Recently Updated"),
}

const val ALL_CATEGORIES = "All Items"

data class InventoryUiState(
    val items: List<InventoryItem> = emptyList(),
    val searchQuery: String = "",
    val auditLog: List<AuditLogEntry> = emptyList(),
    val sortOption: SortOption = SortOption.NAME,
    val categoryFilter: String = ALL_CATEGORIES,
    val currentUser: UserProfile? = null,
) {
    val isAdmin: Boolean get() = currentUser?.role == UserRole.ADMIN

    val categories: List<String>
        get() = listOf(ALL_CATEGORIES) + items.map { it.category }.distinct().sorted()

    val filteredItems: List<InventoryItem>
        get() {
            var result = items
            if (searchQuery.isNotBlank()) {
                result = result.filter {
                    it.name.contains(searchQuery, ignoreCase = true) ||
                        it.sku.contains(searchQuery, ignoreCase = true) ||
                        it.category.contains(searchQuery, ignoreCase = true)
                }
            }
            if (categoryFilter != ALL_CATEGORIES) {
                result = result.filter { it.category == categoryFilter }
            }
            return when (sortOption) {
                SortOption.NAME -> result.sortedBy { it.name.lowercase() }
                SortOption.QUANTITY_LOW_HIGH -> result.sortedBy { it.quantity }
                SortOption.QUANTITY_HIGH_LOW -> result.sortedByDescending { it.quantity }
                SortOption.PRICE_HIGH_LOW -> result.sortedByDescending { it.unitPrice }
                SortOption.RECENTLY_UPDATED -> result.sortedByDescending { it.lastUpdated }
            }
        }

    val lowStockItems: List<InventoryItem>
        get() = items.filter { it.isLowStock }

    val outOfStockCount: Int get() = items.count { it.quantity == 0 }
    val totalValue: Double get() = items.sumOf { it.quantity * it.unitPrice }
    val totalUnits: Int get() = items.sumOf { it.quantity }
}

@HiltViewModel
class InventoryViewModel @Inject constructor(
    private val repository: InventoryRepository,
    private val userRepository: UserRepository,
    private val sessionManager: SessionManager,
) : ViewModel() {

    private val searchQuery = MutableStateFlow("")
    private val sortOption = MutableStateFlow(SortOption.NAME)
    private val categoryFilter = MutableStateFlow(ALL_CATEGORIES)

    private val itemsAndLog = combine(
        repository.items,
        searchQuery,
        repository.auditLog,
    ) { items, query, auditLog -> Triple(items, query, auditLog) }

    private val filters = combine(
        sortOption,
        categoryFilter,
        sessionManager.currentUser,
    ) { sort, category, user -> Triple(sort, category, user) }

    val uiState: StateFlow<InventoryUiState> = combine(
        itemsAndLog,
        filters,
    ) { (items, query, auditLog), (sort, category, user) ->
        InventoryUiState(
            items = items,
            searchQuery = query,
            auditLog = auditLog,
            sortOption = sort,
            categoryFilter = category,
            currentUser = user,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = InventoryUiState(),
    )

    fun onSearchQueryChange(query: String) {
        searchQuery.value = query
    }

    fun onSortOptionChange(option: SortOption) {
        sortOption.value = option
    }

    fun onCategoryFilterChange(category: String) {
        categoryFilter.value = category
    }

    fun addItem(
        name: String,
        sku: String,
        category: String,
        quantity: Int,
        lowStockThreshold: Int,
        unitPrice: Double,
    ) {
        val actor = sessionManager.currentUser.value ?: return
        viewModelScope.launch {
            repository.addItem(
                InventoryItem(
                    name = name,
                    sku = sku,
                    category = category,
                    quantity = quantity,
                    lowStockThreshold = lowStockThreshold,
                    unitPrice = unitPrice,
                ),
                actorName = actor.name,
            )
        }
    }

    fun updateItem(previous: InventoryItem, updated: InventoryItem) {
        val actor = sessionManager.currentUser.value ?: return
        viewModelScope.launch {
            repository.updateItem(previous, updated, actorName = actor.name)
            if (actor.role == UserRole.STOCK_KEEPER && updated.quantity > previous.quantity) {
                awardRestockXp(actor)
            }
        }
    }

    fun deleteItem(item: InventoryItem) {
        val actor = sessionManager.currentUser.value ?: return
        viewModelScope.launch {
            repository.deleteItem(item, actorName = actor.name)
        }
    }

    /** Quick +/- tap on an item card; both roles may restock, only positive deltas earn XP. */
    fun adjustQuantity(item: InventoryItem, delta: Int) {
        val actor = sessionManager.currentUser.value ?: return
        val newQuantity = (item.quantity + delta).coerceAtLeast(0)
        if (newQuantity == item.quantity) return
        viewModelScope.launch {
            repository.updateItem(
                previous = item,
                updated = item.copy(quantity = newQuantity, lastUpdated = System.currentTimeMillis()),
                actorName = actor.name,
            )
            if (delta > 0) {
                awardRestockXp(actor)
            }
        }
    }

    private suspend fun awardRestockXp(actor: UserProfile) {
        val updated = userRepository.awardXp(actor, UserProfile.XP_PER_RESTOCK)
        sessionManager.updateCurrentUser(updated)
    }
}
