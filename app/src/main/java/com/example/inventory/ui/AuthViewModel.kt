package com.example.inventory.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.inventory.data.BackupManager
import com.example.inventory.data.SessionManager
import com.example.inventory.data.UserProfile
import com.example.inventory.data.UserRepository
import com.example.inventory.data.UserRole
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(
    val users: List<UserProfile> = emptyList(),
    val currentUser: UserProfile? = null,
) {
    val hasAnyUsers: Boolean get() = users.isNotEmpty()
    val leaderboard: List<UserProfile>
        get() = users.filter { it.role == UserRole.STOCK_KEEPER }.sortedByDescending { it.xp }
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val sessionManager: SessionManager,
    private val backupManager: BackupManager,
) : ViewModel() {

    val uiState: StateFlow<AuthUiState> = combine(
        userRepository.users,
        sessionManager.currentUser,
    ) { users, currentUser ->
        AuthUiState(users = users, currentUser = currentUser)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AuthUiState(),
    )

    val currentUser: StateFlow<UserProfile?> = sessionManager.currentUser

    fun signIn(user: UserProfile, pin: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            if (userRepository.authenticate(user, pin)) {
                sessionManager.signIn(user)
                onResult(true)
            } else {
                onResult(false)
            }
        }
    }

    fun createUser(
        name: String,
        pin: String,
        avatarEmoji: String,
        role: UserRole,
        signInAfterCreate: Boolean,
        onResult: (UserProfile) -> Unit,
    ) {
        viewModelScope.launch {
            val user = userRepository.createUser(name, pin, avatarEmoji, role)
            if (signInAfterCreate) {
                sessionManager.signIn(user)
            }
            onResult(user)
        }
    }

    fun deleteUser(user: UserProfile, onResult: (success: Boolean) -> Unit) {
        viewModelScope.launch {
            if (userRepository.canDeleteUser(user)) {
                userRepository.deleteUser(user)
                onResult(true)
            } else {
                onResult(false)
            }
        }
    }

    fun resetPin(user: UserProfile, newPin: String, onResult: () -> Unit) {
        viewModelScope.launch {
            val updated = userRepository.resetPin(user, newPin)
            sessionManager.updateCurrentUser(updated)
            onResult()
        }
    }

    fun exportBackup(destination: Uri, onResult: (success: Boolean) -> Unit) {
        viewModelScope.launch {
            val success = runCatching { backupManager.exportTo(destination) }.isSuccess
            onResult(success)
        }
    }

    fun importBackup(source: Uri, onResult: (success: Boolean) -> Unit) {
        viewModelScope.launch {
            val success = runCatching { backupManager.importFrom(source) }.isSuccess
            onResult(success)
        }
    }

    fun signOut() {
        sessionManager.signOut()
    }
}
