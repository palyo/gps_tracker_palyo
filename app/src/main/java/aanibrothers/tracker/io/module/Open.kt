package aanibrothers.tracker.io.module

import aanibrothers.tracker.io.*
import aanibrothers.tracker.io.App.Companion.appOpenManager
import android.app.*
import android.content.*
import android.graphics.drawable.*
import android.view.*
import androidx.core.graphics.drawable.toDrawable

fun Context.viewAppOpen(isWait: Boolean = false, isDialogShown: Boolean = false, listener: (() -> Unit)?) {
    val manager = appOpenManager
    if (manager == null) {
        listener?.invoke()
        return
    }
    manager.showAdIfAvailable(isWait = isWait, isDialogShown = isDialogShown) {
        // Whether the ad showed or not, hand control back to the caller.
        // Manager handles its own next-load triggering on dismiss/fail —
        // no longer chain another loadOpen() here, which used to cause
        // multiple parallel load requests.
        listener?.invoke()
    }
}

fun showProgressDialog(activity: Activity): Dialog {
    val dialog = Dialog(activity, coder.apps.space.library.R.style.Theme_Space_Dialog)
    dialog.setCancelable(false)
    dialog.setContentView(R.layout.lib_dialog_progress)
    dialog.window?.apply {
        setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        statusBarColor = activity.getColor(R.color.colorPrimary)
        setBackgroundDrawable(activity.getColor(R.color.colorPrimary).toDrawable())
        setDimAmount(0.0F)
    }
    try {
        if (!activity.isFinishing) {
            dialog.show()
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return dialog
}