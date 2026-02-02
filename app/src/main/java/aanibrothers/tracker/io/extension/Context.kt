package aanibrothers.tracker.io.extension

import android.content.Context
import android.os.Build
import android.view.View
import android.view.Window
import androidx.core.view.WindowInsetsControllerCompat
import coder.apps.space.library.helper.TinyDB

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