package aanibrothers.tracker.io.afterCall.service

import aanibrothers.tracker.io.R
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
import aanibrothers.tracker.io.afterCall.PostCallApplication
import aanibrothers.tracker.io.afterCall.extensions.checkNotificationPermission
import aanibrothers.tracker.io.afterCall.extensions.enablePostCallScreen
import aanibrothers.tracker.io.afterCall.receiver.CallerWidgetWindow
import com.contact.phone.dailer.postCall.receiver.PhoneCallReceiver
import kotlin.NotImplementedError

class ForegroundService : Service() {
    private var isEnablePostCallScreen = false
    private var isReceiverRegistered = false
    private var windowView: CallerWidgetWindow? = null

    private val broadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent?) {
            if (intent?.action.equals("isVoiceOptionEnabled", ignoreCase = true)) {
                if (windowView != null) {
                    try {
                        windowView?.showMuteOption()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        if (PhoneCallReceiver.isCallEnded()) {
            startNotificationService()
            Log.e("PostCall", "ForegroundService onCreate stopSelf")
            stopSelf()
            return
        }
        myServiceIsRunning = true
        Log.e("PostCall", "ForegroundService onCreate")
        isEnablePostCallScreen = true
        if (hasOverlayPermission(this) && enablePostCallScreen && isEnablePostCallScreen) {
            windowView = CallerWidgetWindow(this)
            windowView?.show()
        }
        val intentFilter = IntentFilter()
        intentFilter.addAction("isVoiceOptionEnabled")
        try {
            if (Build.VERSION.SDK_INT >= 33) {
                registerReceiver(broadcastReceiver, intentFilter, RECEIVER_NOT_EXPORTED)
            } else {
                registerReceiver(broadcastReceiver, intentFilter)
            }
            isReceiverRegistered = true
        } catch (_: Exception) {
        }
    }

    private fun hasOverlayPermission(context: Context): Boolean = Settings.canDrawOverlays(context)

    private fun intView() {
        Thread {
            while (PostCallApplication.isCallingStart()) {
            }
        }.start()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.e("PostCall", "ForegroundService onStartCommand")
        startNotificationService()
        intView()
        return START_NOT_STICKY
    }

    override fun stopService(intent: Intent?): Boolean {
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
        } catch (_: Exception) {
            throw NotImplementedError("An operation is not implemented: Not yet implemented")
        }
    }

    private fun startNotificationService() {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val channelName = getString(R.string.channel_post_call_screen_name)
        val channelDescription = getString(R.string.channel_post_call_screen_description)
        val builder = NotificationCompat.Builder(this, CHANNEL_ID_POST_CALL)
            .setContentTitle(getString(R.string.notification_post_call_title))
            .setContentText(getString(R.string.notification_post_call_private_number))
            .setSmallIcon(R.drawable.post_ic_notification_phone_call)
            .setPriority(-1)
            .setDefaults(-1)
            .setAutoCancel(true)
            .setVibrate(longArrayOf())

        val channel = NotificationChannel(
            CHANNEL_ID_POST_CALL,
            channelName,
            NotificationManager.IMPORTANCE_LOW
        )
        channel.description = channelDescription
        channel.lockscreenVisibility = 1
        channel.enableLights(true)
        channel.lightColor = -7829368
        channel.enableVibration(true)
        channel.setShowBadge(true)
        notificationManager.createNotificationChannel(channel)

        builder.setChannelId(CHANNEL_ID_POST_CALL)
        builder.setBadgeIconType(NotificationCompat.BADGE_ICON_SMALL)
        try {
            if (Build.VERSION.SDK_INT >= 29) {
                if (checkNotificationPermission(this)) {
                    startForeground(
                        NOTIFICATION_ID_POST_CALL,
                        builder.build(),
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL
                    )
                    return
                }
                return
            }
            startForeground(NOTIFICATION_ID_POST_CALL, builder.build())
        } catch (e: Exception) {
            val message = e.message
            Log.e("ForegroundService", "Error starting foreground service: $message", e)
        }
    }

    companion object {
        const val CHANNEL_ID_EndCall_POST_CALL = "PostCall"
        const val CHANNEL_ID_POST_CALL = "PostCallScreen"
        const val NOTIFICATION_ID_POST_CALL = 111

        @JvmField
        var myServiceIsRunning: Boolean = false
    }
}
