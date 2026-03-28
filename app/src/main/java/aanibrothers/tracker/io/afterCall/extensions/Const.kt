package aanibrothers.tracker.io.afterCall.extensions

import android.content.Context
import coder.apps.space.library.helper.TinyDB

const val CALL_TIME = "call_time"
const val CALL_TYPE = "call_type"
const val EXTRA_MOBILE_NUMBER = "EXTRA_MOBILE_NUMBER"
const val START_TIME = "START_TIME"
const val END_TIME = "END_TIME"
const val CALL_COUNTER = "CALL_COUNTER"
const val ENABLE_POST_CALL_SCREEN = "enable_post_call_screen"
const val PREFS_CALL_COUNTER = "prefs_call_counter"
const val PREFS_CALL_INCOMING = "prefs_call_incoming"
const val PREFS_CALL_OUTGOING = "prefs_call_outgoing"
const val PREFS_CALL_STATE = "prefs_call_state"
const val PREFS_KEY = "Prefs"
const val PREFS_START_CALL_TIMER = "prefs_start_call_timer"
const val IS_OPEN_FROM_NOTIFICATION = "isOpenFromNotification"


var Context.enablePostCallScreen: Boolean
    get() = TinyDB(this).getBoolean(ENABLE_POST_CALL_SCREEN, true)
    set(value) = TinyDB(this).putBoolean(ENABLE_POST_CALL_SCREEN, value)


var Context.callCounter: Int
    get() = TinyDB(this).getInt(PREFS_CALL_COUNTER, 0)
    set(value) = TinyDB(this).putInt(PREFS_CALL_COUNTER, value)

var Context.prefs_call_state: Int
    get() = TinyDB(this).getInt(PREFS_CALL_STATE, 0)
    set(value) = TinyDB(this).putInt(PREFS_CALL_STATE, value)

var Context.prefs_call_incoming: Boolean
    get() = TinyDB(this).getBoolean(PREFS_CALL_INCOMING, false)
    set(value) = TinyDB(this).putBoolean(PREFS_CALL_INCOMING, value)

var Context.prefs_call_outgoing: Boolean
    get() = TinyDB(this).getBoolean(PREFS_CALL_OUTGOING, false)
    set(value) = TinyDB(this).putBoolean(PREFS_CALL_OUTGOING, value)

var Context.prefs_start_call_timer: Long
    get() = TinyDB(this).getLong(PREFS_START_CALL_TIMER, 0L)
    set(value) = TinyDB(this).putLong(PREFS_START_CALL_TIMER, value)
