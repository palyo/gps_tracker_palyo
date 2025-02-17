package aanibrothers.tracker.io.extension

import android.content.*
import android.net.*
import android.os.*
import android.view.*
import androidx.core.view.*
import coder.apps.space.library.helper.*

val Context.tinyDb: TinyDB
    get() = TinyDB(this)

fun Window.updateStatusBarIcons(isLight: Boolean) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        WindowInsetsControllerCompat(this, decorView).apply {
            isAppearanceLightStatusBars = isLight
        }
    } else {
        decorView.systemUiVisibility = if (isLight) {
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        } else {
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        }
    }

    decorView.post {
        decorView.requestApplyInsets()
    }
}

fun Context.isNetworkAvailable(): Boolean {
    val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val activeNetwork: NetworkInfo? = connectivityManager.activeNetworkInfo

    return activeNetwork != null && activeNetwork.isConnectedOrConnecting
}
