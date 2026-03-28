package aanibrothers.tracker.io.afterCall.receiver

import aanibrothers.tracker.io.App
import aanibrothers.tracker.io.R
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.WindowManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import aanibrothers.tracker.io.afterCall.extensions.CALL_COUNTER
import aanibrothers.tracker.io.afterCall.extensions.CALL_TIME
import aanibrothers.tracker.io.afterCall.extensions.CALL_TYPE
import aanibrothers.tracker.io.afterCall.extensions.END_TIME
import aanibrothers.tracker.io.afterCall.extensions.EXTRA_MOBILE_NUMBER
import aanibrothers.tracker.io.afterCall.extensions.IS_OPEN_FROM_NOTIFICATION
import aanibrothers.tracker.io.afterCall.extensions.START_TIME
import aanibrothers.tracker.io.afterCall.extensions.callCounter
import aanibrothers.tracker.io.afterCall.extensions.prefs_call_incoming
import aanibrothers.tracker.io.afterCall.extensions.prefs_call_outgoing
import aanibrothers.tracker.io.afterCall.extensions.prefs_call_state
import aanibrothers.tracker.io.afterCall.extensions.prefs_start_call_timer
import aanibrothers.tracker.io.afterCall.service.ForegroundService
import aanibrothers.tracker.io.afterCall.ui.activity.PostCallActivity
import com.contact.phone.dailer.postCall.receiver.PhoneCallReceiver
import java.util.Date

class CallReceiver : PhoneCallReceiver() {
    override fun onIncomingCallReceived(context: Context, str: String?, date: Date) {
        Log.e("PostCall", "onIncomingCallReceived: ")
    }

    override fun onIncomingCallAnswered(context: Context, str: String?, date: Date) {
        Log.e("PostCall", "onIncomingCallAnswered: $date")
    }

    override fun onIncomingCallEnded(context: Context, str: String?, date: Date, date2: Date) {
        Log.e("PostCall", "onIncomingCallEnded: ")
        openNewActivity(context, str, date, date2, context.getString(R.string.label_incoming_call))
    }

    override fun onOutgoingCallStarted(ctx: Context, str: String?, date: Date) {
        Log.e("PostCall", "onOutgoingCallStarted: $date")
    }

    override fun onOutgoingCallEnded(context: Context, str: String?, date: Date, date2: Date) {
        Log.e("PostCall", "onOutgoingCallEnded: ")
        openNewActivity(context, str, date, date2, context.getString(R.string.label_outgoing_call))
    }

    override fun onMissedCall(context: Context, str: String?, date: Date) {
        Log.e("PostCall", "onMissedCall: ")
        openNewActivity(context, str, date, Date(), context.getString(R.string.label_missed_call))
    }

