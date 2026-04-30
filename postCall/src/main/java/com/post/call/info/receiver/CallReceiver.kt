package com.post.call.info.receiver

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
import com.post.call.info.service.PostCall
import com.post.call.info.R
import com.post.call.info.service.ForegroundService
import com.post.call.info.ui.activity.PostCallActivity
import com.post.call.info.utils.CALL_COUNTER
import com.post.call.info.utils.CALL_TIME
import com.post.call.info.utils.CALL_TYPE
import com.post.call.info.utils.END_TIME
import com.post.call.info.utils.EXTRA_MOBILE_NUMBER
import com.post.call.info.utils.INCOMING
import com.post.call.info.utils.IS_OPEN_FROM_NOTIFICATION
import com.post.call.info.utils.MISSED_CALL
import com.post.call.info.utils.OUTGOING
import com.post.call.info.utils.START_TIME
import com.post.call.info.utils.getBaseConfig
import java.util.Date

class CallReceiver : PhoneCallReceiver() {

    override fun onIncomingCallReceived(context: Context, str: String?, date: Date?) {
        Log.e("PostCall", "onIncomingCallReceived: ")
    }

    override fun onIncomingCallAnswered(context: Context, str: String?, date: Date?) {
        Log.e("PostCall", "onIncomingCallAnswered: $date")
    }

    override fun onIncomingCallEnded(context: Context, str: String?, date: Date?, date2: Date?) {
        Log.e("PostCall", "onIncomingCallEnded: ")
        openNewActivity(context, str, date, date2, INCOMING)
    }

    override fun onOutgoingCallStarted(ctx: Context, str: String?, date: Date?) {
        Log.e("PostCall", "onOutgoingCallStarted: $date")
    }

    override fun onOutgoingCallEnded(context: Context, str: String?, date: Date?, date2: Date?) {
        Log.e("PostCall", "onOutgoingCallEnded: ")
        openNewActivity(context, str, date, date2, OUTGOING)
    }

    override fun onMissedCall(context: Context, str: String?, date: Date?) {
        Log.e("PostCall", "onMissedCall: ")
        openNewActivity(context, str, date, Date(), MISSED_CALL)
    }

