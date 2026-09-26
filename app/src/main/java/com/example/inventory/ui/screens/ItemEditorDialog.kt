package com.example.inventory.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.inventory.data.InventoryItem
import com.example.inventory.data.Supplier
import com.example.inventory.data.UserRole
import com.example.inventory.util.formatMwk
import com.example.inventory.util.rememberBarcodeScanner
import java.io.File

/**
 * Default serving unit per category (lowercased) — spirits by the shot, wine by the glass, beer by
 * the bottle. Cider and Soft Drinks are sold as both bottles and cans, so they're left for the
 * admin to pick rather than guessing wrong half the time.
 */
private val CATEGORY_SERVING_UNITS = mapOf(
    "spirits" to "Shot",
    "whisky" to "Shot",
    "wine" to "Glass",
    "beer" to "Bottle",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemEditorDialog(
    item: InventoryItem?,
    role: UserRole,
    isReadOnly: Boolean,
    suppliers: List<Supplier>,
    existingCategories: List<String> = emptyList(),
    onCreateSupplier: (name: String, phone: String, onResult: (Supplier) -> Unit) -> Unit,
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
        unit: String,
        costPrice: Double,
        supplierId: Long?,
        servingsPerPack: Int?,
    ) -> Unit,
    onDelete: (() -> Unit)?,
) {
    var name by remember { mutableStateOf(item?.name.orEmpty()) }
    var sku by remember { mutableStateOf(item?.sku.orEmpty()) }
    var category by remember { mutableStateOf(item?.category.orEmpty()) }
    var quantity by remember { mutableStateOf(item?.quantity?.toString().orEmpty()) }
    var threshold by remember { mutableStateOf(item?.lowStockThreshold?.toString().orEmpty()) }
    var price by remember { mutableStateOf(item?.unitPrice?.toString().orEmpty()) }
    var costPriceText by remember { mutableStateOf(item?.costPrice?.takeIf { it != 0.0 }?.toString().orEmpty()) }
    var photoPath by remember { mutableStateOf(item?.photoPath) }
    var receiptPath by remember { mutableStateOf<String?>(null) }
    var unit by remember { mutableStateOf(item?.unit ?: InventoryItem.DEFAULT_UNIT) }
    var supplierId by remember { mutableStateOf(item?.supplierId) }
    var showCreateSupplier by remember { mutableStateOf(false) }
    var servingsPerPackText by remember { mutableStateOf(item?.servingsPerPack?.toString().orEmpty()) }
    var packCostText by remember { mutableStateOf("") }
    var packCountText by remember { mutableStateOf("") }
    var categoryMenuExpanded by remember { mutableStateOf(false) }
    val categorySuggestions = remember(existingCategories) {
        (InventoryItem.DEFAULT_CATEGORY_SUGGESTIONS + existingCategories).distinct().sorted()
    }
    val filteredCategorySuggestions = remember(category, categorySuggestions) {
        categorySuggestions.filter { it.contains(category, ignoreCase = true) }
    }

    // Spirits are poured by the shot and wine by the glass, not sold whole-bottle — default a new
    // item's unit to how it's actually served, so its selling/cost price are entered on that same
    // footing rather than one per bottle and the other per serving.
    LaunchedEffect(category) {
        val servingUnit = CATEGORY_SERVING_UNITS[category.trim().lowercase()]
        if (item == null && servingUnit != null && unit == InventoryItem.DEFAULT_UNIT) {
            unit = servingUnit
        }
    }

    // A Stock Keeper may only restock an existing item's quantity; item configuration is admin-only.
    val canEditConfig = role == UserRole.ADMIN
    val canDelete = role == UserRole.ADMIN && onDelete != null && !isReadOnly

    // Shots and glasses are poured from a bottle bought as a whole — the pack calculator below
    // turns a bottle cost/count into a per-serving cost/quantity instead of admins doing it by hand.
    val isServingUnit = unit == "Shot" || unit == "Glass"
    val servingsPerPack = servingsPerPackText.toIntOrNull()?.takeIf { it > 0 }
    val computedQuantity = servingsPerPack?.let { spp -> packCountText.toIntOrNull()?.let { it * spp } }
    val computedCostPrice = servingsPerPack?.let { spp -> packCostText.toDoubleOrNull()?.let { it / spp } }

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) onImportImage(uri) { path -> photoPath = path }
    }
    val receiptPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) onImportImage(uri) { path -> receiptPath = path }
    }
    val imageOnlyRequest = PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
    val scanSku = rememberBarcodeScanner(onScanned = { value -> sku = value })

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
                    trailingIcon = if (canEditConfig) {
                        {
                            IconButton(onClick = scanSku) {
                                Icon(Icons.Default.QrCodeScanner, contentDescription = "Scan barcode for SKU")
                            }
                        }
                    } else {
                        null
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                ) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = {
                            category = it
                            categoryMenuExpanded = true
                        },
                        label = { Text("Category / drink type") },
                        singleLine = true,
                        enabled = canEditConfig,
                        trailingIcon = if (canEditConfig && category.isNotEmpty()) {
                            {
                                IconButton(onClick = {
                                    category = ""
                                    categoryMenuExpanded = true
                                }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear category")
                                }
                            }
                        } else {
                            null
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            // Tapping in shows every suggestion right away — previously the dropdown
                            // only opened once you started typing, and clearing a wrong pick meant
                            // backspacing character by character instead of one tap.
                            .onFocusChanged { focusState -> if (focusState.isFocused) categoryMenuExpanded = true },
                    )
                    DropdownMenu(
                        expanded = categoryMenuExpanded && canEditConfig && filteredCategorySuggestions.isNotEmpty(),
                        onDismissRequest = { categoryMenuExpanded = false },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        filteredCategorySuggestions.forEach { suggestion ->
                            DropdownMenuItem(
                                text = { Text(suggestion) },
                                onClick = {
                                    category = suggestion
                                    categoryMenuExpanded = false
                                },
                            )
                        }
                    }
                }
                if (canEditConfig) {
                    Text(
                        text = "Unit",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        // 5 unit choices don't all fit on a narrow phone in an unscrollable Row — the
                        // last chip (Shot) was getting clipped at the screen edge instead of wrapping.
                        modifier = Modifier
                            .horizontalScroll(rememberScrollState())
                            .padding(top = 4.dp),
                    ) {
                        InventoryItem.UNIT_CHOICES.forEach { choice ->
                            FilterChip(
                                selected = unit == choice,
                                onClick = { unit = choice },
                                label = { Text(choice) },
                            )
                        }
                    }
                }
                if (canEditConfig && isServingUnit) {
                    PackCalculator(
                        unit = unit,
                        servingsPerPackText = servingsPerPackText,
                        onServingsPerPackChange = { servingsPerPackText = it },
                        packCountText = packCountText,
                        onPackCountChange = { packCountText = it },
                        packCostText = packCostText,
                        onPackCostChange = { packCostText = it },
                        computedQuantity = computedQuantity,
                        computedCostPrice = computedCostPrice,
                        onApply = {
                            computedQuantity?.let { quantity = it.toString() }
                            computedCostPrice?.let { costPriceText = "%.2f".format(it) }
                        },
                    )
                }
                OutlinedTextField(
                    value = quantity,
                    onValueChange = { input -> quantity = input.filter { it.isDigit() } },
                    label = { Text("Quantity (in ${unit}s)") },
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
                    label = { Text("Selling Price per $unit (MWK)") },
                    singleLine = true,
                    enabled = canEditConfig,
                    leadingIcon = { Text("MWK") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                )
                if (canEditConfig) {
                    // Rendered only for Admins — never shown, even disabled, to a Stock Keeper.
                    OutlinedTextField(
                        value = costPriceText,
                        onValueChange = { input -> costPriceText = input.filter { it.isDigit() || it == '.' } },
                        label = { Text("Cost Price per $unit (MWK) — hidden from Stock Keepers") },
                        singleLine = true,
                        leadingIcon = { Text("MWK") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                    )
                }
                if (canEditConfig) {
                    Text(
                        text = "Reorder from",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(top = 4.dp),
                    ) {
                        FilterChip(
                            selected = supplierId == null,
                            onClick = { supplierId = null },
                            label = { Text("None") },
                        )
                        suppliers.forEach { supplier ->
                            FilterChip(
                                selected = supplierId == supplier.id,
                                onClick = { supplierId = supplier.id },
                                label = { Text(supplier.name) },
                            )
                        }
                        FilterChip(
                            selected = false,
                            onClick = { showCreateSupplier = true },
                            label = { Text("+ New") },
                            leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) },
                        )
                    }
                }
                if (!canEditConfig) {
                    Text(
                        text = "Only Admins can change item details, pricing, or thresholds.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
                if (isReadOnly) {
                    Text(
                        text = "Subscription overdue — saving and deleting are paused until it's renewed.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
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
                enabled = isValid && !isReadOnly,
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
                        unit,
                        costPriceText.toDoubleOrNull() ?: 0.0,
                        supplierId,
                        servingsPerPack,
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

    if (showCreateSupplier) {
        CreateSupplierDialog(
            onDismiss = { showCreateSupplier = false },
            onCreate = { name, phone ->
                onCreateSupplier(name, phone) { supplier ->
                    supplierId = supplier.id
                }
                showCreateSupplier = false
            },
        )
    }
}

/**
 * Turns a bottle cost and a bottle count into a per-serving cost and quantity, so admins don't do
 * that division by hand when a spirit/wine item is bought by the bottle but sold by the shot/glass.
 * Purely a data-entry helper: nothing here is saved except [PackCalculator]'s own [servingsPerPackText],
 * which the caller applies to the real Quantity/Cost Price fields via [onApply].
 */
@Composable
private fun PackCalculator(
    unit: String,
    servingsPerPackText: String,
    onServingsPerPackChange: (String) -> Unit,
    packCountText: String,
    onPackCountChange: (String) -> Unit,
    packCostText: String,
    onPackCostChange: (String) -> Unit,
    computedQuantity: Int?,
    computedCostPrice: Double?,
    onApply: () -> Unit,
) {
    Text(
        text = "Pack calculator",
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 12.dp),
    )
    OutlinedTextField(
        value = servingsPerPackText,
        onValueChange = { input -> onServingsPerPackChange(input.filter { it.isDigit() }) },
        label = { Text("${unit}s per bottle (e.g. 25 shots per 750ml)") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
    )
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
    ) {
        OutlinedTextField(
            value = packCountText,
            onValueChange = { input -> onPackCountChange(input.filter { it.isDigit() }) },
            label = { Text("Bottles received") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f),
        )
        OutlinedTextField(
            value = packCostText,
            onValueChange = { input -> onPackCostChange(input.filter { it.isDigit() || it == '.' }) },
            label = { Text("Cost per bottle (MWK)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.weight(1f),
        )
    }
    if (computedQuantity != null || computedCostPrice != null) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
        ) {
            val quantityPart = computedQuantity?.let { "$it $unit${if (it == 1) "" else "s"}" }
            val costPart = computedCostPrice?.let { "${formatMwk(it)} cost per $unit" }
            Text(
                text = "= " + listOfNotNull(quantityPart, costPart).joinToString(", "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onApply) {
                Text("Use these values")
            }
        }
    }
}

@Composable
private fun CreateSupplierDialog(onDismiss: () -> Unit, onCreate: (name: String, phone: String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New supplier") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone (for call / WhatsApp)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank(),
                onClick = { onCreate(name.trim(), phone.trim()) },
            ) {
                Text("Add supplier")
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
