package aanibrothers.tracker.io.extension

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics

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