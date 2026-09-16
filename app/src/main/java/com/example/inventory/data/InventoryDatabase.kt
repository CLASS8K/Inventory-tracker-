package com.example.inventory.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [InventoryItem::class, AuditLogEntry::class],
    version = 1,
    exportSchema = false,
)
abstract class InventoryDatabase : RoomDatabase() {
    abstract fun inventoryDao(): InventoryDao
    abstract fun auditLogDao(): AuditLogDao
}
