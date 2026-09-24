package com.example.inventory.data

import com.example.inventory.util.formatMwk
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InventoryRepository @Inject constructor(
    private val inventoryDao: InventoryDao,
    private val auditLogDao: AuditLogDao,
    private val lowStockNotifier: LowStockNotifier,
) {
    val items: Flow<List<InventoryItem>> = inventoryDao.observeAll()
    val auditLog: Flow<List<AuditLogEntry>> = auditLogDao.observeAll()

    suspend fun getItem(id: Long): InventoryItem? = inventoryDao.getById(id)

    suspend fun addItem(item: InventoryItem, actorName: String, receiptPath: String? = null) {
        inventoryDao.upsert(item)
        logAction(item.name, "Created", "Added with quantity ${item.quantity}", actorName, receiptPath)
        if (item.isLowStock) lowStockNotifier.notifyLowStock(item)
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
        // Edge-triggered: only fires the moment it crosses the threshold, not on every subsequent
        // adjustment while it stays low — otherwise every -1 tap while low would re-notify.
        if (!previous.isLowStock && updated.isLowStock) lowStockNotifier.notifyLowStock(updated)
    }

    suspend fun deleteItem(item: InventoryItem, actorName: String) {
        inventoryDao.delete(item)
        logAction(item.name, "Deleted", "Removed from inventory", actorName)
        lowStockNotifier.cancelLowStock(item.id)
    }

    /**
     * Records a sale directly from the quick −1 tap (or a bulk sell): decrements quantity and
     * logs revenue + admin-only profit immediately, the same math as a Stock Take's "Sold"
     * branch — without waiting for an end-of-shift reconciliation to know what actually sold.
     * Clamps to what's actually on hand rather than going negative.
     */
    suspend fun recordSale(item: InventoryItem, quantitySold: Int, actorName: String) {
        val actualSold = quantitySold.coerceIn(0, item.quantity)
        if (actualSold == 0) return
        val updated = item.copy(quantity = item.quantity - actualSold, lastUpdated = System.currentTimeMillis())
        inventoryDao.update(updated)
        val unitLabel = item.unit.ifBlank { InventoryItem.DEFAULT_UNIT }
        val amount = actualSold * item.unitPrice
        val profit = actualSold * (item.unitPrice - item.costPrice)
        val detail = "Sold $actualSold $unitLabel(s) · ${formatMwk(amount)}"
        logAction(item.name, "Sale", detail, actorName, profit = profit)
        if (!item.isLowStock && updated.isLowStock) lowStockNotifier.notifyLowStock(updated)
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
    ): Long {
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
        } else if (delta > 0) {
            profit = null
            detail = "Opening $openingStock $unitLabel(s) → Closing $closingStock · Stock increased by $delta, no sale recorded"
        } else {
            profit = null
            detail = "Opening $openingStock $unitLabel(s) → Closing $closingStock · No change"
        }
        val entryId = logAction(item.name, "Stock Take", detail, actorName, profit = profit)
        if (!item.isLowStock && updated.isLowStock) lowStockNotifier.notifyLowStock(updated)
        return entryId
    }

    /**
     * Reverts a stock take taken by mistake: restores the item's pre-count quantity and removes
     * the audit entry it created. Deliberately a real delete, not a correcting entry — an
     * un-noticed typo fixed within seconds of saving was never a real business event, so it
     * shouldn't leave two confusing rows in the permanent audit history.
     */
    suspend fun undoStockTake(item: InventoryItem, previousQuantity: Int, auditEntryId: Long) {
        val restored = item.copy(quantity = previousQuantity, lastUpdated = System.currentTimeMillis())
        inventoryDao.update(restored)
        auditLogDao.deleteById(auditEntryId)
        if (!restored.isLowStock) lowStockNotifier.cancelLowStock(restored.id)
    }

    private suspend fun logAction(
        itemName: String,
        action: String,
        detail: String,
        actorName: String,
        receiptPath: String? = null,
        profit: Double? = null,
    ): Long = auditLogDao.insert(
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
