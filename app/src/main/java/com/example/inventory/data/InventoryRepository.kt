package com.example.inventory.data

import com.example.inventory.util.formatMwk
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InventoryRepository @Inject constructor(
    private val inventoryDao: InventoryDao,
    private val auditLogDao: AuditLogDao,
) {
    val items: Flow<List<InventoryItem>> = inventoryDao.observeAll()
    val auditLog: Flow<List<AuditLogEntry>> = auditLogDao.observeAll()

    suspend fun getItem(id: Long): InventoryItem? = inventoryDao.getById(id)

    suspend fun addItem(item: InventoryItem, actorName: String) {
        inventoryDao.upsert(item)
        logAction(item.name, "Created", "Added with quantity ${item.quantity}", actorName)
    }

    suspend fun updateItem(
        previous: InventoryItem,
        updated: InventoryItem,
        actorName: String,
        receiptPath: String? = null,
    ) {
        inventoryDao.update(updated)
        val detail = if (previous.quantity != updated.quantity) {
            val delta = updated.quantity - previous.quantity
            val sign = if (delta > 0) "+" else ""
            "Quantity changed from ${previous.quantity} to ${updated.quantity} ($sign$delta)"
        } else {
            "Details updated"
        }
        logAction(updated.name, "Updated", detail, actorName, receiptPath)
    }

    suspend fun deleteItem(item: InventoryItem, actorName: String) {
        inventoryDao.delete(item)
        logAction(item.name, "Deleted", "Removed from inventory", actorName)
    }

    /**
     * Reconciles a physical count against the running quantity. A closing count below opening
     * is treated as sales (qty sold * unit price = amount); a closing count at or above opening
     * means stock was added without being logged as a restock, so it's noted as such rather than
     * reported as zero-value sales.
     */
    suspend fun recordStockTake(
        item: InventoryItem,
        openingStock: Int,
        closingStock: Int,
        actorName: String,
    ) {
        val updated = item.copy(quantity = closingStock, lastUpdated = System.currentTimeMillis())
        inventoryDao.update(updated)

        val delta = closingStock - openingStock
        val unitLabel = item.unit.ifBlank { InventoryItem.DEFAULT_UNIT }
        val detail = if (delta < 0) {
            val quantitySold = -delta
            val amount = quantitySold * item.unitPrice
            "Opening $openingStock $unitLabel(s) → Closing $closingStock · Sold $quantitySold · ${formatMwk(amount)}"
        } else {
            "Opening $openingStock $unitLabel(s) → Closing $closingStock · Stock increased by $delta, no sale recorded"
        }
        logAction(item.name, "Stock Take", detail, actorName)
    }

    private suspend fun logAction(
        itemName: String,
        action: String,
        detail: String,
        actorName: String,
        receiptPath: String? = null,
    ) {
        auditLogDao.insert(
            AuditLogEntry(
                itemName = itemName,
                action = action,
                detail = detail,
                actorName = actorName,
                receiptPath = receiptPath,
            ),
        )
    }
}
