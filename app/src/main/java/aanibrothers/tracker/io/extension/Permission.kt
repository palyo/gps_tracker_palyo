package aanibrothers.tracker.io.extension

import android.Manifest
import android.content.Context
import android.os.Build
import android.provider.Settings

val LOCATION_PERMISSION = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)

val SCREEN_PERMISSION = arrayOf(
    Manifest.permission.CAMERA,
    Manifest.permission.ACCESS_FINE_LOCATION,
    Manifest.permission.ACCESS_COARSE_LOCATION,
    *storagePermissions()
)

val AFTER_CALL_PERMISSION = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
    arrayOf(Manifest.permission.READ_PHONE_STATE, Manifest.permission.POST_NOTIFICATIONS)
} else {
    arrayOf(Manifest.permission.READ_PHONE_STATE)
}

private fun storagePermissions(): Array<String> {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        emptyArray()
    } else {
        arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    }
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