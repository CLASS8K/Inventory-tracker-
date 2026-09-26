package com.example.inventory.util

import com.example.inventory.data.AuditLogEntry
import com.example.inventory.data.InventoryItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val CSV_HEADER = listOf(
    "Name", "SKU", "Category", "Unit", "Quantity", "Low Stock Threshold",
    "Unit Price (MWK)", "Total Value (MWK)", "Last Updated",
)

/** Full inventory as CSV — cost/accounting reconciliation, not the on-screen filtered view. */
fun buildInventoryCsv(items: List<InventoryItem>): String {
    val timestampFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
    return buildString {
        appendLine(CSV_HEADER.joinToString(",") { escapeCsvField(it) })
        for (item in items) {
            val row = listOf(
                item.name,
                item.sku,
                item.category,
                item.unit,
                item.quantity.toString(),
                item.lowStockThreshold.toString(),
                formatMwkAmount(item.unitPrice),
                formatMwkAmount(item.quantity * item.unitPrice),
                timestampFormat.format(Date(item.lastUpdated)),
            )
            appendLine(row.joinToString(",") { escapeCsvField(it) })
        }
    }
}

private val SALES_CSV_HEADER = listOf(
    "Date/Time", "Staff", "Item", "Action", "Detail", "Revenue (MWK)", "Profit (MWK)",
)

/**
 * Audit log entries as CSV, admin-only (it includes profit) — for reconciling a shift or day
 * against a printed till slip or bank deposit, not the on-screen truncated detail text.
 */
fun buildSalesCsv(entries: List<AuditLogEntry>): String {
    val timestampFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
    return buildString {
        appendLine(SALES_CSV_HEADER.joinToString(",") { escapeCsvField(it) })
        for (entry in entries) {
            val row = listOf(
                timestampFormat.format(Date(entry.timestamp)),
                entry.actorName,
                entry.itemName,
                entry.action,
                entry.detail,
                entry.revenue?.let { formatMwkAmount(it) } ?: "",
                entry.profit?.let { formatMwkAmount(it) } ?: "",
            )
            appendLine(row.joinToString(",") { escapeCsvField(it) })
        }
    }
}

private fun escapeCsvField(value: String): String =
    if (value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
        "\"${value.replace("\"", "\"\"")}\""
    } else {
        value
    }
