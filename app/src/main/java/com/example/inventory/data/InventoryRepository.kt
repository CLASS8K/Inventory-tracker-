package com.example.inventory.data

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

    suspend fun updateItem(previous: InventoryItem, updated: InventoryItem, actorName: String) {
        inventoryDao.update(updated)
        val detail = if (previous.quantity != updated.quantity) {
            val delta = updated.quantity - previous.quantity
            val sign = if (delta > 0) "+" else ""
            "Quantity changed from ${previous.quantity} to ${updated.quantity} ($sign$delta)"
        } else {
            "Details updated"
        }
        logAction(updated.name, "Updated", detail, actorName)
    }

    suspend fun deleteItem(item: InventoryItem, actorName: String) {
        inventoryDao.delete(item)
        logAction(item.name, "Deleted", "Removed from inventory", actorName)
    }

    private suspend fun logAction(itemName: String, action: String, detail: String, actorName: String) {
        auditLogDao.insert(
            AuditLogEntry(itemName = itemName, action = action, detail = detail, actorName = actorName),
        )
    }
}
