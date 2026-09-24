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
)
