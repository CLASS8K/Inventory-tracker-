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
    /** How this item is counted/sold, e.g. Bottle, Can, Shot — free text but the editor suggests a fixed set. */
    val unit: String = DEFAULT_UNIT,
) {
    val isLowStock: Boolean
        get() = quantity <= lowStockThreshold

    companion object {
        const val DEFAULT_UNIT = "Unit"
        val UNIT_CHOICES = listOf("Unit", "Bottle", "Can", "Shot")
    }
}
