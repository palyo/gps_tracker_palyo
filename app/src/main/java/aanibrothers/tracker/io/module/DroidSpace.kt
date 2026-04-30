package aanibrothers.tracker.io.module

import aanibrothers.tracker.io.*
import android.content.*
import coder.apps.space.library.helper.*

var Context.appOpenCount: Int
    get() = TinyDB(this).getInt("appOpenCount", 0)
    set(value) {
        TinyDB(this).putInt("appOpenCount", value)
    }

fun getPolicyLink(): String {
    return "https://sites.google.com/view/gpsmap-byaanibrothersinfotech/home"
}

fun getAdmobNativeId(): String {
    return if(BuildConfig.DEBUG) "ca-app-pub-3940256099942544/2247696110" else "ca-app-pub-4852962457779682/7424288358"
}

fun getAdmobInterId(): String {
    return if(BuildConfig.DEBUG) "ca-app-pub-3940256099942544/1033173712" else "ca-app-pub-4852962457779682/2228484186"
}

fun getAdmobBannerId(): String {
    return if(BuildConfig.DEBUG) "ca-app-pub-3940256099942544/9214589741" else "ca-app-pub-4852962457779682/9160041852"
}

fun getAdmobBannerMRECId(): String {
    return if(BuildConfig.DEBUG) "ca-app-pub-3940256099942544/6300978111" else "ca-app-pub-4852962457779682/7450062871"
}

fun getAdmobOpenId(): String {
    return if(BuildConfig.DEBUG) "ca-app-pub-3940256099942544/9257395921" else "ca-app-pub-4852962457779682/9453090865"
}

// registerAppId() was removed. AdMob APPLICATION_ID must come from the
// AndroidManifest — the SDK reads it once at MobileAds.initialize() and never
// re-reads it, so trying to overwrite it at runtime did nothing in production.
