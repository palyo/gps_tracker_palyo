package aanibrothers.tracker.io.extension

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.core.content.ContextCompat
import coder.apps.space.library.helper.TinyDB


val LOCATION_PERMISSION =
    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)

val CAMERA_PERMISSION = arrayOf(Manifest.permission.CAMERA)



fun Context.hasLocationPermissions(): Boolean {
    return LOCATION_PERMISSION.all {
        ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
    }
}

fun Context.hasCameraPermissions(): Boolean {
    return CAMERA_PERMISSION.all {
        ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
    }
}


/**
 * The app writes only its own files into shared storage, which needs no
 * permission on the supported API range, so this is camera plus location.
 */
fun Context.hasAllNewPermissions(): Boolean {
    return hasLocationPermissions() && hasCameraPermissions()
}

fun Context.isGrantedOverlay(): Boolean {
    return Settings.canDrawOverlays(this)
}

/**
 * Whether the onboarding screen should be shown right now. True only when the
 * Firebase Remote Config kill-switch ([IS_INTRO_REMOTE_ENABLED], cached) is on
 * AND the user has not already completed onboarding ([IS_INTRO_ENABLED]).
 */

fun Context.isLocationEnabled(): Boolean {
    try {
        return Settings.Secure.getInt(contentResolver, "location_mode") != 0
    } catch (e: Settings.SettingNotFoundException) {
        e.printStackTrace()
        return false
    }
}
