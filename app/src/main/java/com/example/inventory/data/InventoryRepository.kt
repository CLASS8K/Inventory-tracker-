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
     * Reconciles a physical count against the running quantity. A closing count below opening is
     * a loss of some kind — [reason] says which; only [StockLossReason.SOLD] counts as revenue,
     * everything else (spillage, comps, theft) is logged as a cost with no revenue, so it never
     * inflates the numbers on a Stock Take that wasn't actually a sale. A closing count at or
     * above opening means stock was added without being logged as a restock, so it's noted as
     * such. Profit (or loss, for a non-sale reason) is stored as a structured field, never in
     * [AuditLogEntry.detail] — that text is shown to every role, and profit is admin-only.
     */
    suspend fun recordStockTake(
        item: InventoryItem,
        openingStock: Int,
        closingStock: Int,
        actorName: String,
        reason: StockLossReason = StockLossReason.SOLD,
    ) {
        val updated = item.copy(quantity = closingStock, lastUpdated = System.currentTimeMillis())
        inventoryDao.update(updated)

        val delta = closingStock - openingStock
        val unitLabel = item.unit.ifBlank { InventoryItem.DEFAULT_UNIT }
        val profit: Double?
        val detail: String
        if (delta < 0) {
            val quantityLost = -delta
            if (reason == StockLossReason.SOLD) {
                val amount = quantityLost * item.unitPrice
                profit = quantityLost * (item.unitPrice - item.costPrice)
                detail = "Opening $openingStock $unitLabel(s) → Closing $closingStock · Sold $quantityLost · ${formatMwk(amount)}"
            } else {
                profit = -(quantityLost * item.costPrice)
                detail = "Opening $openingStock $unitLabel(s) → Closing $closingStock · " +
                    "Lost $quantityLost to ${reason.label} — no sale recorded"
            }
        } else {
            profit = null
            detail = "Opening $openingStock $unitLabel(s) → Closing $closingStock · Stock increased by $delta, no sale recorded"
        }
        logAction(item.name, "Stock Take", detail, actorName, profit = profit)
    }

    private suspend fun logAction(
        itemName: String,
        action: String,
        detail: String,
        actorName: String,
        receiptPath: String? = null,
        profit: Double? = null,
    ) {
        auditLogDao.insert(
            AuditLogEntry(
                itemName = itemName,
                action = action,
                detail = detail,
                actorName = actorName,
                receiptPath = receiptPath,
                profit = profit,
            ),
        )
    }
}
