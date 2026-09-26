package com.example.inventory.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "audit_log")
data class AuditLogEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val itemName: String,
    /** One of "Created", "Updated", "Deleted". */
    val action: String,
    val detail: String,
    val actorName: String,
    val timestamp: Long = System.currentTimeMillis(),
    /** Absolute path to a locally-stored receipt/proof-of-restock photo, if one was attached. */
    val receiptPath: String? = null,
    /**
     * Profit realized by a sale, stored as a structured field rather than baked into [detail] so
     * it can be withheld from non-admin viewers without touching the shared text every role sees.
     */
    val profit: Double? = null,
    /**
     * Revenue from an actual sale (a quick-sell tap, or a Stock Take with [StockLossReason.SOLD]) —
     * null for every other entry, including a Stock Take logged as spillage/comp/theft, so summing
     * this column across a day never counts a loss as money taken in.
     */
    val revenue: Double? = null,
) {
    /** An entry that represents money actually taken in — a quick sell tap or a Stock Take
     * reconciled as [StockLossReason.SOLD]. The single source of truth for "was this a sale",
     * so callers never have to re-derive it by combining [action]/[revenue]/[profit] themselves. */
    val isSale: Boolean get() = revenue != null

    /** A Stock Take logged as a loss (spillage, comp, theft, other) rather than a sale — never
     * true for [isSale] entries, since those always carry a non-null [revenue] instead. */
    val isLoss: Boolean get() = action == "Stock Take" && revenue == null && (profit ?: 0.0) < 0
}
