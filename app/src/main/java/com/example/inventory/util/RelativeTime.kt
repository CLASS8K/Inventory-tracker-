package com.example.inventory.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/** True if [timestamp] falls on the same calendar day as [now], in the device's local timezone. */
fun isSameDay(timestamp: Long, now: Long = System.currentTimeMillis()): Boolean {
    val a = Calendar.getInstance().apply { timeInMillis = timestamp }
    val b = Calendar.getInstance().apply { timeInMillis = now }
    return a.get(Calendar.YEAR) == b.get(Calendar.YEAR) && a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)
}

/** Formats a timestamp relative to now, falling back to a date for anything older than a week. */
fun formatRelativeTime(timestamp: Long, now: Long = System.currentTimeMillis()): String {
    val diffMs = (now - timestamp).coerceAtLeast(0)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(diffMs)
    val hours = TimeUnit.MILLISECONDS.toHours(diffMs)
    val days = TimeUnit.MILLISECONDS.toDays(diffMs)
    return when {
        minutes < 1 -> "Just now"
        minutes < 60 -> "${minutes}m ago"
        hours < 24 -> "${hours}h ago"
        days < 7 -> "${days}d ago"
        else -> SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date(timestamp))
    }
}
