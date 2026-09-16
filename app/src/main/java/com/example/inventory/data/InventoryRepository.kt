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

    suspend fun addItem(item: InventoryItem) {
        inventoryDao.upsert(item)
        logAction(item.name, "Created", "Added with quantity ${item.quantity}")
    }

    suspend fun updateItem(previous: InventoryItem, updated: InventoryItem) {
        inventoryDao.update(updated)
        val detail = if (previous.quantity != updated.quantity) {
            "Quantity changed from ${previous.quantity} to ${updated.quantity}"
        } else {
            "Details updated"
        }
        logAction(updated.name, "Updated", detail)
    }

    suspend fun deleteItem(item: InventoryItem) {
        inventoryDao.delete(item)
        logAction(item.name, "Deleted", "Removed from inventory")
    }

    private suspend fun logAction(itemName: String, action: String, detail: String) {
        auditLogDao.insert(AuditLogEntry(itemName = itemName, action = action, detail = detail))
    }
}
