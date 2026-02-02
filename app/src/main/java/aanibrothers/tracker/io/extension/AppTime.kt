package aanibrothers.tracker.io.extension

import android.content.Context

fun Context.saveLastFunctionCallTime() {
    tinyDb.putLong("lastFunctionCallTime", System.currentTimeMillis())
}

fun Context.shouldCallFunction(): Boolean {
    val lastCallTime = tinyDb.getLong("lastFunctionCallTime", 0L)
    val currentTime = System.currentTimeMillis()
    val oneWeekMillis = 7 * 24 * 60 * 60 * 1000L
    val b = (currentTime - lastCallTime) >= oneWeekMillis
    return b
}