package com.example.inventory.data

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Best-effort live feed of business activity (stock takes and sales) to Firestore, so an
 * owner can watch a dashboard and see what's happening without opening the app themselves.
 * Never blocks or fails the real action on a network problem: every write is fire-and-forget
 * and silently dropped on failure, same posture as [LicenseChecker].
 *
 * Sales carry [revenue]/[profit] — real money figures — so unlike the license/device data,
 * the dashboard that reads this collection is meant to sit behind a login (see
 * dashboard/FIRESTORE_RULES.md): the app can always *write* an event with no auth, same as
 * today, but only a signed-in owner can *read* the collection back.
 */
@Singleton
class ActivityReporter @Inject constructor(
    private val licenseChecker: LicenseChecker,
) {
    private val firestore by lazy { FirebaseFirestore.getInstance() }

    suspend fun reportStockTake(
        itemName: String,
        detail: String,
        actorName: String,
        revenue: Double? = null,
        profit: Double? = null,
    ) {
        runCatching { push(TYPE_STOCK_TAKE, itemName, detail, actorName, revenue = revenue, profit = profit) }
    }

    suspend fun reportSale(itemName: String, detail: String, actorName: String, revenue: Double, profit: Double) {
        runCatching { push(TYPE_SALE, itemName, detail, actorName, revenue = revenue, profit = profit) }
    }

    private suspend fun push(
        type: String,
        itemName: String,
        detail: String,
        actorName: String,
        revenue: Double?,
        profit: Double?,
    ): Unit = suspendCancellableCoroutine { cont ->
        val businessName = licenseChecker.businessName().orEmpty()
        val event = buildMap {
            put(FIELD_TYPE, type)
            put(FIELD_BUSINESS_NAME, businessName)
            put(FIELD_BUSINESS_NAME_NORMALIZED, normalizeBusinessName(businessName))
            put(FIELD_DEVICE_ID, licenseChecker.deviceId)
            put(FIELD_ITEM_NAME, itemName)
            put(FIELD_DETAIL, detail)
            put(FIELD_ACTOR_NAME, actorName)
            put(FIELD_TIMESTAMP, FieldValue.serverTimestamp())
            if (revenue != null) put(FIELD_REVENUE, revenue)
            if (profit != null) put(FIELD_PROFIT, profit)
        }
        firestore.collection(COLLECTION).add(event)
            .addOnSuccessListener { if (cont.isActive) cont.resume(Unit) }
            .addOnFailureListener { if (cont.isActive) cont.resume(Unit) }
    }

    companion object {
        private fun normalizeBusinessName(name: String): String = name.trim().lowercase()

        private const val COLLECTION = "businessEvents"
        const val TYPE_STOCK_TAKE = "stock_take"
        const val TYPE_SALE = "sale"
        private const val FIELD_TYPE = "type"
        private const val FIELD_BUSINESS_NAME = "businessName"
        private const val FIELD_BUSINESS_NAME_NORMALIZED = "businessNameNormalized"
        private const val FIELD_DEVICE_ID = "deviceId"
        private const val FIELD_ITEM_NAME = "itemName"
        private const val FIELD_DETAIL = "detail"
        private const val FIELD_ACTOR_NAME = "actorName"
        private const val FIELD_TIMESTAMP = "timestamp"
        private const val FIELD_REVENUE = "revenue"
        private const val FIELD_PROFIT = "profit"
    }
}
