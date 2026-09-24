package com.example

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.inventory.data.UserRole
import com.example.inventory.ui.AuthViewModel
import com.example.inventory.ui.InventoryViewModel
import com.example.inventory.ui.LicenseViewModel
import com.example.inventory.ui.SupplierViewModel
import com.example.inventory.ui.screens.AdminPanelScreen
import com.example.inventory.ui.screens.InventoryMainScreen
import com.example.inventory.ui.screens.SignInScreen
import com.example.ui.theme.MyApplicationTheme
import dagger.hilt.android.AndroidEntryPoint

/** Auto-signs out a forgotten session on a shared device after this much time in the background. */
private const val SESSION_TIMEOUT_MILLIS = 5 * 60 * 1000L

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    NkhokweApp()
                }
            }
        }
    }
}

@Composable
private fun NkhokweApp() {
    val authViewModel: AuthViewModel = hiltViewModel()
    val inventoryViewModel: InventoryViewModel = hiltViewModel()
    val supplierViewModel: SupplierViewModel = hiltViewModel()
    val licenseViewModel: LicenseViewModel = hiltViewModel()
    val currentUser by authViewModel.currentUser.collectAsStateWithLifecycle()
    val licenseStatus by licenseViewModel.status.collectAsStateWithLifecycle()
    val licenseDaysUntilDue by licenseViewModel.daysUntilDue.collectAsStateWithLifecycle()

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* Low-stock alerts just stay silent if denied — nothing else depends on this. */ }
    LaunchedEffect(currentUser?.id) {
        if (currentUser != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        var backgroundedAt: Long? = null
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> backgroundedAt = System.currentTimeMillis()
                Lifecycle.Event.ON_START -> {
                    val elapsed = backgroundedAt?.let { System.currentTimeMillis() - it }
                    if (elapsed != null && elapsed >= SESSION_TIMEOUT_MILLIS) {
                        authViewModel.signOut()
                    }
                    backgroundedAt = null
                    licenseViewModel.refresh()
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (currentUser == null) {
        SignInScreen(viewModel = authViewModel)
        return
    }

    var showAdminPanel by remember { mutableStateOf(false) }
    if (showAdminPanel && currentUser?.role == UserRole.ADMIN) {
        AdminPanelScreen(viewModel = authViewModel, supplierViewModel = supplierViewModel, onBack = { showAdminPanel = false })
    } else {
        InventoryMainScreen(
            viewModel = inventoryViewModel,
            supplierViewModel = supplierViewModel,
            licenseStatus = licenseStatus,
            licenseDaysUntilDue = licenseDaysUntilDue,
            onOpenAdminPanel = { showAdminPanel = true },
            onSignOut = {
                showAdminPanel = false
                authViewModel.signOut()
            },
        )
    }
}

// Kept for Robolectric/Roborazzi test compatibility
@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(text = "Hello $name!", modifier = modifier)
}
