package aanibrothers.tracker.io.analytics

import android.content.Context
import androidx.core.content.edit

/**
 * Tracks lifetime captures and reports the bucketed value as a user property.
 * Bucketing avoids high-cardinality user-property values (Firebase recommends ≤500 distinct).
 */
object CaptureCounter {

    private const val PREFS = "analytics_capture_counter"
    private const val KEY_COUNT = "lifetime_captures"

    fun bumpAndReport(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val next = prefs.getInt(KEY_COUNT, 0) + 1
        prefs.edit { putInt(KEY_COUNT, next) }
        Analytics.setProperty(UserProp.LIFETIME_CAPTURE_BUCKET, bucketFor(next))
        Analytics.setProperty(UserProp.USER_STAGE, stageFor(next))
    }

    fun reportCurrent(context: Context) {
        val count = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt(KEY_COUNT, 0)
        Analytics.setProperty(UserProp.LIFETIME_CAPTURE_BUCKET, bucketFor(count))
        Analytics.setProperty(UserProp.USER_STAGE, stageFor(count))
    }

    private fun bucketFor(count: Int): String = when {
        count <= 0 -> "0"
        count <= 5 -> "1_5"
        count <= 20 -> "6_20"
        count <= 50 -> "21_50"
        else -> "50_plus"
    }

    private fun stageFor(count: Int): String = when {
        count == 0 -> "new"
        count < 6 -> "activated"
        else -> "engaged"
    }
}
