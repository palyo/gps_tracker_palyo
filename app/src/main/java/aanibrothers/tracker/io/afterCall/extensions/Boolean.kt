package aanibrothers.tracker.io.afterCall.extensions

import android.content.Context
import android.os.Build
import androidx.core.content.ContextCompat
import coder.apps.space.library.helper.TinyDB

fun isBannerLoad(i: Int): Boolean {
    return true
}

fun isNativeLoad(i: Int): Boolean {
    return false
}

fun getBaseConfig(context: Context): TinyDB {
    return TinyDB(context)
}

fun checkNotificationPermission(context: Context): Boolean {
    return Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(
        context,
        "android.permission.POST_NOTIFICATIONS"
    ) == 0
}
