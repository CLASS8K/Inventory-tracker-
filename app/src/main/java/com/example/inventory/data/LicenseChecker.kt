package com.example.inventory.data

import android.content.Context
import androidx.core.content.edit
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

enum class LicenseStatus { ACTIVE, WARNING, READ_ONLY }

/**
 * Checks a single Firestore document for this business's subscription status. Deliberately not
 * a general licensing system — one client, one document (`license/status`, field
 * `activeUntilMillis`), updated by hand in the Firebase console after each payment.
 *
 * Never requires a live connection to open the app: the last successfully-fetched due date is
 * cached locally in SharedPreferences and used until a fresher one is fetched, so a bar's
 * data/WiFi being down never by itself blocks them from selling drinks. A device that has never
 * successfully reached Firestore (e.g. offline during first setup) defaults to ACTIVE rather
 * than locking out a fresh install for a reason that isn't the business's fault.
 */
@Singleton
class LicenseChecker @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val prefs get() = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val firestore by lazy { FirebaseFirestore.getInstance() }

    /**
     * Best-effort refresh from Firestore; silently keeps the last cached value on any failure —
     * including before a real Firebase project is wired up (no `google-services.json` yet makes
     * [FirebaseFirestore] throw on first use), which must never crash the app that's actually
     * running the business. [runCatching] covers both that synchronous throw and any exception
     * from the network call itself.
     */
    suspend fun refresh() {
        val activeUntilMillis = runCatching { fetchActiveUntilMillis() }.getOrNull() ?: return
        prefs.edit { putLong(KEY_ACTIVE_UNTIL, activeUntilMillis) }
    }

    fun currentStatus(): LicenseStatus {
        val daysUntilDue = daysUntilDue()
        if (daysUntilDue == Long.MAX_VALUE) return LicenseStatus.ACTIVE
        return when {
            daysUntilDue > WARNING_THRESHOLD_DAYS -> LicenseStatus.ACTIVE
            daysUntilDue > -GRACE_PERIOD_DAYS -> LicenseStatus.WARNING
            else -> LicenseStatus.READ_ONLY
        }
    }

    /** [Long.MAX_VALUE] means "never successfully checked" — treated as far from due. */
    fun daysUntilDue(): Long {
        val activeUntil = prefs.getLong(KEY_ACTIVE_UNTIL, NEVER_CHECKED)
        if (activeUntil == NEVER_CHECKED) return Long.MAX_VALUE
        return (activeUntil - System.currentTimeMillis()) / DAY_MILLIS
    }

    private suspend fun fetchActiveUntilMillis(): Long? = suspendCancellableCoroutine { cont ->
        firestore.collection(COLLECTION).document(DOCUMENT).get()
            .addOnSuccessListener { snapshot ->
                val value = if (snapshot != null && snapshot.exists()) snapshot.getLong(FIELD_ACTIVE_UNTIL) else null
                if (cont.isActive) cont.resume(value)
            }
            .addOnFailureListener {
                if (cont.isActive) cont.resume(null)
            }
    }

    companion object {
        private const val PREFS_NAME = "license_prefs"
        private const val KEY_ACTIVE_UNTIL = "active_until_millis"
        private const val NEVER_CHECKED = -1L
        private const val COLLECTION = "license"
        private const val DOCUMENT = "status"
        private const val FIELD_ACTIVE_UNTIL = "activeUntilMillis"
        private const val DAY_MILLIS = 24 * 60 * 60 * 1000L

        /** Full function starts showing a warning banner this many days before the due date. */
        const val WARNING_THRESHOLD_DAYS = 7L

        /**
         * Days past the due date the app stays fully functional (warning banner only) before
         * going read-only. This is the offline-tolerance buffer: it's not "days since last
         * check," it's slack built into the due date itself, so a bar that's simply offline
         * right around the deadline isn't punished for it.
         */
        const val GRACE_PERIOD_DAYS = 5L
    }
}
