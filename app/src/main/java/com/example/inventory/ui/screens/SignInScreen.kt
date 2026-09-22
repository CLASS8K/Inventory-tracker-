package com.example.inventory.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.inventory.data.UserProfile
import com.example.inventory.data.UserRole
import com.example.inventory.ui.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignInScreen(viewModel: AuthViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var pendingSignIn by remember { mutableStateOf<UserProfile?>(null) }
    var signInError by remember { mutableStateOf(false) }
    var showCreateDialog by remember { mutableStateOf(false) }

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    Scaffold { padding ->
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(400)),
            modifier = Modifier.padding(padding).fillMaxSize(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height(32.dp))
                Text(text = "🛖", style = MaterialTheme.typography.displayMedium)
                Text(
                    text = "Nkhokwe",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = if (uiState.hasAnyUsers) "Who's stocking today?" else "Set up the granary",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp, bottom = 24.dp),
                )

                if (!uiState.hasAnyUsers) {
                    Text(
                        text = "Create the first account. It will be the admin account with full control.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 16.dp),
                    )
                    Button(onClick = { showCreateDialog = true }) {
                        Icon(Icons.Default.PersonAdd, contentDescription = null)
                        Spacer(Modifier.size(8.dp))
                        Text("Create admin account")
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 24.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        items(uiState.users, key = { it.id }) { user ->
                            ProfileCard(user = user, onClick = { pendingSignIn = user; signInError = false })
                        }
                        item {
                            AddProfileCard(onClick = { showCreateDialog = true })
                        }
                    }
                }
            }
        }
    }

    pendingSignIn?.let { user ->
        PinEntryDialog(
            user = user,
            isError = signInError,
            onDismiss = { pendingSignIn = null },
            onSubmit = { pin ->
                viewModel.signIn(user, pin) { success ->
                    if (success) {
                        pendingSignIn = null
                    } else {
                        signInError = true
                    }
                }
            },
        )
    }

    if (showCreateDialog) {
        CreateUserDialog(
            forcedRole = if (!uiState.hasAnyUsers) UserRole.ADMIN else null,
            onDismiss = { showCreateDialog = false },
            onCreate = { name, pin, avatar, role ->
                viewModel.createUser(
                    name = name,
                    pin = pin,
                    avatarEmoji = avatar,
                    role = role,
                    signInAfterCreate = true,
                ) {
                    showCreateDialog = false
                }
            },
        )
    }
}

@Composable
private fun ProfileCard(user: UserProfile, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(text = user.avatarEmoji, style = MaterialTheme.typography.displaySmall)
            Spacer(Modifier.height(8.dp))
            Text(
                text = user.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
            RoleBadge(role = user.role)
            if (user.role == UserRole.STOCK_KEEPER) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Lv ${user.level} · ${user.xp} XP",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                LinearProgressIndicator(
                    progress = { user.xpIntoLevel / user.xpForNextLevel.toFloat() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                        .height(4.dp),
                )
            }
        }
    }
}

@Composable
private fun AddProfileCard(onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                Icons.Default.PersonAdd,
                contentDescription = "Add user",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp),
            )
            Spacer(Modifier.height(8.dp))
            Text("Add stock keeper", style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
fun RoleBadge(role: UserRole) {
    val (label, color) = when (role) {
        UserRole.ADMIN -> "👑 Admin" to MaterialTheme.colorScheme.tertiary
        UserRole.STOCK_KEEPER -> "📦 Stock Keeper" to MaterialTheme.colorScheme.secondary
    }
    Surface(
        color = color.copy(alpha = 0.18f),
        shape = RoundedCornerShape(50),
        modifier = Modifier.padding(top = 4.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
        )
    }
}

@Composable
private fun PinEntryDialog(
    user: UserProfile,
    isError: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (String) -> Unit,
) {
    var pin by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${user.avatarEmoji} Enter PIN for ${user.name}") },
        text = {
            Column {
                OutlinedTextField(
                    value = pin,
                    onValueChange = { input -> if (input.length <= 4) pin = input.filter { it.isDigit() } },
                    label = { Text("4-digit PIN") },
                    singleLine = true,
                    isError = isError,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                )
                AnimatedVisibility(visible = isError) {
                    Text(
                        text = "Incorrect PIN, try again.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(enabled = pin.length == 4, onClick = { onSubmit(pin) }) { Text("Unlock") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CreateUserDialog(
    forcedRole: UserRole?,
    onDismiss: () -> Unit,
    onCreate: (name: String, pin: String, avatar: String, role: UserRole) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var avatar by remember { mutableStateOf(UserProfile.AVATAR_CHOICES.first()) }
    var role by remember { mutableStateOf(forcedRole ?: UserRole.STOCK_KEEPER) }

    val pinsMatch = pin.length == 4 && pin == confirmPin
    val isValid = name.isNotBlank() && pinsMatch

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (forcedRole == UserRole.ADMIN) "Create admin account" else "Add a stock keeper") },
        text = {
            Column {
                Text("Choose an avatar", style = MaterialTheme.typography.labelLarge)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 8.dp, bottom = 12.dp),
                ) {
                    UserProfile.AVATAR_CHOICES.forEach { emoji ->
                        val selected = emoji == avatar
                        Surface(
                            shape = CircleShape,
                            color = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                            modifier = Modifier
                                .size(40.dp)
                                .clickable { avatar = emoji },
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                Text(emoji, style = MaterialTheme.typography.titleLarge)
                            }
                        }
                    }
                }
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = pin,
                    onValueChange = { input -> if (input.length <= 4) pin = input.filter { it.isDigit() } },
                    label = { Text("4-digit PIN") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
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
                if (forcedRole == null) {
                    Text(
                        text = "New accounts join as Stock Keepers. Promote to Admin from the Admin Panel.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(enabled = isValid, onClick = { onCreate(name.trim(), pin, avatar, role) }) {
                Text("Create")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
