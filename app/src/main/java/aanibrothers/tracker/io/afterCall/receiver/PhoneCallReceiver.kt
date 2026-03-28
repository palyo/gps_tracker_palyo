package com.contact.phone.dailer.postCall.receiver

import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import aanibrothers.tracker.io.afterCall.PostCallApplication
import aanibrothers.tracker.io.afterCall.ads.PostCallBannerAds
import aanibrothers.tracker.io.afterCall.ads.PostCallNativeAds
import aanibrothers.tracker.io.afterCall.extensions.enablePostCallScreen
import aanibrothers.tracker.io.afterCall.extensions.getBaseConfig
import aanibrothers.tracker.io.afterCall.extensions.isBannerLoad
import aanibrothers.tracker.io.afterCall.extensions.isNativeLoad
import aanibrothers.tracker.io.afterCall.extensions.*
import aanibrothers.tracker.io.afterCall.receiver.CallerWidgetWindow
import aanibrothers.tracker.io.afterCall.service.ForegroundService
import aanibrothers.tracker.io.afterCall.ui.activity.PostCallActivity
import java.util.Date

abstract class PhoneCallReceiver : BroadcastReceiver() {
    private var isEnablePostCallScreen = false
    private var isOverlyPermision = false
    private var isShowPostCallScreen = false

    protected open fun onIncomingCallAnswered(context: Context, str: String?, date: Date) {
    }

    protected open fun onIncomingCallEnded(context: Context, str: String?, date: Date, date2: Date) {
    }

    protected open fun onIncomingCallReceived(context: Context, str: String?, date: Date) {
    }

    protected fun onIncomingCallStarted(ctx: Context, str: String?, date: Date) {
    }

    protected open fun onMissedCall(context: Context, str: String?, date: Date) {
    }

    protected open fun onOutgoingCallEnded(context: Context, str: String?, date: Date, date2: Date) {
    }

    protected open fun onOutgoingCallStarted(ctx: Context, str: String?, date: Date) {
    }

    fun isEnablePostCallScreen(): Boolean {
        return isEnablePostCallScreen
    }

    fun isShowPostCallScreen(): Boolean {
        return isShowPostCallScreen
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.e("PostCall", "onReceive call")
        var callState = 1
        isEnablePostCallScreen = true
        isShowPostCallScreen = context.enablePostCallScreen
        isOverlyPermision = hasOverlayPermission(context)

        if (Intent.ACTION_NEW_OUTGOING_CALL == intent.action) {
            val extras = intent.extras
            if (extras != null) {
                val number = extras.getString("android.intent.extra.PHONE_NUMBER")
                savedNumber = number
                Log.e("PostCall", "savedNumber: $number")
            }
            return
        }

        Log.e("PostCall", "extras available: ${intent.extras != null}")
        val extras: Bundle = intent.extras ?: return
        val state = extras.getString("state")
        val incomingNumber = extras.getString("incoming_number")

        if (TelephonyManager.EXTRA_STATE_IDLE != state) {
            if (TelephonyManager.EXTRA_STATE_OFFHOOK == state) {
                callState = 2
            }
            val ramInfo = getRamInfoInMB(context)
            val totalRam = ramInfo.first
            val freeRam = ramInfo.second
            if (freeRam < 1500 && totalRam > 2000) {
                Log.e("PostCall", "onCallStateChanged call")
                onCallStateChanged(context, callState, incomingNumber)
            } else {
                onCallStateChanged(context, callState, incomingNumber)
                Log.e("PostCall", "service not started (low RAM)")
            }
        } else {
            callState = 0
            val ramInfo = getRamInfoInMB(context)
            val freeRam = ramInfo.second
            if (freeRam < 1500) {
                onCallStateManageWithoutService(context, callState, incomingNumber)
                Log.e("PostCall", "service not started (low RAM)")
            }
        }
    }

    private fun getRamInfoInMB(context: Context): Pair<Long, Long> {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        if (activityManager == null) {
            return Pair(0L, 0L)
        }
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        val mb = 1024L * 1024L
        val gb = 1024L * 1024L * 1024L
        val totalMB = memoryInfo.totalMem / mb
        val availMB = memoryInfo.availMem / mb
        val totalGB = memoryInfo.totalMem / gb
        val availGB = memoryInfo.availMem / gb
        Log.e("PostCall", "getRamInfoInMB RamGB-> $availGB/$totalGB RamMB-> $availMB/$totalMB")
        return Pair(totalGB, availGB)
    }

