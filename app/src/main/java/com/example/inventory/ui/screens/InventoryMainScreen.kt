package com.example.inventory.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.inventory.data.InventoryItem
import com.example.inventory.data.Supplier
import com.example.inventory.data.UserProfile
import com.example.inventory.data.UserRole
import com.example.inventory.ui.ALL_CATEGORIES
import com.example.inventory.ui.InventoryUiState
import com.example.inventory.ui.InventoryViewModel
import com.example.inventory.ui.SortOption
import com.example.inventory.ui.SupplierViewModel
import com.example.inventory.util.buildInventoryCsv
import com.example.inventory.util.formatMwk
import com.example.inventory.util.rememberBarcodeScanner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryMainScreen(
    viewModel: InventoryViewModel,
    supplierViewModel: SupplierViewModel,
    onOpenAdminPanel: () -> Unit,
    onSignOut: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val suppliers by supplierViewModel.suppliers.collectAsStateWithLifecycle()
    val lastStockTake by viewModel.lastStockTake.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var editingItem by remember { mutableStateOf<InventoryItem?>(null) }
    var stockTakeItem by remember { mutableStateOf<InventoryItem?>(null) }
    var isAdding by remember { mutableStateOf(false) }
    var showAuditLog by remember { mutableStateOf(false) }
    var showShareMenu by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }

    LaunchedEffect(lastStockTake?.auditEntryId) {
        val undo = lastStockTake ?: return@LaunchedEffect
        val result = snackbarHostState.showSnackbar(
            message = "Stock take saved for ${undo.item.name}",
            actionLabel = "Undo",
            duration = SnackbarDuration.Long,
        )
        if (result == SnackbarResult.ActionPerformed) {
            viewModel.undoLastStockTake()
        } else {
            viewModel.dismissStockTakeUndo()
        }
    }

    val role = uiState.currentUser?.role ?: UserRole.STOCK_KEEPER
    val scanSearch = rememberBarcodeScanner(onScanned = viewModel::onSearchQueryChange)

    val csvExportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv"),
    ) { uri ->
        if (uri != null) {
            val csv = buildInventoryCsv(uiState.items)
            coroutineScope.launch {
                val written = withContext(Dispatchers.IO) {
                    runCatching {
                        context.contentResolver.openOutputStream(uri)?.use { it.write(csv.toByteArray()) }
                            ?: error("no output stream")
                    }.isSuccess
                }
                Toast.makeText(
                    context,
                    if (written) "CSV exported" else "CSV export failed",
                    Toast.LENGTH_SHORT,
                ).show()
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Nkhokwe") },
                actions = {
                    Box {
                        IconButton(onClick = { showShareMenu = true }) {
                            Icon(Icons.Default.Share, contentDescription = "Share report")
                        }
                        DropdownMenu(expanded = showShareMenu, onDismissRequest = { showShareMenu = false }) {
                            DropdownMenuItem(
                                text = { Text("Share full inventory report") },
                                onClick = {
                                    showShareMenu = false
                                    shareReport(context, buildReport(uiState.items, "Nkhokwe inventory report", uiState.totalValue))
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("Share low-stock report") },
                                onClick = {
                                    showShareMenu = false
                                    shareReport(
                                        context,
                                        buildReport(uiState.lowStockItems, "Nkhokwe low-stock report", null),
                                    )
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("Export as CSV") },
                                onClick = {
                                    showShareMenu = false
                                    val stamp = SimpleDateFormat("yyyyMMdd-HHmm", Locale.US).format(Date())
                                    csvExportLauncher.launch("nkhokwe-inventory-$stamp.csv")
                                },
                            )
                        }
                    }
                    IconButton(onClick = { showAuditLog = true }) {
                        Icon(Icons.Default.History, contentDescription = "Audit log")
                    }
                    if (uiState.isAdmin) {
                        IconButton(onClick = onOpenAdminPanel) {
                            Icon(Icons.Default.AdminPanelSettings, contentDescription = "Admin panel")
                        }
                    }
                    IconButton(onClick = onSignOut) {
                        Icon(Icons.Default.Logout, contentDescription = "Sign out")
                    }
                },
            )
        },
        floatingActionButton = {
            if (uiState.isAdmin) {
                FloatingActionButton(onClick = { isAdding = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Add item")
                }
            }
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            uiState.currentUser?.let { CurrentUserStrip(it) }

            DashboardRow(uiState = uiState)

            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = viewModel::onSearchQueryChange,
                label = { Text("Search by name, SKU, or category") },
                singleLine = true,
                trailingIcon = {
                    IconButton(onClick = scanSearch) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = "Scan barcode to search")
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            ) {
                CategoryChips(
                    categories = uiState.categories,
                    selected = uiState.categoryFilter,
                    onSelect = viewModel::onCategoryFilterChange,
                    modifier = Modifier.weight(1f),
                )
                Box {
                    IconButton(onClick = { showSortMenu = true }) {
                        Icon(Icons.Default.SortByAlpha, contentDescription = "Sort")
                    }
                    DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }) {
                        SortOption.entries.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option.label) },
                                onClick = {
                                    showSortMenu = false
                                    viewModel.onSortOptionChange(option)
                                },
                            )
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = uiState.lowStockItems.isNotEmpty(),
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                LowStockBanner(items = uiState.lowStockItems, suppliers = suppliers)
            }

            val filtered = uiState.filteredItems
            if (filtered.isEmpty()) {
                EmptyState(hasSearch = uiState.searchQuery.isNotBlank())
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(filtered, key = { it.id }) { item ->
                        InventoryItemRow(
                            item = item,
                            modifier = Modifier.animateItem(),
                            onClick = { editingItem = item },
                            onAdjust = { delta -> viewModel.adjustQuantity(item, delta) },
                            onStockTake = { stockTakeItem = item },
                        )
                    }
                }
            }
        }
    }

    if (isAdding) {
        ItemEditorDialog(
            item = null,
            role = role,
            suppliers = suppliers,
            onCreateSupplier = { name, phone, onResult -> supplierViewModel.createSupplier(name, phone, "", onResult) },
            onImportImage = viewModel::importImage,
            onDismiss = { isAdding = false },
            onSave = { name, sku, category, quantity, threshold, price, photoPath, _, unit, costPrice, supplierId ->
                viewModel.addItem(name, sku, category, quantity, threshold, price, photoPath, unit, costPrice, supplierId)
                isAdding = false
            },
            onDelete = null,
        )
    }

    editingItem?.let { item ->
        ItemEditorDialog(
            item = item,
            role = role,
            suppliers = suppliers,
            onCreateSupplier = { name, phone, onResult -> supplierViewModel.createSupplier(name, phone, "", onResult) },
            onImportImage = viewModel::importImage,
            onDismiss = { editingItem = null },
            onSave = { name, sku, category, quantity, threshold, price, photoPath, receiptPath, unit, costPrice, supplierId ->
                viewModel.updateItem(
                    previous = item,
                    updated = item.copy(
                        name = name,
                        sku = sku,
                        category = category,
                        quantity = quantity,
                        lowStockThreshold = threshold,
                        unitPrice = price,
                        photoPath = photoPath,
                        unit = unit,
                        costPrice = costPrice,
                        supplierId = supplierId,
                        lastUpdated = System.currentTimeMillis(),
                    ),
                    receiptPath = receiptPath,
                )
                editingItem = null
            },
            onDelete = if (role == UserRole.ADMIN) {
                {
                    viewModel.deleteItem(item)
                    editingItem = null
                }
            } else {
                null
            },
        )
    }

    if (showAuditLog) {
        AuditLogSheet(entries = uiState.auditLog, isAdmin = uiState.isAdmin, onDismiss = { showAuditLog = false })
    }

    stockTakeItem?.let { item ->
        StockTakeDialog(
            item = item,
            isAdmin = uiState.isAdmin,
            onDismiss = { stockTakeItem = null },
            onSave = { openingStock, closingStock, reason ->
                viewModel.recordStockTake(item, openingStock, closingStock, reason)
                stockTakeItem = null
            },
        )
    }
}

