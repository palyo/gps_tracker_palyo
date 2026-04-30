package com.post.call.info.receiver

import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.post.call.info.PostCallApplication
import com.post.call.info.ads.PostCallBannerAds
import com.post.call.info.ads.PostCallNativeAds
import com.post.call.info.service.ForegroundService
import com.post.call.info.ui.activity.PostCallActivity
import com.post.call.info.utils.getBaseConfig
import com.post.call.info.utils.isBannerLoad
import com.post.call.info.utils.isNativeLoad
import java.util.Date

abstract class PhoneCallReceiver : BroadcastReceiver() {

    private var isEnablePostCallScreen: Boolean = false
    private var isOverlyPermision: Boolean = false
    private var isShowPostCallScreen: Boolean = false

    companion object {
        private var callStartTime: Date? = null
        var isCallEnded: Boolean = false
            private set
        private var isIncomingCall: Boolean = false
        private var isOutgoingCall: Boolean = false
        var isVoiceOptionEnabled: Boolean = false
        private var savedNumber: String? = null
        private var windowView: CallerWidgetWindow? = null
        private var lastState: Int = -1

        fun setLastState(i: Int) { lastState = i }
        fun setIncomingCall(z: Boolean) { isIncomingCall = z }
        fun setOutgoingCall(z: Boolean) { isOutgoingCall = z }
        fun setCallEnded(z: Boolean) { isCallEnded = z }
    }

    fun isEnablePostCallScreen(): Boolean = isEnablePostCallScreen
    fun isShowPostCallScreen(): Boolean = isShowPostCallScreen

    protected open fun onIncomingCallAnswered(context: Context, str: String?, date: Date?) {}
    protected open fun onIncomingCallEnded(context: Context, str: String?, date: Date?, date2: Date?) {}
    protected open fun onIncomingCallReceived(context: Context, str: String?, date: Date?) {}
    protected open fun onIncomingCallStarted(ctx: Context, str: String?, date: Date?) {}
    protected open fun onMissedCall(context: Context, str: String?, date: Date?) {}
    protected open fun onOutgoingCallEnded(context: Context, str: String?, date: Date?, date2: Date?) {}
    protected open fun onOutgoingCallStarted(ctx: Context, str: String?, date: Date?) {}

    override fun onReceive(context: Context, intent: Intent) {
        Log.e("PostCall", "onReceive call")
        var callState = 1
        isEnablePostCallScreen = true
        isShowPostCallScreen = getBaseConfig(context).enablePostCallScreen
        isOverlyPermision = hasOverlayPermission(context)

        if ("android.intent.action.NEW_OUTGOING_CALL" == intent.action) {
            val number = intent.extras?.getString("android.intent.extra.PHONE_NUMBER")
            savedNumber = number
            Log.e("PostCall", "savedNumber: $number")
            return
        }

        Log.e("PostCall", "extras available: ${intent.extras != null}")
        val extras = intent.extras ?: return
        val state = extras.getString("state")
        val incomingNumber = extras.getString("incoming_number")

        if (!TelephonyManager.EXTRA_STATE_IDLE.equals(state)) {
            if (TelephonyManager.EXTRA_STATE_OFFHOOK.equals(state)) callState = 2
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
            val freeRam = getRamInfoInMB(context).second
            if (freeRam < 1500) {
                onCallStateManageWithoutService(context, callState, incomingNumber)
                Log.e("PostCall", "service not started (low RAM)")
            }
        }
    }

    private fun getRamInfoInMB(context: Context): Pair<Long, Long> {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            ?: return Pair(0L, 0L)
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        val gb = 1024L * 1024L * 1024L
        val totalGB = memoryInfo.totalMem / gb
        val availGB = memoryInfo.availMem / gb
        Log.e("PostCall", "getRamInfoInMB RamGB-> $availGB/$totalGB")
        return Pair(totalGB, availGB)
    }

