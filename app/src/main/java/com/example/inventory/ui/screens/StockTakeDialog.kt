package com.example.inventory.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
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
import com.example.inventory.util.formatMwk

@Composable
fun StockTakeDialog(
    item: InventoryItem,
    isAdmin: Boolean,
    onDismiss: () -> Unit,
    onSave: (openingStock: Int, closingStock: Int) -> Unit,
) {
    var opening by remember { mutableStateOf(item.quantity.toString()) }
    var closing by remember { mutableStateOf("") }

    val openingValue = opening.toIntOrNull()
    val closingValue = closing.toIntOrNull()
    val isValid = openingValue != null && openingValue >= 0 && closingValue != null && closingValue >= 0

    val unitLabel = item.unit.ifBlank { InventoryItem.DEFAULT_UNIT }
    val summary = if (openingValue != null && closingValue != null) {
        val delta = closingValue - openingValue
        if (delta < 0) {
            val quantitySold = -delta
            val revenue = "Sold $quantitySold $unitLabel(s) · ${formatMwk(quantitySold * item.unitPrice)}"
            if (isAdmin) {
                val profit = quantitySold * (item.unitPrice - item.costPrice)
                "$revenue · Profit ${formatMwk(profit)}"
            } else {
                revenue
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
            Column {
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
                summary?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = isValid,
                onClick = { onSave(openingValue!!, closingValue!!) },
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
