package aanibrothers.tracker.io.extension

import android.*
import android.app.*
import android.app.role.*
import android.content.*
import android.content.pm.*
import android.os.*
import android.os.Build.VERSION.SDK_INT
import android.provider.*
import androidx.core.app.*

val LOCATION_PERMISSION = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)

fun Activity.hasPermissions(permissions: Array<String>): Boolean = permissions.all { ActivityCompat.checkSelfPermission(applicationContext, it) == PackageManager.PERMISSION_GRANTED }

fun Activity.hasOverlayPermission(): Boolean {
    return Settings.canDrawOverlays(this)
}

fun Context.isLocationEnabled(): Boolean {
    try {
        return Settings.Secure.getInt(contentResolver, "location_mode") != 0
    } catch (e: Settings.SettingNotFoundException) {
        e.printStackTrace()
        return false
    }
}