    fun onCallStateManageWithoutService(context: Context, callState: Int, str: String?) {
        Log.e("PostCall", "onCallStateManage :state $callState")
        if (lastState == callState) {
            Log.e("PostCall", "onCallStateManage current state and lastState both are same!!!")
            return
        }
        when (callState) {
            0 -> {
                Log.e("PostCall", "onCallStateManage CallEnded callStartTime ${callStartTime == null}")
                if (callStartTime == null) {
                    gettingTimeFromPref(context)
                    lastState = getPrefState(context)
                    isOutgoingCall = getOutgoingCall(context)
                    isIncomingCall = getIncomingCall(context)
                }
                if (lastState == 1) {
                    if (isIncomingCall) onIncomingCallEnded(context, savedNumber, callStartTime, Date())
                    else onOutgoingCallEnded(context, savedNumber, callStartTime, Date())
                } else {
                    if (isIncomingCall) onIncomingCallEnded(context, savedNumber, callStartTime, Date())
                    else onOutgoingCallEnded(context, savedNumber, callStartTime, Date())
                }
            }
            1 -> {
                Log.e("PostCall", "onCallStateManage:CALL_STATE_RINGING $callState isOutgoingCall-> $isOutgoingCall isIncomingCall-> $isIncomingCall")
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
            }
            2 -> {
                Log.e("PostCall", "onCallStateManage:CALL_STATE_OFFHOOK $callState lastState $lastState callStartTime ${callStartTime == null} isOutgoingCall-> $isOutgoingCall isIncomingCall-> $isIncomingCall")
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
                    }
                } else if (!isOutgoingCall) {
                    isIncomingCall = true
                    callStartTime = Date()
                    saveStartTime(context, callStartTime!!)
                    saveCallState(context, callState, isIncomingCall, isOutgoingCall)
                    onIncomingCallAnswered(context, savedNumber, callStartTime)
                }
            }
        }
        lastState = callState
    }

    private fun gettingTimeFromPref(context: Context) {
        val prefStartTime = getBaseConfig(context).prefs_start_call_timer
        callStartTime = if (prefStartTime != 0L) Date(prefStartTime) else {
            val date = Date()
            saveStartTime(context, date)
            date
        }
    }

    private fun saveStartTime(context: Context, date: Date?) {
        date?.let { saveStartTimePref(context, it.time) }
    }

    fun saveStartTimePref(context: Context, j: Long) {
        getBaseConfig(context).prefs_start_call_timer = j
    }

    fun startService(context: Context) {
        Log.e("PostCall", "startService:for")
        if (PostCallApplication.isCallingStart) return
        PostCallApplication.isStartedService = false
        if (checkNotificationPermission(context)) {
            PostCallApplication.isCallingStart = true
            try {
                context.startService(Intent(context, ForegroundService::class.java))
                PostCallApplication.isStartedService = true
            } catch (unused: Exception) {
                if (Build.VERSION.SDK_INT >= 26) {
                    try {
                        context.applicationContext.startForegroundService(Intent(context.applicationContext, ForegroundService::class.java))
                        PostCallApplication.isStartedService = true
                    } catch (e: Exception) {
                        Log.e("PostCall", "startService CALL Exception e1 ${e.message}")
                        PostCallApplication.isStartedService = false
                    }
                }
            }
        } else {
            PostCallApplication.isStartedService = false
        }
        if (PostCallApplication.isStartedService || !hasOverlayPermission(context)) return
        PostCallApplication.isCallingStart = true
        showOverlay(context)
    }

    private fun saveCallState(context: Context, i: Int, z: Boolean, z2: Boolean) {
        getBaseConfig(context).prefs_call_state = i
        getBaseConfig(context).prefs_call_incoming = z
        getBaseConfig(context).prefs_call_outgoing = z2
    }

    private fun getPrefState(context: Context): Int = getBaseConfig(context).prefs_call_state
    private fun getIncomingCall(context: Context): Boolean = getBaseConfig(context).prefs_call_incoming
    private fun getOutgoingCall(context: Context): Boolean = getBaseConfig(context).prefs_call_outgoing

    private fun checkNotificationPermission(context: Context): Boolean {
        val z = try {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        } catch (unused: Exception) {
            false
        }
        Log.e("PostCall", "Receiver notificationsEnabled  $z")
        if (Build.VERSION.SDK_INT < 26 || z) {
            return Build.VERSION.SDK_INT < 33 ||
                    ContextCompat.checkSelfPermission(context, "android.permission.POST_NOTIFICATIONS") == 0
        }
        return false
    }

    fun stopService(context: Context) {
        if (!PostCallApplication.isStartedService) removeOverlay()
        try {
            if (PostCallApplication.isCallingStart) {
                PostCallApplication.isCallingStart = false
                if (PostCallApplication.isStartedService) {
                    PostCallApplication.isStartedService = false
                    context.applicationContext.stopService(Intent(context.applicationContext, ForegroundService::class.java))
                }
            }
        } catch (ignored: Exception) {}
    }

    fun onCallStateChanged(context: Context, callState: Int, str: String?) {
        Log.e("PostCall", "onCallStateChanged:state $callState")
        if (lastState == callState) {
            Log.e("PostCall", "current state and lastState both are same!!!")
            return
        }
        when (callState) {
            0 -> {
                if (isVoiceOptionEnabled) {
                    isVoiceOptionEnabled = false
                    context.applicationContext.sendBroadcast(Intent("isVoiceOptionEnabled"))
                }
                Log.e("PostCall", "onCallStateChanged:CALL_STATE_IDLE $callState lastState $lastState isIncoming-> $isIncomingCall isOutgoingCall-> $isOutgoingCall")
                if (callStartTime == null) {
                    gettingTimeFromPref(context)
                    lastState = getPrefState(context)
                    isOutgoingCall = getOutgoingCall(context)
                    isIncomingCall = getIncomingCall(context)
                }
                if (lastState == 1) {
                    if (isIncomingCall) onIncomingCallEnded(context, savedNumber, callStartTime, Date())
                    else onOutgoingCallEnded(context, savedNumber, callStartTime, Date())
                    nativeLoadPostCall(context)
                } else {
                    if (isIncomingCall) onIncomingCallEnded(context, savedNumber, callStartTime, Date())
                    else onOutgoingCallEnded(context, savedNumber, callStartTime, Date())
                }
            }
            1 -> {
                Log.e("PostCall", "onCallStateChanged:CALL_STATE_RINGING $callState isOutgoingCall-> $isOutgoingCall isIncomingCall-> $isIncomingCall")
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
                    onIncomingCallStarted(context, str, callStartTime)
                    Log.e("PostCall", "onCallStateChanged:postCallActivity1")
                }
                nativeLoadPostCall(context)
            }
            2 -> {
                if (isVoiceOptionEnabled) {
                    isVoiceOptionEnabled = false
                    context.applicationContext.sendBroadcast(Intent("isVoiceOptionEnabled"))
                }
                startService(context)
                Log.e("PostCall", "onCallStateChanged:CALL_STATE_OFFHOOK $callState lastState $lastState isOutgoingCall-> $isOutgoingCall isIncomingCall-> $isIncomingCall")
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
                        onOutgoingCallStarted(context, savedNumber, callStartTime)
                        Log.e("PostCall", "onCallStateChanged:postCallActivity2")
                    }
                    nativeLoadPostCall(context)
                } else if (!isOutgoingCall) {
                    isIncomingCall = true
                    callStartTime = Date()
                    saveStartTime(context, callStartTime!!)
                    saveCallState(context, callState, isIncomingCall, isOutgoingCall)
                    onIncomingCallAnswered(context, savedNumber, callStartTime)
                    nativeLoadPostCall(context)
                }
            }
        }
        lastState = callState
    }

    private fun disabledOpenAds() {
        PostCallApplication.setOpenAdHide(true)
    }

    fun nativeLoadPostCall(context: Context) {
        if (checkPermission() && isShowPostCallScreen && isEnablePostCallScreen) {
            val callCounter = getBaseConfig(context).callCounter
            if (isBannerLoad(callCounter)) {
                Log.e("PostCall", "nativeLoadPostCall NativeTag isBannerLoad true")
                loadGoogleBannerAd(context)
            } else if (isNativeLoad(callCounter)) {
                Log.e("PostCall", "nativeLoadPostCall NativeTag isNativeLoad true")
                if (PostCallApplication.isNativeFailedGooglePostCall) return
                Log.e("PostCall", "nativeLoadPostCall NativeTag googleNative call")
                googleNativeLoadPostCall(context)
            }
        }
    }

    private fun loadGoogleBannerAd(context: Context) {
        if (PostCallApplication.bannerAdView == null && !PostCallApplication.bannerAdLoading) {
            PostCallBannerAds().loadBanner(context, null, null)
        } else if (PostCallApplication.bannerAdView == null || PostCallApplication.bannerAdLoading ||
            System.currentTimeMillis() - PostCallApplication.adBannerLoadTime < PostCallApplication.adBannerExpirationTime) {
            // banner is already loading or fresh — do nothing
        } else {
            PostCallBannerAds().onAdExpired(context)
            Log.e("PostCall", "bannerAD is Expired.....")
        }
        // Native is NOT pre-loaded here intentionally.
        // It loads on-demand in PostCallActivity only when both Banner1 and Banner2 fail.
        // Pre-loading native here would waste ~91% of native matches → destroys show rate.
    }

    fun googleNativeLoadPostCall(context: Context) {
        if (checkPermission() && isShowPostCallScreen && isEnablePostCallScreen) {
            if (PostCallApplication.nativeAdsPostCall == null && !PostCallApplication.isNativePostCallLoading) {
                Log.e("PostCall", "NativeTag PostCall google native is loading")
                PostCallApplication.isNativeGooglePostCall = true
                PostCallNativeAds(context).loadPostNative()
                return
            }
            if (PostCallApplication.nativeAdsPostCall != null) {
                val currentTimeMillis = System.currentTimeMillis()
                Log.e("PostCall", "NativeTag google PostCall Time Diff-->> ${currentTimeMillis - PostCallApplication.adNativeLoadTime}")
                if (currentTimeMillis - PostCallApplication.adNativeLoadTime >= PostCallApplication.adNativeExpirationTime) {
                    if (!PostCallApplication.isNativePostCallLoading) {
                        PostCallApplication.adNativeLoadTime = 0L
                        PostCallApplication.nativeAdsPostCall = null
                        PostCallNativeAds(context).loadPostNative()
                    }
                    Log.e("PostCall", "NativeTag google PostCall Time ad expired, loading a new ad")
                }
            }
            Log.e("PostCall", "NativeTag PostCall google native is not load")
        }
    }

    fun checkPermission(): Boolean = isOverlyPermision
    fun hasOverlayPermission(context: Context): Boolean = Settings.canDrawOverlays(context)

    private fun showOverlay(context: Context) {
        windowView = CallerWidgetWindow(context)
        windowView?.show()
    }

    private fun removeOverlay() {
        Log.e("PostCall", "removeOverlay ${windowView != null}")
        try {
            windowView?.hide()
        } catch (ignored: Exception) {}
    }
}
