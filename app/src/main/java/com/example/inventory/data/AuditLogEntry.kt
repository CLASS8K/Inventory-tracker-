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
)
