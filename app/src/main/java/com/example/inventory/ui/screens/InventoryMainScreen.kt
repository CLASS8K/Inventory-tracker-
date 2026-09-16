package com.example.inventory.ui.screens

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.inventory.data.InventoryItem
import com.example.inventory.ui.InventoryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryMainScreen(viewModel: InventoryViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var editingItem by remember { mutableStateOf<InventoryItem?>(null) }
    var isAdding by remember { mutableStateOf(false) }
    var showAuditLog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Inventory") },
                actions = {
                    IconButton(onClick = { showAuditLog = true }) {
                        Icon(Icons.Default.History, contentDescription = "Audit log")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { isAdding = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add item")
            }
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = viewModel::onSearchQueryChange,
                label = { Text("Search by name, SKU, or category") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )

            if (uiState.lowStockItems.isNotEmpty()) {
                LowStockBanner(items = uiState.lowStockItems)
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
                        InventoryItemRow(item = item, onClick = { editingItem = item })
                    }
                }
            }
        }
    }

    if (isAdding) {
        ItemEditorDialog(
            item = null,
            onDismiss = { isAdding = false },
            onSave = { name, sku, category, quantity, threshold, price ->
                viewModel.addItem(name, sku, category, quantity, threshold, price)
                isAdding = false
            },
            onDelete = null,
        )
    }

    editingItem?.let { item ->
        ItemEditorDialog(
            item = item,
            onDismiss = { editingItem = null },
            onSave = { name, sku, category, quantity, threshold, price ->
                viewModel.updateItem(
                    previous = item,
                    updated = item.copy(
                        name = name,
                        sku = sku,
                        category = category,
                        quantity = quantity,
                        lowStockThreshold = threshold,
                        unitPrice = price,
                        lastUpdated = System.currentTimeMillis(),
                    ),
                )
                editingItem = null
            },
            onDelete = {
                viewModel.deleteItem(item)
                editingItem = null
            },
        )
    }

    if (showAuditLog) {
        AuditLogSheet(entries = uiState.auditLog, onDismiss = { showAuditLog = false })
    }
}

@Composable
private fun LowStockBanner(items: List<InventoryItem>) {
    val context = LocalContext.current
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(12.dp),
        ) {
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
            IconButton(onClick = { sendLowStockEmail(context, items) }) {
                Icon(
                    Icons.Default.Email,
                    contentDescription = "Email low stock alert",
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        }
    }
}

private fun sendLowStockEmail(context: android.content.Context, items: List<InventoryItem>) {
    val body = buildString {
        appendLine("The following items are low on stock:")
        appendLine()
        for (item in items) {
            appendLine("- ${item.name} (${item.sku}): ${item.quantity} left, threshold ${item.lowStockThreshold}")
        }
    }
    val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:")).apply {
        putExtra(Intent.EXTRA_SUBJECT, "Low stock alert")
        putExtra(Intent.EXTRA_TEXT, body)
    }
    try {
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(context, "No email app found", Toast.LENGTH_SHORT).show()
    }
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
private fun InventoryItemRow(item: InventoryItem, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = item.name, fontWeight = FontWeight.Bold)
                Text(
                    text = "${item.sku} · ${item.category}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${item.quantity} in stock",
                    color = if (item.isLowStock) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                    fontWeight = if (item.isLowStock) FontWeight.Bold else FontWeight.Normal,
                )
                Text(
                    text = "$%.2f/unit".format(item.unitPrice),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
