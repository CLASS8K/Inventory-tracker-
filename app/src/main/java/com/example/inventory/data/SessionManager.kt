package com.example.inventory.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** Holds the signed-in user for the current app process; there is no persisted login. */
@Singleton
class SessionManager @Inject constructor() {
    private val _currentUser = MutableStateFlow<UserProfile?>(null)
    val currentUser: StateFlow<UserProfile?> = _currentUser.asStateFlow()

    fun signIn(user: UserProfile) {
        _currentUser.value = user
    }

    fun updateCurrentUser(user: UserProfile) {
        if (_currentUser.value?.id == user.id) {
            _currentUser.value = user
        }
    }

    fun signOut() {
        _currentUser.value = null
    }
}