    private fun openNewActivity(
        context: Context,
        str: String?,
        date: Date,
        date2: Date,
        callType: String
    ) {
        val number = if (str.isNullOrEmpty()) "" else str
        val checkPermission = checkPermission()
        val isEnablePostCallScreen = isEnablePostCallScreen()
        val isShowPostCallScreen = isShowPostCallScreen()
        val hasStartTime = true
        val hasEndTime = true
        Log.e(
            "PostCall",
            "openPostCallActivity checkPermission-->> $checkPermission  isEnablePostCallScreen-> $isEnablePostCallScreen " +
                    "isShowPostCallScreen-> $isShowPostCallScreen str--> $number START_TIME $hasStartTime END_TIME $hasEndTime  CALL_TYPE $callType"
        )
        PostCallActivity.getPostCallActivity()?.finishActivity()
        setCallEnded(true)
        stopService(context)
        if (isEnablePostCallScreen() && isShowPostCallScreen()) {
            val callCounter = context.callCounter
            val intent = Intent(context, PostCallActivity::class.java)
            intent.putExtra(EXTRA_MOBILE_NUMBER, number)
            intent.putExtra(CALL_TIME, date)
            intent.putExtra(START_TIME, date.time)
            intent.putExtra(END_TIME, date2.time)
            intent.putExtra(CALL_TYPE, callType)
            intent.putExtra(CALL_COUNTER, callCounter)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.prefs_call_state = callCounter + 1
            context.prefs_start_call_timer = 0L
            context.prefs_call_state = -1
            context.prefs_call_incoming = false
            context.prefs_call_outgoing = false
            if (checkPermission()) {
                App.isOpenInter = true
                intent.putExtra(IS_OPEN_FROM_NOTIFICATION, false)
                if (ForegroundService.myServiceIsRunning) {
                    context.startActivity(intent)
                } else {
                    val windowManager =
                        context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
                    val view = View(context)
                    view.setBackgroundColor(0)
                    windowManager.addView(
                        view,
                        WindowManager.LayoutParams(
                            1,
                            1,
                            2038,
                            24,
                            -3
                        )
                    )
                    Handler(Looper.getMainLooper()).postDelayed({
                        context.startActivity(intent)
                        windowManager.removeView(view)
                    }, 200L)
                }
            } else {
                intent.putExtra(IS_OPEN_FROM_NOTIFICATION, true)
                showPostCallNotification(context, intent)
                setIncomingCall(false)
                setOutgoingCall(false)
                setLastState(-1)
                return
            }
        }
        setIncomingCall(false)
        setOutgoingCall(false)
        setLastState(-1)
    }

    private fun showPostCallNotification(context: Context, intent: Intent) {
        Log.e("PostCall", "showPostCallNotification")
        try {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    "android.permission.POST_NOTIFICATIONS"
                ) == 0
            ) {
                NotificationManagerCompat.from(context).cancelAll()
            }
        } catch (ignored: Exception) {
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val activity = if (Build.VERSION.SDK_INT >= 31) {
            PendingIntent.getActivity(context, System.currentTimeMillis().toInt(), intent, 67108866)
        } else {
            PendingIntent.getActivity(
                context,
                System.currentTimeMillis().toInt(),
                intent,
                134217728
            )
        }
        val from = NotificationManagerCompat.from(context)
        val autoCancel = NotificationCompat.Builder(context, CHANNEL_ID_POST_CALL)
            .setContentTitle(context.getString(R.string.notification_post_call_info_title))
            .setSmallIcon(R.drawable.post_ic_notification_phone_call)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setPriority(2)
            .setWhen(System.currentTimeMillis())
            .setDefaults(1)
            .setAutoCancel(true)
        autoCancel.setVibrate(longArrayOf())
        autoCancel.setChannelId(CHANNEL_ID_POST_CALL)
        val channel = NotificationChannel(
            CHANNEL_ID_POST_CALL,
            context.getString(R.string.channel_post_call_name),
            NotificationManager.IMPORTANCE_HIGH
        )
        channel.description = context.getString(R.string.channel_post_call_description)
        channel.lockscreenVisibility = 1
        channel.enableVibration(true)
        channel.setShowBadge(false)
        from.createNotificationChannel(channel)
        if (ActivityCompat.checkSelfPermission(
                context,
                "android.permission.POST_NOTIFICATIONS"
            ) != 0
        ) {
            return
        }
        if (isFullScreenGranted(context)) {
            autoCancel.setFullScreenIntent(activity, true)
        } else {
            autoCancel.setContentIntent(activity)
        }
        val isFullScreenGranted = isFullScreenGranted(context)
        Log.e("PostCall", "isFullScreenGranted-> $isFullScreenGranted")
        from.notify(System.currentTimeMillis().toInt(), autoCancel.build())
    }

    fun isFullScreenGranted(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= 34) {
            (context.getSystemService(NotificationManager::class.java)).canUseFullScreenIntent()
        } else {
            true
        }
    }

    companion object {
        private const val CHANNEL_ID_POST_CALL = "PostCall"
    }
}
