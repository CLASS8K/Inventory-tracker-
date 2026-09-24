package com.example.inventory.util

import com.example.inventory.data.InventoryItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val CSV_HEADER = listOf(
    "Name", "SKU", "Category", "Quantity", "Low Stock Threshold",
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

private fun escapeCsvField(value: String): String =
    if (value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
        "\"${value.replace("\"", "\"\"")}\""
    } else {
        value
    }
