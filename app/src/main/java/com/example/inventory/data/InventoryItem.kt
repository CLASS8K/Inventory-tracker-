package com.example.inventory.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "inventory_items")
data class InventoryItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val sku: String,
    val category: String,
    val quantity: Int,
    val lowStockThreshold: Int,
    val unitPrice: Double,
    val lastUpdated: Long = System.currentTimeMillis(),
    /** Absolute path to a locally-stored photo of the item, copied in via [com.example.inventory.data.ImageStore]. */
    val photoPath: String? = null,
) {
    val isLowStock: Boolean
        get() = quantity <= lowStockThreshold
}
