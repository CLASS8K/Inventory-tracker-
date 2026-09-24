package com.example.inventory.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.inventory.data.AuditLogEntry
import com.example.inventory.data.ImageStore
import com.example.inventory.data.InventoryItem
import com.example.inventory.data.InventoryRepository
import com.example.inventory.data.SessionManager
import com.example.inventory.data.StockLossReason
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

/** A just-saved stock take that can still be undone; cleared once the undo window closes. */
data class UndoableStockTake(val item: InventoryItem, val previousQuantity: Int, val auditEntryId: Long)

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

    /** Profit if everything currently on the shelf sold at its listed price. Admin-only figure. */
    val totalPotentialProfit: Double
        get() = items.sumOf { it.quantity * (it.unitPrice - it.costPrice) }
}

@HiltViewModel
class InventoryViewModel @Inject constructor(
    private val repository: InventoryRepository,
    private val userRepository: UserRepository,
    private val sessionManager: SessionManager,
    private val imageStore: ImageStore,
) : ViewModel() {

    private val searchQuery = MutableStateFlow("")
    private val sortOption = MutableStateFlow(SortOption.NAME)
    private val categoryFilter = MutableStateFlow(ALL_CATEGORIES)
    private val _lastStockTake = MutableStateFlow<UndoableStockTake?>(null)
    val lastStockTake: StateFlow<UndoableStockTake?> = _lastStockTake

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
        photoPath: String? = null,
        unit: String = InventoryItem.DEFAULT_UNIT,
        costPrice: Double = 0.0,
        supplierId: Long? = null,
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
                    photoPath = photoPath,
                    unit = unit,
                    costPrice = costPrice,
                    supplierId = supplierId,
                ),
                actorName = actor.name,
            )
        }
    }

    /** Reconciles a physical count: closing stock becomes the item's new quantity. */
    fun recordStockTake(
        item: InventoryItem,
        openingStock: Int,
        closingStock: Int,
        reason: StockLossReason = StockLossReason.SOLD,
    ) {
        val actor = sessionManager.currentUser.value ?: return
        viewModelScope.launch {
            val auditEntryId = repository.recordStockTake(item, openingStock, closingStock, actorName = actor.name, reason = reason)
            _lastStockTake.value = UndoableStockTake(item, previousQuantity = item.quantity, auditEntryId = auditEntryId)
        }
    }

    /** Reverts the most recent stock take, if the undo window hasn't been dismissed yet. */
    fun undoLastStockTake() {
        val undo = _lastStockTake.value ?: return
        _lastStockTake.value = null
        viewModelScope.launch {
            repository.undoStockTake(undo.item, undo.previousQuantity, undo.auditEntryId)
        }
    }

    /** Closes the undo window without reverting — called once the Snackbar times out or is swiped away. */
    fun dismissStockTakeUndo() {
        _lastStockTake.value = null
    }

    fun updateItem(previous: InventoryItem, updated: InventoryItem, receiptPath: String? = null) {
        val actor = sessionManager.currentUser.value ?: return
        viewModelScope.launch {
            repository.updateItem(previous, updated, actorName = actor.name, receiptPath = receiptPath)
            if (actor.role == UserRole.STOCK_KEEPER && updated.quantity > previous.quantity) {
                awardRestockXp(actor)
            }
            if (previous.photoPath != null && previous.photoPath != updated.photoPath) {
                imageStore.delete(previous.photoPath)
            }
        }
    }

    /** Copies a picked photo into app storage; returns the local path to persist, or null on failure. */
    fun importImage(uri: Uri, onResult: (String?) -> Unit) {
        viewModelScope.launch {
            onResult(imageStore.importImage(uri))
        }
    }

    fun deleteItem(item: InventoryItem) {
        val actor = sessionManager.currentUser.value ?: return
        viewModelScope.launch {
            repository.deleteItem(item, actorName = actor.name)
            imageStore.delete(item.photoPath)
        }
    }

    /** Quick + tap on an item card to restock; both roles may do this, and it earns XP. */
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

    /**
     * Quick − tap (or long-press for a bulk quantity) on an item card. Unlike [adjustQuantity],
     * this logs a real sale — revenue and admin-only profit computed immediately, same math as
     * a Stock Take's "Sold" branch — so the fast, per-drink tap a barman actually reaches for
     * produces real numbers instead of deferring everything to an end-of-shift reconciliation.
     * [quantity] defaults to 1 for the single tap.
     */
    fun sellUnits(item: InventoryItem, quantity: Int = 1) {
        val actor = sessionManager.currentUser.value ?: return
        viewModelScope.launch {
            repository.recordSale(item, quantity, actorName = actor.name)
        }
    }

    private suspend fun awardRestockXp(actor: UserProfile) {
        val updated = userRepository.awardXp(actor, UserProfile.XP_PER_RESTOCK)
        sessionManager.updateCurrentUser(updated)
    }
}
