package com.post.call.info.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import androidx.core.content.ContextCompat
import com.google.firebase.analytics.FirebaseAnalytics
import com.ironsource.fa


const val INCOMING = "Incoming call"
const val OUTGOING = "Outgoing call"
const val MISSED_CALL = "Missed call"
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


fun isBannerLoad(i: Int): Boolean = true

fun isNativeLoad(i: Int): Boolean = false

fun getBaseConfig(context: Context): Config = Config(context)

fun isDebug() : Boolean{
    return false
}

fun checkNotificationPermission(context: Context): Boolean =
    Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, "android.permission.POST_NOTIFICATIONS") == 0

fun Context.isNetworkOn(): Boolean {
    val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
    val nc = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
    return nc.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
            nc.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
            nc.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
}

fun Context.firebaseASOEvent(eventName: String, params: Map<String, Any?> = emptyMap()) {
    val bundle = Bundle()
    params.forEach { (key, value) ->
        when (value) {
            is String -> bundle.putString(key, value)
            is Int -> bundle.putInt(key, value)
            is Double -> bundle.putDouble(key, value)
            is Long -> bundle.putLong(key, value)
        }
    }
    FirebaseAnalytics.getInstance(this).logEvent(eventName, bundle)
}