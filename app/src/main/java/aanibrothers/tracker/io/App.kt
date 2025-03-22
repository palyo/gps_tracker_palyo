package aanibrothers.tracker.io

import aanibrothers.tracker.io.cdo.*
import aanibrothers.tracker.io.extension.*
import aanibrothers.tracker.io.module.*
import aanibrothers.tracker.io.ui.*
import aanibrothers.tracker.io.ui.LauncherActivity
import android.Manifest
import android.app.*
import android.content.*
import android.os.*
import androidx.lifecycle.*
import androidx.multidex.*
import coder.apps.space.library.extension.hasOverlayPermission
import coder.apps.space.library.extension.hasPermissions
import com.calldorado.ui.aftercall.*

class App : MultiDexApplication(), Application.ActivityLifecycleCallbacks {
    companion object {
        var isOpenInter = false
        private var instance: App? = null
        private var appContext: Context? = null
        fun getInstance(): App = instance ?: throw IllegalStateException("Application is not created yet!")
        fun getAppContext(): Context = appContext ?: throw IllegalStateException("Application is not created yet!")
        var appOpenManager: AppOpenManager? = null
        var currentActivity: Activity? = null
        var classes: MutableList<Class<*>> = mutableListOf()
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        appContext = applicationContext
        registerActivityLifecycleCallbacks(this)
        createNotificationChannel()
        initCalldorado()

        setAvoidMultipleClass(
            mutableListOf(
                LauncherActivity::class.java,
                AppPermissionActivity::class.java,
                PromptActivity::class.java,
                CallerIdActivity::class.java
            )
        )
        registerActivityLifecycleCallbacks(this)

        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                if (!getAppContext().hasPermissions(arrayOf(Manifest.permission.READ_PHONE_STATE)) && !hasOverlayPermission() && !isPremium) {
                    if (!isOpenInter) {
                        if (isShowOpenAdsOnStart(currentActivity?.javaClass?.name ?: "")) {
                            viewAppOpen(listener = null, isWait = false)
                        }
                    }
                } else {
                    if (!isOpenInter && (applicationContext?.appOpenCount ?: 0) >= 2) {
                        if (isShowOpenAdsOnStart(currentActivity?.javaClass?.name ?: "")) {
                            viewAppOpen(listener = null, isWait = false)
                        }
                    }
                }

                if (currentActivity != null) {
                    if (isOpenInter) isOpenInter = false
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