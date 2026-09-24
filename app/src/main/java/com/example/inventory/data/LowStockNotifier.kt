package com.example.inventory.data

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Posts a local notification the moment an item crosses into low stock — this is a purely
 * on-device alert (no server, no FCM); there is nothing to sync it to besides this device.
 */
@Singleton
class LowStockNotifier @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Low stock alerts",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = "Notifies you when an item drops to or below its low-stock threshold."
            }
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    /** Uses the item's own id as the notification id, so a later update replaces it rather than stacking. */
    fun notifyLowStock(item: InventoryItem) {
        if (!hasPermission()) return
        val unitLabel = item.unit.ifBlank { InventoryItem.DEFAULT_UNIT }
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_warning)
            .setContentTitle("${item.name} is low on stock")
            .setContentText(
                "$unitLabel(s) left: ${item.quantity} — at or below the threshold of ${item.lowStockThreshold}.",
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(notificationId(item.id), notification)
    }

    /** Called when a fix (e.g. undoing a bad stock take) takes an item back out of low stock. */
    fun cancelLowStock(itemId: Long) {
        NotificationManagerCompat.from(context).cancel(notificationId(itemId))
    }

    /**
     * Long.hashCode() folds the high and low 32 bits together (id xor (id ushr 32)) rather than
     * truncating to the low bits like a raw toInt() would — spreads the id across the full Int
     * range instead of just wrapping once ids exceed Int.MAX_VALUE, so two different items are
     * far less likely to collide on the same notification id.
     */
    private fun notificationId(itemId: Long): Int = itemId.hashCode()

    private fun hasPermission(): Boolean {
        val runtimeGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        return runtimeGranted && NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    companion object {
        private const val CHANNEL_ID = "low_stock"
    }
}
