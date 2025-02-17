package aanibrothers.tracker.io

import aanibrothers.tracker.io.cdo.*
import android.app.*
import android.content.*
import android.os.*
import androidx.multidex.*

class App : MultiDexApplication(), Application.ActivityLifecycleCallbacks {
    companion object {
        var isOpenInter = false
        private var instance: App? = null
        private var appContext: Context? = null
        fun getInstance(): App = instance ?: throw IllegalStateException("Application is not created yet!")
        fun getAppContext(): Context = appContext ?: throw IllegalStateException("Application is not created yet!")
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        appContext = applicationContext
        registerActivityLifecycleCallbacks(this)
        createNotificationChannel()
        initCalldorado()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val navigationChannel = NotificationChannel(
                "navigation_channel",
                "Navigation",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Channel for navigation"
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(navigationChannel)
        }
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}

    override fun onActivityStarted(activity: Activity) {}

    override fun onActivityResumed(activity: Activity) {}

    override fun onActivityPaused(activity: Activity) {}

    override fun onActivityStopped(activity: Activity) {}

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

    override fun onActivityDestroyed(activity: Activity) {}
}