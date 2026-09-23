package com.example.inventory.ui.screens

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.inventory.data.ImportResult
import com.example.inventory.data.UserProfile
import com.example.inventory.data.UserRole
import com.example.inventory.ui.AuthViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPanelScreen(viewModel: AuthViewModel, onBack: () -> Unit) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showCreateDialog by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<UserProfile?>(null) }
    var deleteBlocked by remember { mutableStateOf(false) }
    var pendingPinReset by remember { mutableStateOf<UserProfile?>(null) }
    var showRestoreConfirm by remember { mutableStateOf(false) }
    var restoreComplete by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream"),
    ) { uri ->
        if (uri != null) {
            viewModel.exportBackup(uri) { success ->
                Toast.makeText(
                    context,
                    if (success) "Backup saved" else "Backup failed",
                    Toast.LENGTH_SHORT,
                ).show()
            }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            viewModel.importBackup(uri) { result ->
                when (result) {
                    ImportResult.Success -> restoreComplete = true
                    ImportResult.InvalidFile -> Toast.makeText(
                        context,
                        "That file isn't a Nkhokwe backup from this app version",
                        Toast.LENGTH_LONG,
                    ).show()
                    ImportResult.ReadError -> Toast.makeText(
                        context,
                        "Couldn't read that file",
                        Toast.LENGTH_SHORT,
                    ).show()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Admin Panel") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val stamp = SimpleDateFormat("yyyyMMdd-HHmm", Locale.US).format(Date())
                        exportLauncher.launch("nkhokwe-backup-$stamp.db")
                    }) {
                        Icon(Icons.Default.Backup, contentDescription = "Backup data")
                    }
                    IconButton(onClick = { showRestoreConfirm = true }) {
                        Icon(Icons.Default.Restore, contentDescription = "Restore backup")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreateDialog = true }) {
                Icon(Icons.Default.PersonAdd, contentDescription = "Add user")
            }
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (uiState.leaderboard.isNotEmpty()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                    Text(
                        text = "Restocker leaderboard",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(uiState.users, key = { it.id }) { user ->
                    val rank = uiState.leaderboard.indexOf(user).takeIf { it >= 0 }
                    UserRow(
                        user = user,
                        rank = rank,
                        onDelete = {
                            pendingDelete = user
                            deleteBlocked = false
                        },
                        onResetPin = { pendingPinReset = user },
                    )
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateUserDialog(
            forcedRole = null,
            onDismiss = { showCreateDialog = false },
            onCreate = { name, pin, avatar, role ->
                viewModel.createUser(
                    name = name,
                    pin = pin,
                    avatarEmoji = avatar,
                    role = role,
                    signInAfterCreate = false,
                ) {
                    showCreateDialog = false
                }
            },
        )
    }

    pendingDelete?.let { user ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Remove ${user.name}?") },
            text = {
                Text(
                    if (deleteBlocked) {
                        "Nkhokwe needs at least one admin. Promote another user before removing the last one."
                    } else {
                        "They will lose access immediately. This can't be undone."
                    },
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteUser(user) { success ->
                        if (success) {
                            pendingDelete = null
                        } else {
                            deleteBlocked = true
                        }
                    }
                }) { Text("Remove") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null; deleteBlocked = false }) { Text("Cancel") }
            },
        )
    }

    pendingPinReset?.let { user ->
        ResetPinDialog(
            user = user,
            onDismiss = { pendingPinReset = null },
            onReset = { newPin ->
                viewModel.resetPin(user, newPin) {
                    pendingPinReset = null
                    Toast.makeText(context, "PIN reset for ${user.name}", Toast.LENGTH_SHORT).show()
                }
            },
        )
    }

    if (showRestoreConfirm) {
        AlertDialog(
            onDismissRequest = { showRestoreConfirm = false },
            title = { Text("Restore backup?") },
            text = {
                Text("This replaces every item, user, and audit entry currently on this device with what's in the backup file. This can't be undone.")
            },
            confirmButton = {
                TextButton(onClick = {
                    showRestoreConfirm = false
                    importLauncher.launch(arrayOf("*/*"))
                }) { Text("Choose backup file") }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreConfirm = false }) { Text("Cancel") }
            },
        )
    }

    if (restoreComplete) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Restore complete") },
            text = { Text("Nkhokwe needs to restart to load the restored data.") },
            confirmButton = {
                TextButton(onClick = { restartApp(context) }) { Text("Restart now") }
            },
        )
    }
}

private fun restartApp(context: Context) {
    val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
    val component = launchIntent?.component ?: return
    context.startActivity(Intent.makeRestartActivityTask(component))
    Runtime.getRuntime().exit(0)
}

@Composable
private fun ResetPinDialog(user: UserProfile, onDismiss: () -> Unit, onReset: (String) -> Unit) {
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    val pinsMatch = pin.length == 4 && pin == confirmPin

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Reset PIN for ${user.name}") },
        text = {
            Column {
                OutlinedTextField(
                    value = pin,
                    onValueChange = { input -> if (input.length <= 4) pin = input.filter { it.isDigit() } },
                    label = { Text("New 4-digit PIN") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = confirmPin,
                    onValueChange = { input -> if (input.length <= 4) confirmPin = input.filter { it.isDigit() } },
                    label = { Text("Confirm PIN") },
                    singleLine = true,
                    isError = confirmPin.isNotEmpty() && !pinsMatch,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                )
            }
        },
        confirmButton = {
            TextButton(enabled = pinsMatch, onClick = { onReset(pin) }) { Text("Reset PIN") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
private fun UserRow(user: UserProfile, rank: Int?, onDelete: () -> Unit, onResetPin: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = user.avatarEmoji, style = MaterialTheme.typography.headlineSmall)
            Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                Text(text = user.name, fontWeight = FontWeight.Bold)
                RoleBadge(role = user.role)
                if (user.role == UserRole.STOCK_KEEPER) {
                    Text(
                        text = "Lv ${user.level} · ${user.xp} XP" + (rank?.let { " · #${it + 1} on leaderboard" } ?: ""),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            IconButton(onClick = onResetPin) {
                Icon(Icons.Default.Key, contentDescription = "Reset PIN for ${user.name}")
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Remove ${user.name}", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}
