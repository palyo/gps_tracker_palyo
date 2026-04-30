package com.post.call.info.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

class Config(val context: Context) {
    val prefs: SharedPreferences = context.getSharedPreferences(PREFS_KEY, 0)

    var enablePostCallScreen: Boolean
        get() = this.prefs.getBoolean(ENABLE_POST_CALL_SCREEN, true)
        set(z) {
            this.prefs.edit { putBoolean(ENABLE_POST_CALL_SCREEN, z) }
        }

    var callCounter: Int
        get() = this.prefs.getInt(PREFS_CALL_COUNTER, 0)
        set(i) {
            this.prefs.edit { putInt(PREFS_CALL_COUNTER, i) }
        }

    var prefs_call_state: Int
        get() = this.prefs.getInt(PREFS_CALL_STATE, 0)
        set(i) {
            this.prefs.edit { putInt(PREFS_CALL_STATE, i) }
        }

    var prefs_call_incoming: Boolean
        get() = this.prefs.getBoolean(PREFS_CALL_INCOMING, false)
        set(z) {
            this.prefs.edit { putBoolean(PREFS_CALL_INCOMING, z) }
        }

    var prefs_call_outgoing: Boolean
        get() = this.prefs.getBoolean(PREFS_CALL_OUTGOING, false)
        set(z) {
            this.prefs.edit { putBoolean(PREFS_CALL_OUTGOING, z) }
        }

    var prefs_start_call_timer: Long
        get() = this.prefs.getLong(PREFS_START_CALL_TIMER, 0L)
        set(j) {
            this.prefs.edit { putLong(PREFS_START_CALL_TIMER, j) }
        }
}