    private fun openNewActivity(context: Context, str: String?, date: Date?, date2: Date?, callType: String) {
        val phoneNumber = if (str.isNullOrEmpty()) "" else str
        val checkPermission = checkPermission()
        val isEnablePostCallScreen = isEnablePostCallScreen()
        val isShowPostCallScreen = isShowPostCallScreen()
        Log.e("PostCall", "openPostCallActivity checkPermission-->> $checkPermission isEnablePostCallScreen-> $isEnablePostCallScreen isShowPostCallScreen-> $isShowPostCallScreen str--> $phoneNumber START_TIME ${date != null} END_TIME ${date2 != null} CALL_TYPE $callType")

        PostCallActivity.getPostCallActivity()?.finishActivity()
        PhoneCallReceiver.setCallEnded(true)
        context.let { stopService(it) }

        if (isEnablePostCallScreen() && isShowPostCallScreen()) {
            val callCounter = getBaseConfig(context).callCounter
            val intent = Intent(context, PostCallActivity::class.java).apply {
                putExtra(EXTRA_MOBILE_NUMBER, phoneNumber)
                putExtra(CALL_TIME, date)
                putExtra(START_TIME, date!!.time)
                putExtra(END_TIME, date2!!.time)
                putExtra(CALL_TYPE, callType)
                putExtra(CALL_COUNTER, callCounter)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            getBaseConfig(context).prefs_call_state = callCounter + 1
            getBaseConfig(context).prefs_start_call_timer = 0L
            getBaseConfig(context).prefs_call_state = -1
            getBaseConfig(context).prefs_call_incoming = false
            getBaseConfig(context).prefs_call_outgoing = false

            if (checkPermission()) {
                intent.putExtra(IS_OPEN_FROM_NOTIFICATION, false)
                if (ForegroundService.myServiceIsRunning) {
                    try {
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        Log.e("PostCall", "startActivity failed: ${e.message}")
                        // Fall back to notification path so user still sees the entry.
                        intent.putExtra(IS_OPEN_FROM_NOTIFICATION, true)
                        showPostCallNotification(context, intent)
                    }
                } else {
                    // Hack: add a 1x1 system overlay so Android allows the
                    // pending startActivity from this background context.
                    // This can fail with "InputChannel is not initialized" on
                    // devices where the overlay permission is silently
                    // restricted (some OEMs, work profiles, recent Android
                    // versions). Wrap in try/catch and fall back to a
                    // notification so the receiver never crashes.
                    val windowManager = try {
                        context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
                    } catch (e: Exception) {
                        null
                    }
                    val dummyView = View(context).apply { setBackgroundColor(0) }
                    val layoutType = if (Build.VERSION.SDK_INT >= 26) 2038 else 2002
                    var dummyAdded = false
                    try {
                        windowManager?.addView(
                            dummyView,
                            WindowManager.LayoutParams(1, 1, layoutType, 24, -3)
                        )
                        dummyAdded = true
                    } catch (e: Exception) {
                        Log.e("PostCall", "overlay addView failed: ${e.message}")
                    }

                    if (dummyAdded) {
                        Handler(Looper.getMainLooper()).postDelayed({
                            try {
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Log.e("PostCall", "delayed startActivity failed: ${e.message}")
                            }
                            try {
                                windowManager?.removeView(dummyView)
                            } catch (_: Exception) {
                                // View may already be detached on some devices.
                            }
                        }, 200L)
                    } else {
                        // Overlay path unavailable — try a direct activity start
                        // (works on older Android / when the activity is in the
                        // foreground). If that throws, fall back to a
                        // notification so we never crash the receiver.
                        try {
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Log.e("PostCall", "fallback startActivity failed: ${e.message}")
                            intent.putExtra(IS_OPEN_FROM_NOTIFICATION, true)
                            showPostCallNotification(context, intent)
                        }
                    }
                }
            } else {
                intent.putExtra(IS_OPEN_FROM_NOTIFICATION, true)
                showPostCallNotification(context, intent)
                setIncomingCall(false)
                setOutgoingCall(false)
                setLastState(-1)
                PostCall.INSTANCE.startPostCall(context)
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
            if (ActivityCompat.checkSelfPermission(context, "android.permission.POST_NOTIFICATIONS") == 0) {
                NotificationManagerCompat.from(context).cancelAll()
            }
        } catch (ignored: Exception) {}

        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent = if (Build.VERSION.SDK_INT >= 31) {
            PendingIntent.getActivity(context, System.currentTimeMillis().toInt(), intent, 67108866)
        } else {
            PendingIntent.getActivity(context, System.currentTimeMillis().toInt(), intent, 134217728)
        }

        val notificationManager = NotificationManagerCompat.from(context)
        val builder = NotificationCompat.Builder(context, "PostCall")
            .setContentTitle("See call information")
            .setSmallIcon(R.drawable.post_ic_notification_phone_call)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setPriority(2)
            .setWhen(System.currentTimeMillis())
            .setDefaults(1)
            .setAutoCancel(true)
            .setVibrate(LongArray(0))

        if (Build.VERSION.SDK_INT >= 26) {
            builder.setChannelId("PostCall")
            val channel = NotificationChannel("PostCall", "PostCall", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Notifications."
                setLockscreenVisibility(1)
                enableVibration(true)
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }

        if (ActivityCompat.checkSelfPermission(context, "android.permission.POST_NOTIFICATIONS") != 0) return

        if (isFullScreenGranted(context)) {
            builder.setFullScreenIntent(pendingIntent, true)
        } else {
            builder.setContentIntent(pendingIntent)
        }
        Log.e("PostCall", "isFullScreenGranted-> ${isFullScreenGranted(context)}")
        notificationManager.notify(System.currentTimeMillis().toInt(), builder.build())
    }

    fun isFullScreenGranted(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= 34) {
            return (context.getSystemService(NotificationManager::class.java)).canUseFullScreenIntent()
        }
        return true
    }
}
