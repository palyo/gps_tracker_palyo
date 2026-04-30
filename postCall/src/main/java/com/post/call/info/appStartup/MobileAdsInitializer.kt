package com.post.call.info.appStartup

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.startup.Initializer
import com.google.android.gms.ads.MobileAds

class MobileAdsInitializer : Initializer<Unit> {

    override fun create(context: Context) {
        Handler(Looper.getMainLooper()).post {
            MobileAds.initialize(context)
            Log.e("Msg", "Mobile Ads Initialize: Called")
        }
    }

    override fun dependencies(): List<Class<out Initializer<*>>> {
        return listOf(WebViewInitializer::class.java)
    }
}