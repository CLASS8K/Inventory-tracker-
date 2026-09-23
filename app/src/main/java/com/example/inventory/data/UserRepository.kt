package com.example.inventory.data

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

sealed interface PinAuthResult {
    data object Success : PinAuthResult
    data object InvalidPin : PinAuthResult
    data class Locked(val retryAfterMillis: Long) : PinAuthResult
}

@Singleton
class UserRepository @Inject constructor(
    private val userDao: UserDao,
) {
    val users: Flow<List<UserProfile>> = userDao.observeAll()

    suspend fun hasAnyUsers(): Boolean = userDao.count() > 0

    suspend fun createUser(
        name: String,
        pin: String,
        avatarEmoji: String,
        role: UserRole,
    ): UserProfile {
        val salt = PinHasher.generateSalt()
        val profile = UserProfile(
            name = name,
            avatarEmoji = avatarEmoji,
            role = role,
            pinSalt = salt,
            pinHash = PinHasher.hash(pin, salt),
        )
        val id = userDao.insert(profile)
        return profile.copy(id = id)
    }

    /**
     * Re-reads the user's row before checking, so lockout state can't be bypassed with a stale
     * in-memory [UserProfile] (e.g. from a list snapshot taken before an earlier failed attempt).
     */
    suspend fun authenticate(user: UserProfile, pin: String): PinAuthResult {
        val now = System.currentTimeMillis()
        val current = userDao.getById(user.id) ?: user
        if (current.isLockedAt(now)) {
            return PinAuthResult.Locked(current.lockedUntilMillis - now)
        }

        val matches = PinHasher.hash(pin, current.pinSalt) == current.pinHash
        if (matches) {
            if (current.failedPinAttempts != 0) {
                userDao.update(current.copy(failedPinAttempts = 0, lockedUntilMillis = 0))
            }
            return PinAuthResult.Success
        }

        val attempts = current.failedPinAttempts + 1
        val lockedUntil = if (attempts >= UserProfile.MAX_PIN_ATTEMPTS) {
            now + UserProfile.LOCKOUT_DURATION_MILLIS
        } else {
            0L
        }
        userDao.update(current.copy(failedPinAttempts = attempts, lockedUntilMillis = lockedUntil))
        return if (lockedUntil > 0) PinAuthResult.Locked(lockedUntil - now) else PinAuthResult.InvalidPin
    }

    suspend fun awardXp(user: UserProfile, amount: Int): UserProfile {
        val updated = user.copy(xp = user.xp + amount)
        userDao.update(updated)
        return updated
    }

    suspend fun resetPin(user: UserProfile, newPin: String): UserProfile {
        val salt = PinHasher.generateSalt()
        val updated = user.copy(
            pinSalt = salt,
            pinHash = PinHasher.hash(newPin, salt),
            failedPinAttempts = 0,
            lockedUntilMillis = 0,
        )
        userDao.update(updated)
        return updated
    }

    /** Admins may not delete themselves out of the last admin seat. */
    suspend fun canDeleteUser(user: UserProfile): Boolean {
        if (user.role != UserRole.ADMIN) return true
        return userDao.adminCount() > 1
    }

    suspend fun deleteUser(user: UserProfile) {
        userDao.delete(user)
    }
}
