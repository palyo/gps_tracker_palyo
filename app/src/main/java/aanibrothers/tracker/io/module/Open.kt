package aanibrothers.tracker.io.module

import aanibrothers.tracker.io.*
import aanibrothers.tracker.io.App.Companion.appOpenManager
import android.app.*
import android.content.*
import android.graphics.drawable.*
import android.view.*

fun Context.viewAppOpen(isWait: Boolean = false, isDialogShown: Boolean = false, listener: (() -> Unit)?) {
    if (appOpenManager == null || isPremium) {
        listener?.invoke()
        return
    }
    appOpenManager?.showAdIfAvailable(isWait = isWait, isDialogShown = isDialogShown) {
        if (it) {
            listener?.invoke()
        } else {
            if (!AppOpenManager.isShowingAd) {
                if (App.currentActivity != null) {
                    AppOpenManager.isShowingAd = false
                    appOpenManager?.loadOpen()
                    listener?.invoke()
                } else {
                    listener?.invoke()
                }
            } else {
                listener?.invoke()
            }
        }
    }
}

fun showProgressDialog(activity: Activity): Dialog {
    val dialog = Dialog(activity, coder.apps.space.library.R.style.Theme_Space_Dialog)
    dialog.setCancelable(false)
    dialog.setContentView(R.layout.lib_dialog_progress)
    dialog.window?.apply {
        setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        statusBarColor = activity.getColor(R.color.colorPrimary)
        setBackgroundDrawable(ColorDrawable(activity.getColor(R.color.colorPrimary)))
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