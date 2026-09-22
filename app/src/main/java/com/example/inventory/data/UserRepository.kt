package com.example.inventory.data

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

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

    fun authenticate(user: UserProfile, pin: String): Boolean =
        PinHasher.hash(pin, user.pinSalt) == user.pinHash

    suspend fun awardXp(user: UserProfile, amount: Int): UserProfile {
        val updated = user.copy(xp = user.xp + amount)
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
