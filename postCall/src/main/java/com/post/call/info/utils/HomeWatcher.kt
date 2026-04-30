package com.post.call.info.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build

class HomeWatcher(private val mContext: Context) {

    private val mFilter = IntentFilter(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)
    private var mListener: OnHomePressedListener? = null
    private val mReceiver = InnerReceiver()

    interface OnHomePressedListener {
        fun onHomePressed()
        fun onHomeLongPressed()
    }

    fun setOnHomePressedListener(listener: OnHomePressedListener) {
        mListener = listener
    }

    fun startWatch() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            mContext.registerReceiver(mReceiver, mFilter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            mContext.registerReceiver(mReceiver, mFilter)
        }
    }

    fun stopWatch() {
        mContext.unregisterReceiver(mReceiver)
    }

    inner class InnerReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_CLOSE_SYSTEM_DIALOGS) {
                val reason = intent.getStringExtra("reason") ?: return
                when (reason) {
                    "homekey" -> mListener?.onHomePressed()
                    "recentapps" -> mListener?.onHomeLongPressed()
                }
            }
        }
    }
}