    fun onCallStateManageWithoutService(context: Context, callState: Int, str: String?) {
        Log.e("PostCall", "onCallStateManage :state $callState")
        if (lastState == callState) {
            Log.e("PostCall", "onCallStateManage current state and lastState both are same!!!")
            return
        }
        if (callState == 0) {
            Log.e("PostCall", "onCallStateManage CallEnded callStartTime ${callStartTime == null}  ")
            if (callStartTime == null) {
                gettingTimeFromPref(context)
                lastState = getPrefState(context)
                isOutgoingCall = getOutgoingCall(context)
                isIncomingCall = getIncomingCall(context)
            }
            val startTime = callStartTime ?: Date()
            if (lastState == 1) {
                if (isIncomingCall) {
                    onIncomingCallEnded(context, savedNumber, startTime, Date())
                } else {
                    onOutgoingCallEnded(context, savedNumber, startTime, Date())
                }
            } else if (isIncomingCall) {
                onIncomingCallEnded(context, savedNumber, startTime, Date())
            } else {
                onOutgoingCallEnded(context, savedNumber, startTime, Date())
            }
        } else if (callState == 1) {
            Log.e(
                "PostCall",
                "onCallStateManage:CALL_STATE_RINGING $callState isOutgoingCall-> $isOutgoingCall isIncomingCall-> $isIncomingCall"
            )
            PostCallActivity.getPostCallActivity()?.finishActivity()
            disabledOpenAds()
            if (!isOutgoingCall) {
                isIncomingCall = true
                val date = Date()
                callStartTime = date
                saveStartTime(context, date)
                saveCallState(context, callState, isIncomingCall, isOutgoingCall)
                savedNumber = str
            }
        } else if (callState == 2) {
            Log.e(
                "PostCall",
                "onCallStateManage:CALL_STATE_OFFHOOK $callState  lastState $lastState  callStartTime ${callStartTime == null} " +
                    "isOutgoingCall-> $isOutgoingCall  isIncomingCall-> $isIncomingCall if-> ${lastState != 1}"
            )
            if (callStartTime == null) {
                lastState = getPrefState(context)
                isOutgoingCall = getOutgoingCall(context)
                isIncomingCall = getIncomingCall(context)
            }
            if (lastState != 1) {
                PostCallActivity.getPostCallActivity()?.finishActivity()
                disabledOpenAds()
                if (!isOutgoingCall && !isIncomingCall) {
                    isOutgoingCall = true
                    isIncomingCall = false
                    val date2 = Date()
                    callStartTime = date2
                    saveStartTime(context, date2)
                    saveCallState(context, callState, isIncomingCall, isOutgoingCall)
                    Log.e("PostCall", "onCallStateManage:postCallActivity2 ")
                }
            } else if (!isOutgoingCall) {
                isIncomingCall = true
                callStartTime = Date()
                saveStartTime(context, callStartTime ?: Date())
                saveCallState(context, callState, isIncomingCall, isOutgoingCall)
                onIncomingCallAnswered(context, savedNumber, callStartTime ?: Date())
            }
        }
        lastState = callState
    }

    private fun gettingTimeFromPref(context: Context) {
        val prefsStartCallTimer = context.prefs_start_call_timer
        if (prefsStartCallTimer != 0L) {
            callStartTime = Date(prefsStartCallTimer)
            return
        }
        callStartTime = Date()
        saveStartTime(context, callStartTime ?: Date())
    }

    private fun saveStartTime(context: Context, date: Date) {
        saveStartTimePref(context, date.time)
    }

    fun saveStartTimePref(context: Context, value: Long) {
        context.prefs_start_call_timer = value
    }

    fun startService(context: Context) {
        Log.e("PostCall", "startService:for ")
        if (PostCallApplication.isCallingStart()) {
            return
        }
        PostCallApplication.setStartedService(false)
        if (checkNotificationPermission(context)) {
            PostCallApplication.setCallingStart(true)
            try {
                context.startService(Intent(context, ForegroundService::class.java))
                PostCallApplication.setStartedService(true)
            } catch (unused: Exception) {
                try {
                    context.applicationContext.startForegroundService(
                        Intent(context.applicationContext, ForegroundService::class.java)
                    )
                    PostCallApplication.setStartedService(true)
                } catch (e: Exception) {
                    Log.e("PostCall", "startService CALL Exception e1 ${e.message}")
                    PostCallApplication.setStartedService(false)
                }
            }
        } else {
            PostCallApplication.setStartedService(false)
        }
        if (PostCallApplication.isStartedService() || !hasOverlayPermission(context)) {
            return
        }
        PostCallApplication.setCallingStart(true)
        showOverlay(context)
    }

    private fun saveCallState(context: Context, state: Int, incoming: Boolean, outgoing: Boolean) {
        context.prefs_call_state = state
        context.prefs_call_incoming = incoming
        context.prefs_call_outgoing = outgoing
    }

