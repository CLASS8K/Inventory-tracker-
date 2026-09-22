package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.inventory.data.UserRole
import com.example.inventory.ui.AuthViewModel
import com.example.inventory.ui.InventoryViewModel
import com.example.inventory.ui.screens.AdminPanelScreen
import com.example.inventory.ui.screens.InventoryMainScreen
import com.example.inventory.ui.screens.SignInScreen
import com.example.ui.theme.MyApplicationTheme
import dagger.hilt.android.AndroidEntryPoint

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
    val currentUser by authViewModel.currentUser.collectAsStateWithLifecycle()

    if (currentUser == null) {
        SignInScreen(viewModel = authViewModel)
        return
    }

    var showAdminPanel by remember { mutableStateOf(false) }
    if (showAdminPanel && currentUser?.role == UserRole.ADMIN) {
        AdminPanelScreen(viewModel = authViewModel, onBack = { showAdminPanel = false })
    } else {
        InventoryMainScreen(
            viewModel = inventoryViewModel,
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