@Composable
private fun CurrentUserStrip(user: UserProfile) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        Text(text = user.avatarEmoji, style = MaterialTheme.typography.titleLarge)
        Column(modifier = Modifier.padding(start = 8.dp)) {
            Text(text = "Welcome back, ${user.name}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            RoleBadge(role = user.role)
        }
    }
}

@Composable
private fun DashboardRow(uiState: InventoryUiState) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    AnimatedVisibility(visible = visible, enter = fadeIn(tween(400)) + expandVertically()) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            MetricCard(
                label = "Total Value",
                value = formatMwk(uiState.totalValue),
                modifier = Modifier.weight(1.3f),
            )
            MetricCard(
                label = "Units",
                value = uiState.totalUnits.toString(),
                modifier = Modifier.weight(1f),
            )
            MetricCard(
                label = "Low Stock",
                value = uiState.lowStockItems.size.toString(),
                modifier = Modifier.weight(1f),
                emphasize = uiState.lowStockItems.isNotEmpty(),
            )
            if (uiState.isAdmin) {
                MetricCard(
                    label = "Potential Profit",
                    value = formatMwk(uiState.totalPotentialProfit),
                    modifier = Modifier.weight(1.3f),
                )
            }
        }
    }
}

@Composable
private fun MetricCard(label: String, value: String, modifier: Modifier = Modifier, emphasize: Boolean = false) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = if (emphasize) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryChips(
    categories: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = modifier) {
        items(categories) { category ->
            FilterChip(
                selected = category == selected,
                onClick = { onSelect(category) },
                label = { Text(if (category == ALL_CATEGORIES) "All Items" else category) },
            )
        }
    }
}

