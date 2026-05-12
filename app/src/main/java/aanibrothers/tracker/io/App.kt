package aanibrothers.tracker.io

import aanibrothers.tracker.io.afterCall.PostDataFragment
import aanibrothers.tracker.io.module.AppOpenManager
import aanibrothers.tracker.io.module.appOpenCount
import aanibrothers.tracker.io.module.viewAppOpen
import aanibrothers.tracker.io.ui.LauncherActivity
import android.app.Activity
import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import coder.apps.space.library.extension.THEME
import coder.apps.space.library.extension.themeToggleMode
import coder.apps.space.library.helper.TinyDB
import com.post.call.info.PostCallApplication
import com.post.call.info.PostCallConfig
import com.post.call.info.ui.activity.PostCallActivity

class App : PostCallApplication(), Application.ActivityLifecycleCallbacks {
    companion object {

        var isOpenInter = false
        private var instance: App? = null
        private var appContext: Context? = null
        fun getInstance(): App =
            instance ?: throw IllegalStateException("Application is not created yet!")

        fun getAppContext(): Context =
            appContext ?: throw IllegalStateException("Application is not created yet!")

        var appOpenManager: AppOpenManager? = null
        var currentActivity: Activity? = null
        var classes: MutableList<Class<*>> = mutableListOf()

        // Don't fire AppOpen on the very first foreground (cold start) — that
        // collides with whatever the launcher is doing and triggers the
        // "ad before user interaction" policy. Only fire on warm starts.
        private var isFirstForeground = true
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        appContext = applicationContext
        PostCallConfig.dataFragmentClass = PostDataFragment::class.java
        registerActivityLifecycleCallbacks(this)
        createNotificationChannel()
        TinyDB(this).putInt(THEME, 1)
        themeToggleMode()
        setAvoidMultipleClass(
            mutableListOf(
                LauncherActivity::class.java,
                PostCallActivity::class.java,
            )
        )

        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                // Skip the first onStart of the process — that's the cold
                // start path, handled by LauncherActivity's own consent flow.
                if (isFirstForeground) {
                    isFirstForeground = false
                    applicationContext?.let { it.appOpenCount = it.appOpenCount + 1 }
                    return
                }

                applicationContext?.let { it.appOpenCount = it.appOpenCount + 1 }

                // Skip if the app is currently in the middle of showing /
                // returning from an interstitial — avoids stacked full-screen ads.
                if (isOpenInter) {
                    isOpenInter = false
                    return
                }

                // Wait until the user has opened the app a couple of times
                // before showing AppOpen at all (gentler onboarding).
                if ((applicationContext?.appOpenCount ?: 0) < 3) return

                if (isShowOpenAdsOnStart(currentActivity?.javaClass?.name ?: "")) {
                    viewAppOpen(listener = null, isWait = false)
                }
            }
        })
    }

    fun isShowOpenAdsOnStart(classname: String): Boolean {
        if (classname == "com.google.android.gms.ads.AdActivity" || AppOpenManager.isShowingAd) {
            return false
        }
        for (aClass in classes) {
            if (aClass.name.equals(classname, ignoreCase = true)) {
                return false
            }
        }

        return true
    }

    open fun setAvoidMultipleClass(aClass: MutableList<Class<*>>) {
        classes.addAll(aClass)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val navigationChannel = NotificationChannel(
                "navigation_channel",
                getString(R.string.channel_navigation_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = getString(R.string.channel_navigation_description)
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(navigationChannel)
        }
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}

    override fun onActivityStarted(activity: Activity) {
        currentActivity = activity
    }

    override fun onActivityResumed(activity: Activity) {
        currentActivity = activity
    }

    override fun onActivityPaused(activity: Activity) {}

    override fun onActivityStopped(activity: Activity) {}

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

    override fun onActivityDestroyed(activity: Activity) {}
}
