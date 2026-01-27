package aanibrothers.tracker.io.caller

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import coder.apps.space.library.helper.TinyDB
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

var callStartTime: Long = 0L
var callTime: String = "00:00"
var callEndTime = "00:00"
const val START_STATE = "start"
const val STATE = "state"

class PhoneReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == TelephonyManager.ACTION_PHONE_STATE_CHANGED) {
            val callState = (context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager).callState
            when (callState) {
                TelephonyManager.CALL_STATE_RINGING -> {
                    callStartTime = System.currentTimeMillis()
                    callTime = getFormattedTime()
                }

                TelephonyManager.CALL_STATE_OFFHOOK -> {
                    callStartTime = System.currentTimeMillis()
                    callTime = getFormattedTime()
                }

                TelephonyManager.CALL_STATE_IDLE -> {
                    val endTime = System.currentTimeMillis()
                    val durationInSeconds = ((endTime - callStartTime) / 1000)
                    callEndTime = durationInSeconds.getFormattedDuration()
                    val showAfterCall = TinyDB(context).getBoolean("ShowAfterCall", true)
                    if (showAfterCall) {
                        val intentCall = Intent(context, AfterCallActivity::class.java).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            addFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT)
                            addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
                        }
                        context.startActivity(intentCall)
                    }
                }
            }
        }
    }

    fun Long.getFormattedDuration(): String {
        val totalSeconds = this / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60

        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }

    fun getFormattedTime(): String {
        val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
        return sdf.format(Date(System.currentTimeMillis()))
    }
}