@Composable
private fun LowStockBanner(items: List<InventoryItem>, suppliers: List<Supplier>) {
    var expanded by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val supplierById = remember(suppliers) { suppliers.associateBy { it.id } }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable { expanded = !expanded },
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                )
                Text(
                    text = "${items.size} item${if (items.size == 1) "" else "s"} low on stock",
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .weight(1f),
                )
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    items.forEach { item ->
                        val supplier = item.supplierId?.let { supplierById[it] }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${item.name} · ${item.quantity} left",
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                                Text(
                                    text = supplier?.name ?: "No supplier set — add one from the item's editor",
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    style = MaterialTheme.typography.labelSmall,
                                )
                            }
                            if (supplier != null && supplier.phone.isNotBlank()) {
                                IconButton(onClick = { callSupplier(context, supplier.phone) }) {
                                    Icon(
                                        Icons.Default.Call,
                                        contentDescription = "Call ${supplier.name}",
                                        tint = MaterialTheme.colorScheme.onErrorContainer,
                                    )
                                }
                                IconButton(onClick = { whatsAppSupplier(context, supplier.phone) }) {
                                    Icon(
                                        Icons.Default.Chat,
                                        contentDescription = "WhatsApp ${supplier.name}",
                                        tint = MaterialTheme.colorScheme.onErrorContainer,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun callSupplier(context: Context, phone: String) {
    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
    context.startActivity(intent)
}

private fun whatsAppSupplier(context: Context, phone: String) {
    val digits = phone.filter { it.isDigit() }
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$digits"))
    context.startActivity(intent)
}

private fun buildReport(items: List<InventoryItem>, title: String, totalValue: Double?): String = buildString {
    appendLine(title)
    appendLine()
    if (items.isEmpty()) {
        appendLine("No items to report.")
    }
    for (item in items) {
        appendLine("- ${item.name} (${item.sku}) · ${item.category}")
        appendLine("  Qty: ${item.quantity} · Unit: ${formatMwk(item.unitPrice)} · Total: ${formatMwk(item.quantity * item.unitPrice)}")
    }
    if (totalValue != null) {
        appendLine()
        appendLine("Total inventory value: ${formatMwk(totalValue)}")
    }
}

private fun shareReport(context: Context, body: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "Nkhokwe inventory report")
        putExtra(Intent.EXTRA_TEXT, body)
    }
    context.startActivity(Intent.createChooser(intent, "Share report"))
}

@Composable
private fun EmptyState(hasSearch: Boolean) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = if (hasSearch) "No items match your search." else "No items yet. Tap + to add your first item.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun StatusBadge(item: InventoryItem) {
    val (label, color) = when {
        item.quantity == 0 -> "Out of Stock" to MaterialTheme.colorScheme.error
        item.isLowStock -> "Low Stock" to Color(0xFFB8790A)
        else -> "In Stock" to Color(0xFF2E7D32)
    }
    Surface(color = color.copy(alpha = 0.16f), shape = RoundedCornerShape(50)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
        )
    }
}

@Composable
private fun InventoryItemRow(
    item: InventoryItem,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onAdjust: (Int) -> Unit,
    onStockTake: () -> Unit,
) {
    Card(onClick = onClick, modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (item.photoPath != null) {
                AsyncImage(
                    model = File(item.photoPath),
                    contentDescription = "${item.name} photo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(44.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp)),
                )
                Spacer(Modifier.width(12.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${item.sku} · ${item.category}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.width(4.dp))
                StatusBadge(item)
            }
            IconButton(onClick = onStockTake) {
                Icon(Icons.Default.Assignment, contentDescription = "Stock take for ${item.name}")
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { onAdjust(-1) }, enabled = item.quantity > 0) {
                    Icon(Icons.Default.Remove, contentDescription = "Decrease quantity")
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "${item.quantity}", fontWeight = FontWeight.Bold)
                    Text(
                        text = formatMwk(item.unitPrice),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = { onAdjust(1) }) {
                    Icon(Icons.Default.Add, contentDescription = "Increase quantity")
                }
            }
        }
    }
}
