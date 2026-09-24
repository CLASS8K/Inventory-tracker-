package com.example.inventory.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.inventory.data.InventoryItem
import com.example.inventory.data.StockLossReason
import com.example.inventory.util.formatMwk

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun StockTakeDialog(
    item: InventoryItem,
    isAdmin: Boolean,
    isReadOnly: Boolean,
    onDismiss: () -> Unit,
    onSave: (openingStock: Int, closingStock: Int, reason: StockLossReason) -> Unit,
) {
    var opening by remember { mutableStateOf(item.quantity.toString()) }
    var closing by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf(StockLossReason.SOLD) }

    val openingValue = opening.toIntOrNull()
    val closingValue = closing.toIntOrNull()
    val isValid = openingValue != null && openingValue >= 0 && closingValue != null && closingValue >= 0
    val isLoss = openingValue != null && closingValue != null && closingValue < openingValue

    val unitLabel = item.unit.ifBlank { InventoryItem.DEFAULT_UNIT }
    val summary = if (openingValue != null && closingValue != null) {
        val delta = closingValue - openingValue
        if (delta < 0) {
            val quantityLost = -delta
            if (reason == StockLossReason.SOLD) {
                val revenue = "Sold $quantityLost $unitLabel(s) · ${formatMwk(quantityLost * item.unitPrice)}"
                if (isAdmin) {
                    val profit = quantityLost * (item.unitPrice - item.costPrice)
                    "$revenue · Profit ${formatMwk(profit)}"
                } else {
                    revenue
                }
            } else {
                "Lost $quantityLost $unitLabel(s) to ${reason.label} — not counted as revenue"
            }
        } else if (delta > 0) {
            "Stock increased by $delta $unitLabel(s) — not counted as a sale"
        } else {
            "No change"
        }
    } else {
        null
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Stock take: ${item.name}") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    text = "$unitLabel · ${formatMwk(item.unitPrice)} each",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                OutlinedTextField(
                    value = opening,
                    onValueChange = { input -> opening = input.filter { it.isDigit() } },
                    label = { Text("Opening stock") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = closing,
                    onValueChange = { input -> closing = input.filter { it.isDigit() } },
                    label = { Text("Closing stock (count it now)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                )
                if (isLoss) {
                    Text(
                        text = "Why is stock down?",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(top = 4.dp),
                    ) {
                        StockLossReason.entries.forEach { choice ->
                            FilterChip(
                                selected = reason == choice,
                                onClick = { reason = choice },
                                label = { Text(choice.label) },
                            )
                        }
                    }
                }
                summary?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
                if (isReadOnly) {
                    Text(
                        text = "Subscription overdue — saving a count is paused until it's renewed.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = isValid && !isReadOnly,
                onClick = { onSave(openingValue!!, closingValue!!, reason) },
            ) {
                Text("Save count")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}
