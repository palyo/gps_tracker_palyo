package aanibrothers.tracker.io.module

import aanibrothers.tracker.io.*
import android.app.*
import android.content.*
import android.content.pm.*
import coder.apps.space.library.helper.*
import retrofit2.*

var OPEN_ID = ""
var INTER_ID = ""
var NATIVE_ID = ""
var BANNER_ID = ""

var Context.appOpenCount: Int
    get() = TinyDB(this).getInt("appOpenCount", 0)
    set(value) {
        TinyDB(this).putInt("appOpenCount", value)
    }

var Context.currentAdLevel: Int
    get() = TinyDB(this).getInt("currentAdLevel", 0)
    set(value) {
        TinyDB(this).putInt("currentAdLevel", value)
    }

fun Activity.init(callback: () -> Unit) {
    val apiService = ApiClient(this).client?.create(ApiService::class.java)
    val call = apiService?.getConfig("52_gpsmap/ad_manager.json")
    call?.enqueue(object : Callback<ConfigJson> {
        override fun onResponse(call: Call<ConfigJson>, response: Response<ConfigJson>) {
            if (response.isSuccessful) {
                response.body()?.let { habitJson ->
                    setAppJson(habitJson)
                    callback.invoke()
                }
            } else {
                val recorderJson = appJson()
                recorderJson?.let { initAds(it) }
                callback.invoke()
            }
        }

        override fun onFailure(call: Call<ConfigJson>, t: Throwable) {
            val recorderJson = appJson()
            recorderJson?.let { initAds(it) }
            callback.invoke()
        }
    })
}

fun Activity.setAppJson(appJson: ConfigJson) {
    TinyDB(this).putObject("appJson", appJson)
    initAds(appJson)
}

fun Context.appJson(): ConfigJson? {
    return TinyDB(this).getObject("appJson", ConfigJson::class.java)
}

fun Context.getPolicyLink(): String {
    return appJson()?.policyUrl ?: "https://privacy-and-policy-online.blogspot.com/2021/10/privacy-policy.html"
}

fun Activity.initAds(appJson: ConfigJson) {
    NATIVE_ID = if (BuildConfig.DEBUG) "ca-app-pub-3940256099942544/2247696110" else appJson.nativeID ?: ""
    INTER_ID = if (BuildConfig.DEBUG) "ca-app-pub-3940256099942544/1033173712" else appJson.interID ?: ""
    OPEN_ID = if (BuildConfig.DEBUG) "ca-app-pub-3940256099942544/9257395921" else appJson.openID ?: ""
    BANNER_ID = if (BuildConfig.DEBUG) "ca-app-pub-3940256099942544/9214589741" else appJson.bannerID ?: ""
    registerAppId(appJson.appId ?: "ca-app-pub-3940256099942544~3347511713")
}

fun Activity.registerAppId(appId: String) {
    try {
        val ai: ApplicationInfo = packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
        ai.metaData.putString("com.google.android.gms.ads.APPLICATION_ID", appId)
    } catch (e: PackageManager.NameNotFoundException) {
    } catch (e: NullPointerException) {
    }
}
