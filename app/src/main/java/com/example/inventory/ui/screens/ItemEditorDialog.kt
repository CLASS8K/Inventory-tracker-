package com.example.inventory.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import com.example.inventory.data.UserRole

@Composable
fun ItemEditorDialog(
    item: InventoryItem?,
    role: UserRole,
    onDismiss: () -> Unit,
    onSave: (name: String, sku: String, category: String, quantity: Int, lowStockThreshold: Int, unitPrice: Double) -> Unit,
    onDelete: (() -> Unit)?,
) {
    var name by remember { mutableStateOf(item?.name.orEmpty()) }
    var sku by remember { mutableStateOf(item?.sku.orEmpty()) }
    var category by remember { mutableStateOf(item?.category.orEmpty()) }
    var quantity by remember { mutableStateOf(item?.quantity?.toString().orEmpty()) }
    var threshold by remember { mutableStateOf(item?.lowStockThreshold?.toString().orEmpty()) }
    var price by remember { mutableStateOf(item?.unitPrice?.toString().orEmpty()) }

    // A Stock Keeper may only restock an existing item's quantity; item configuration is admin-only.
    val canEditConfig = role == UserRole.ADMIN
    val canDelete = role == UserRole.ADMIN && onDelete != null

    val isValid = name.isNotBlank() &&
        quantity.toIntOrNull() != null &&
        threshold.toIntOrNull() != null &&
        price.toDoubleOrNull() != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (item == null) "Add item" else if (canEditConfig) "Edit item" else "Restock item") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    enabled = canEditConfig,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                )
                OutlinedTextField(
                    value = sku,
                    onValueChange = { sku = it },
                    label = { Text("SKU") },
                    singleLine = true,
                    enabled = canEditConfig,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                )
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Category") },
                    singleLine = true,
                    enabled = canEditConfig,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                )
                OutlinedTextField(
                    value = quantity,
                    onValueChange = { input -> quantity = input.filter { it.isDigit() } },
                    label = { Text("Quantity") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                )
                OutlinedTextField(
                    value = threshold,
                    onValueChange = { input -> threshold = input.filter { it.isDigit() } },
                    label = { Text("Low stock threshold") },
                    singleLine = true,
                    enabled = canEditConfig,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                )
                OutlinedTextField(
                    value = price,
                    onValueChange = { input -> price = input.filter { it.isDigit() || it == '.' } },
                    label = { Text("Unit Price (MWK)") },
                    singleLine = true,
                    enabled = canEditConfig,
                    leadingIcon = { Text("MWK") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                )
                if (!canEditConfig) {
                    Text(
                        text = "Only Admins can change item details, pricing, or thresholds.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
                if (canDelete) {
                    TextButton(onClick = onDelete!!, modifier = Modifier.padding(top = 12.dp)) {
                        Text("Delete item")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = isValid,
                onClick = {
                    onSave(
                        name.trim(),
                        sku.trim(),
                        category.trim(),
                        quantity.toInt(),
                        threshold.toInt(),
                        price.toDouble(),
                    )
                },
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}
