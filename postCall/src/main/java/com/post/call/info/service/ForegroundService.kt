package com.post.call.info.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.post.call.info.PostCallApplication
import com.post.call.info.R
import com.post.call.info.receiver.CallerWidgetWindow
import com.post.call.info.receiver.PhoneCallReceiver
import com.post.call.info.utils.checkNotificationPermission
import com.post.call.info.utils.getBaseConfig

class ForegroundService : Service() {

    companion object {
        const val CHANNEL_ID_POST_CALL = "PostCallScreen"
        const val CHANNEL_NAME_POST_CALL = "PostCallScreen"
        const val NOTIFICATION_ID_POST_CALL = 111
        var myServiceIsRunning: Boolean = false
    }

    private var isEnablePostCallScreen: Boolean = false
    private var isReceiverRegistered: Boolean = false
    private var windowView: CallerWidgetWindow? = null

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent?) {
            if (intent?.action.equals("isVoiceOptionEnabled", ignoreCase = true)) {
                try {
                    windowView?.showMuteOption()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    override fun onBind(intent: Intent): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        if (PhoneCallReceiver.isCallEnded) {
            startNotificationService()
            Log.e("PostCall", "ForegroundService onCreate stopSelf")
            stopSelf()
            return
        }
        myServiceIsRunning = true
        Log.e("PostCall", "ForegroundService onCreate")
        isEnablePostCallScreen = true
        if (hasOverlayPermission(this) && getBaseConfig(this).enablePostCallScreen && isEnablePostCallScreen) {
            windowView = CallerWidgetWindow(this)
            windowView?.show()
        }
        val intentFilter = IntentFilter().apply { addAction("isVoiceOptionEnabled") }
        try {
            if (Build.VERSION.SDK_INT >= 33) {
                registerReceiver(broadcastReceiver, intentFilter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                registerReceiver(broadcastReceiver, intentFilter)
            }
            isReceiverRegistered = true
        } catch (unused: Exception) {}
    }

    private fun hasOverlayPermission(context: Context): Boolean = Settings.canDrawOverlays(context)

    private fun intView() {
        Thread {
            while (PostCallApplication.isCallingStart) {}
        }.start()
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        Log.e("PostCall", "ForegroundService onStartCommand")
        startNotificationService()
        intView()
        return START_NOT_STICKY
    }

    override fun stopService(intent: Intent): Boolean {
        removeNotification()
        return super.stopService(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        myServiceIsRunning = false
        removeNotification()
        if (isReceiverRegistered) {
            unregisterReceiver(broadcastReceiver)
            isReceiverRegistered = false
        }
        Log.e("PostCall", "ForegroundService onDestroy")
    }

    private fun removeNotification() {
        NotificationManagerCompat.from(this).cancel(NOTIFICATION_ID_POST_CALL)
        try {
            windowView?.hide()
        } catch (unused: Exception) {}
    }

    private fun startNotificationService() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val builder = NotificationCompat.Builder(this, CHANNEL_ID_POST_CALL)
            .setContentTitle("This Call")
            .setContentText("Private number")
            .setSmallIcon(R.drawable.post_ic_notification_phone_call)
            .setPriority(-1)
            .setDefaults(-1)
            .setAutoCancel(true)
            .setVibrate(LongArray(0))

        if (Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel(CHANNEL_ID_POST_CALL, CHANNEL_NAME_POST_CALL, NotificationManager.IMPORTANCE_LOW).apply {
                description = "Notifications are info related."
                setLockscreenVisibility(1)
                enableLights(true)
                lightColor = -7829368
                enableVibration(true)
                setShowBadge(true)
            }
            notificationManager.createNotificationChannel(channel)
            builder.setChannelId(CHANNEL_ID_POST_CALL)
            builder.setBadgeIconType(NotificationCompat.BADGE_ICON_SMALL)
        }
        try {
            if (Build.VERSION.SDK_INT >= 29) {
                if (checkNotificationPermission(this)) {
                    startForeground(NOTIFICATION_ID_POST_CALL, builder.build(), ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL)
                }
            } else {
                startForeground(NOTIFICATION_ID_POST_CALL, builder.build())
            }
        } catch (e: Exception) {
            Log.e("ForegroundService", "Error starting foreground service: ${e.message}", e)
        }
    }
}