    private fun getPrefState(context: Context): Int {
        return context.prefs_call_state
    }

    private fun getIncomingCall(context: Context): Boolean {
        return context.prefs_call_incoming
    }

    private fun getOutgoingCall(context: Context): Boolean {
        return context.prefs_call_outgoing
    }

    private fun checkNotificationPermission(context: Context): Boolean {
        val enabled = try {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        } catch (unused: Exception) {
            false
        }
        Log.e("PostCall", "Receiver notificationsEnabled  $enabled")
        return if (enabled) {
            Build.VERSION.SDK_INT < 33 ||
                ContextCompat.checkSelfPermission(context, "android.permission.POST_NOTIFICATIONS") == 0
        } else {
            false
        }
    }

    fun stopService(context: Context) {
        if (!PostCallApplication.isStartedService()) {
            removeOverlay()
        }
        try {
            if (PostCallApplication.isCallingStart()) {
                PostCallApplication.setCallingStart(false)
                if (PostCallApplication.isStartedService()) {
                    PostCallApplication.setStartedService(false)
                    context.applicationContext.stopService(
                        Intent(context.applicationContext, ForegroundService::class.java)
                    )
                }
            }
        } catch (ignored: Exception) {
        }
    }

    fun onCallStateChanged(context: Context, callState: Int, str: String?) {
        Log.e("PostCall", "onCallStateChanged:state $callState")
        if (lastState == callState) {
            Log.e("PostCall", "current state and lastState both are same!!!")
            return
        }
        if (callState == 0) {
            if (isVoiceOptionEnabled) {
                isVoiceOptionEnabled = false
                context.applicationContext.sendBroadcast(Intent("isVoiceOptionEnabled"))
            }
            Log.e(
                "PostCall",
                "onCallStateChanged:CALL_STATE_IDLE $callState lastState $lastState isIncoming-> $isIncomingCall isOutgoingCall-> $isOutgoingCall"
            )
            if (callStartTime == null) {
                gettingTimeFromPref(context)
                lastState = getPrefState(context)
                isOutgoingCall = getOutgoingCall(context)
                isIncomingCall = getIncomingCall(context)
            }
            val startTime = callStartTime ?: Date()
            if (lastState == 1) {
                if (isIncomingCall) {
                    onIncomingCallEnded(context, savedNumber, startTime, Date())
                } else {
                    onOutgoingCallEnded(context, savedNumber, startTime, Date())
                }
                nativeLoadPostCall(context)
            } else if (isIncomingCall) {
                onIncomingCallEnded(context, savedNumber, startTime, Date())
            } else {
                onOutgoingCallEnded(context, savedNumber, startTime, Date())
            }
        } else if (callState == 1) {
            Log.e(
                "PostCall",
                "onCallStateChanged:CALL_STATE_RINGING $callState isOutgoingCall-> $isOutgoingCall isIncomingCall-> $isIncomingCall"
            )
            isVoiceOptionEnabled = true
            startService(context)
            PostCallActivity.getPostCallActivity()?.finishActivity()
            disabledOpenAds()
            if (!isOutgoingCall) {
                isIncomingCall = true
                val date = Date()
                callStartTime = date
                saveStartTime(context, date)
                saveCallState(context, callState, isIncomingCall, isOutgoingCall)
                savedNumber = str
                onIncomingCallStarted(context, str, callStartTime ?: Date())
                Log.e("PostCall", "onCallStateChanged:postCallActivity1 ")
            }
            nativeLoadPostCall(context)
        } else if (callState == 2) {
            if (isVoiceOptionEnabled) {
                isVoiceOptionEnabled = false
                context.applicationContext.sendBroadcast(Intent("isVoiceOptionEnabled"))
            }
            startService(context)
            Log.e(
                "PostCall",
                "onCallStateChanged:CALL_STATE_OFFHOOK $callState  lastState $lastState isOutgoingCall-> $isOutgoingCall  isIncomingCall-> $isIncomingCall if-> ${lastState != 1}"
            )
            if (callStartTime == null) {
                lastState = getPrefState(context)
                isOutgoingCall = getOutgoingCall(context)
                isIncomingCall = getIncomingCall(context)
            }
            if (lastState != 1) {
                PostCallActivity.getPostCallActivity()?.finishActivity()
                disabledOpenAds()
                if (!isOutgoingCall && !isIncomingCall) {
                    isOutgoingCall = true
                    isIncomingCall = false
                    val date2 = Date()
                    callStartTime = date2
                    saveStartTime(context, date2)
                    saveCallState(context, callState, isIncomingCall, isOutgoingCall)
                    onOutgoingCallStarted(context, savedNumber, callStartTime ?: Date())
                    Log.e("PostCall", "onCallStateChanged:postCallActivity2 ")
                }
                nativeLoadPostCall(context)
            } else if (!isOutgoingCall) {
                isIncomingCall = true
                callStartTime = Date()
                saveStartTime(context, callStartTime ?: Date())
                saveCallState(context, callState, isIncomingCall, isOutgoingCall)
                onIncomingCallAnswered(context, savedNumber, callStartTime ?: Date())
                nativeLoadPostCall(context)
            }
        }
        lastState = callState
    }

