package aanibrothers.tracker.io.caller.alert

import aanibrothers.tracker.io.R
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat

class AlertReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra("title") ?: "Alert"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channelId = "alert_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Alert Notifications",
                NotificationManager.IMPORTANCE_HIGH,
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notification =
            NotificationCompat.Builder(context, channelId).setSmallIcon(R.drawable.ic_call_icon).setContentTitle("Reminder").setContentText(title).setPriority(NotificationCompat.PRIORITY_HIGH).setAutoCancel(true).build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
}
