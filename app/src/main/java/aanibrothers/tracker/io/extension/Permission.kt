package aanibrothers.tracker.io.extension

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat

val LOCATION_PERMISSION = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)

val AFTER_CALL_PERMISSION = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
    arrayOf(Manifest.permission.READ_PHONE_STATE, Manifest.permission.POST_NOTIFICATIONS)
} else {
    arrayOf(Manifest.permission.READ_PHONE_STATE)
}

fun storagePermissions(): Array<String> {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        emptyArray()
    } else {
        arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    }
}

fun Context.hasAfterCallPermissions(): Boolean {
    return AFTER_CALL_PERMISSION.all { permission ->
        ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
    }
}

fun Context.hasRequiredAppPermissions(): Boolean {
    return hasAfterCallPermissions() && isGrantedOverlay()
}

fun Context.isGrantedOverlay(): Boolean {
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
