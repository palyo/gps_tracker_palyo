package aanibrothers.tracker.io.afterCall

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build

class HomeWatcher(context: Context) {
    private val appContext: Context = context
    private val filter: IntentFilter = IntentFilter(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)
    private var listener: OnHomePressedListener? = null
    private var receiver: InnerRecevier? = InnerRecevier()

    interface OnHomePressedListener {
        fun onHomePressed()
        fun onHomeLongPressed()
    }

    fun setOnHomePressedListener(listener: OnHomePressedListener) {
        this.listener = listener
    }

    fun startWatch() {
        val currentReceiver = receiver ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            appContext.registerReceiver(currentReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            appContext.registerReceiver(currentReceiver, filter)
        }
    }

    fun stopWatch() {
        val currentReceiver = receiver ?: return
        appContext.unregisterReceiver(currentReceiver)
    }

    inner class InnerRecevier : BroadcastReceiver() {
        private val systemDialogReasonKey = "reason"
        private val systemDialogReasonRecentApps = "recentapps"
        private val systemDialogReasonHomeKey = "homekey"

        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (Intent.ACTION_CLOSE_SYSTEM_DIALOGS == action) {
                val reason = intent.getStringExtra(systemDialogReasonKey)
                val currentListener = listener
                if (reason != null && currentListener != null) {
                    if (reason == systemDialogReasonHomeKey) {
                        currentListener.onHomePressed()
                    } else if (reason == systemDialogReasonRecentApps) {
                        currentListener.onHomeLongPressed()
                    }
                }
            }
        }
    }
}
