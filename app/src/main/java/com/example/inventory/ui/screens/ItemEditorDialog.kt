package com.example.inventory.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.inventory.data.InventoryItem
import com.example.inventory.data.UserRole
import java.io.File

@Composable
fun ItemEditorDialog(
    item: InventoryItem?,
    role: UserRole,
    onImportImage: (Uri, (String?) -> Unit) -> Unit,
    onDismiss: () -> Unit,
    onSave: (
        name: String,
        sku: String,
        category: String,
        quantity: Int,
        lowStockThreshold: Int,
        unitPrice: Double,
        photoPath: String?,
        receiptPath: String?,
    ) -> Unit,
    onDelete: (() -> Unit)?,
) {
    var name by remember { mutableStateOf(item?.name.orEmpty()) }
    var sku by remember { mutableStateOf(item?.sku.orEmpty()) }
    var category by remember { mutableStateOf(item?.category.orEmpty()) }
    var quantity by remember { mutableStateOf(item?.quantity?.toString().orEmpty()) }
    var threshold by remember { mutableStateOf(item?.lowStockThreshold?.toString().orEmpty()) }
    var price by remember { mutableStateOf(item?.unitPrice?.toString().orEmpty()) }
    var photoPath by remember { mutableStateOf(item?.photoPath) }
    var receiptPath by remember { mutableStateOf<String?>(null) }

    // A Stock Keeper may only restock an existing item's quantity; item configuration is admin-only.
    val canEditConfig = role == UserRole.ADMIN
    val canDelete = role == UserRole.ADMIN && onDelete != null

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) onImportImage(uri) { path -> photoPath = path }
    }
    val receiptPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) onImportImage(uri) { path -> receiptPath = path }
    }
    val imageOnlyRequest = PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)

    val isValid = name.isNotBlank() &&
        quantity.toIntOrNull() != null &&
        threshold.toIntOrNull() != null &&
        price.toDoubleOrNull() != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (item == null) "Add item" else if (canEditConfig) "Edit item" else "Restock item") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                if (canEditConfig) {
                    PhotoPickerRow(
                        label = if (photoPath == null) "Add item photo" else "Change item photo",
                        photoPath = photoPath,
                        onClick = { photoPicker.launch(imageOnlyRequest) },
                    )
                }
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
                ReceiptPickerRow(
                    hasReceipt = receiptPath != null,
                    onClick = { receiptPicker.launch(imageOnlyRequest) },
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
                        photoPath,
                        receiptPath,
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

@Composable
private fun PhotoPickerRow(label: String, photoPath: String?, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 8.dp)
            .clickable(onClick = onClick),
    ) {
        if (photoPath != null) {
            AsyncImage(
                model = File(photoPath),
                contentDescription = "Item photo",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(48.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp)),
            )
        } else {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.PhotoCamera, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Text(text = label, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(start = 12.dp))
    }
}

@Composable
private fun ReceiptPickerRow(hasReceipt: Boolean, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .clickable(onClick = onClick),
    ) {
        Icon(
            if (hasReceipt) Icons.Default.CheckCircle else Icons.Default.Receipt,
            contentDescription = null,
            tint = if (hasReceipt) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = if (hasReceipt) "Receipt attached for this restock" else "Attach a receipt photo (optional)",
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}