    private fun disabledOpenAds() {
        PostCallApplication.setOpenAdHide(true)
    }

    fun nativeLoadPostCall(context: Context) {
        if (checkPermission() && isShowPostCallScreen && isEnablePostCallScreen) {
            val callCounter = context.callCounter
            if (isBannerLoad(callCounter)) {
                Log.e("PostCall", "nativeLoadPostCall NativeTag isBannerLoad true ")
                loadGoogleBannerAd(context)
            } else if (isNativeLoad(callCounter)) {
                Log.e("PostCall", "nativeLoadPostCall NativeTag isNativeLoad true ")
                if (PostCallApplication.isNativeFailedGooglePostCall()) {
                    return
                }
                Log.e("PostCall", "nativeLoadPostCall NativeTag googleNative call ")
                googleNativeLoadPostCall(context)
            }
        }
    }

    private fun loadGoogleBannerAd(context: Context) {
        if (PostCallApplication.getBannerAdView() == null && !PostCallApplication.getBannerAdLoading()) {
            PostCallBannerAds().loadBanner(context, null, null)
        } else if (PostCallApplication.getBannerAdView() == null ||
            PostCallApplication.getBannerAdLoading() ||
            System.currentTimeMillis() - PostCallApplication.getAdBannerLoadTime() <
            PostCallApplication.getAdBannerExpirationTime()
        ) {
        } else {
            PostCallBannerAds().onAdExpired(context)
            Log.e("PostCall", "bannerAD is Expired.....")
        }
    }

    fun googleNativeLoadPostCall(context: Context) {
        if (checkPermission() && isShowPostCallScreen && isEnablePostCallScreen) {
            if (PostCallApplication.getNativeAdsPostCall() == null &&
                !PostCallApplication.isNativePostCallLoading()
            ) {
                Log.e("PostCall", "NativeTag PostCall google native is loading  ")
                PostCallApplication.setNativeGooglePostCall(true)
                PostCallNativeAds(context).loadPostNative()
                return
            }
            if (PostCallApplication.getNativeAdsPostCall() != null) {
                val currentTimeMillis = System.currentTimeMillis()
                Log.e(
                    "PostCall",
                    "NativeTag google PostCall Time Diff-->> ${currentTimeMillis - PostCallApplication.getAdNativeLoadTime()}  "
                )
                if (currentTimeMillis - PostCallApplication.getAdNativeLoadTime() >=
                    PostCallApplication.getAdNativeExpirationTime()
                ) {
                    if (!PostCallApplication.isNativePostCallLoading()) {
                        PostCallApplication.setAdNativeLoadTime(0L)
                        PostCallApplication.setNativeAdsPostCall(null)
                        PostCallNativeAds(context).loadPostNative()
                    }
                    Log.e("PostCall", "NativeTag google PostCall Time ad expired, loading a new ad ")
                }
            }
            Log.e("PostCall", "NativeTag PostCall google native is not load ")
        }
    }

    fun checkPermission(): Boolean {
        return isOverlyPermision
    }

    fun hasOverlayPermission(context: Context): Boolean {
        return Settings.canDrawOverlays(context)
    }

    private fun showOverlay(context: Context) {
        windowView = CallerWidgetWindow(context)
        windowView?.show()
    }

    private fun removeOverlay() {
        Log.e("PostCall", "removeOverlay ${windowView != null} ")
        try {
            windowView?.hide()
        } catch (ignored: Exception) {
        }
    }

    companion object {
        private var callStartTime: Date? = null
        private var isCallEnded = false
        private var isIncomingCall = false
        private var isOutgoingCall = false

        @JvmField
        var isVoiceOptionEnabled = false

        private var savedNumber: String? = null
        private var windowView: CallerWidgetWindow? = null
        private var lastState: Int = -1

        fun setLastState(i: Int) {
            lastState = i
        }

        fun setIncomingCall(z: Boolean) {
            isIncomingCall = z
        }

        fun setOutgoingCall(z: Boolean) {
            isOutgoingCall = z
        }

        fun isCallEnded(): Boolean {
            return isCallEnded
        }

        fun setCallEnded(z: Boolean) {
            isCallEnded = z
        }
    }
}
