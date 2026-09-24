package com.example.inventory.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.inventory.data.AuditLogEntry
import com.example.inventory.util.formatMwk
import com.example.inventory.util.formatRelativeTime
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuditLogSheet(entries: List<AuditLogEntry>, isAdmin: Boolean, onDismiss: () -> Unit) {
    var viewingReceipt by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Text(
                text = "Audit log",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 12.dp),
            )
            if (entries.isEmpty()) {
                Text(
                    text = "No activity yet.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 24.dp),
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                ) {
                    items(entries, key = { it.id }) { entry ->
                        AuditLogRow(entry, isAdmin = isAdmin, onViewReceipt = { viewingReceipt = entry.receiptPath })
                    }
                }
            }
        }
    }

    viewingReceipt?.let { path ->
        Dialog(onDismissRequest = { viewingReceipt = null }) {
            AsyncImage(
                model = File(path),
                contentDescription = "Receipt photo",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewingReceipt = null },
            )
        }
    }
}

@Composable
private fun ActionBadge(action: String) {
    val color = when (action) {
        "Created" -> MaterialTheme.colorScheme.tertiary
        "Deleted" -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.primary
    }
    Surface(color = color.copy(alpha = 0.16f), shape = RoundedCornerShape(50)) {
        Text(
            text = action.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
        )
    }
}

@Composable
private fun AuditLogRow(entry: AuditLogEntry, isAdmin: Boolean, onViewReceipt: () -> Unit) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ActionBadge(entry.action)
            Text(text = entry.itemName, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f, fill = false))
            if (entry.receiptPath != null) {
                AsyncImage(
                    model = File(entry.receiptPath),
                    contentDescription = "Receipt for ${entry.itemName}",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(28.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(6.dp))
                        .clickable(onClick = onViewReceipt),
                )
            }
        }
        Text(
            text = entry.detail,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp),
        )
        if (isAdmin && entry.profit != null) {
            Text(
                text = "Profit: ${formatMwk(entry.profit)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Text(
            text = "${entry.actorName} · ${formatRelativeTime(entry.timestamp)}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
