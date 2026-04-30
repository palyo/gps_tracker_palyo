package com.post.call.info

import android.app.Application
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.nativead.NativeAd
import com.post.call.info.ads.PostCallBannerAds
import com.post.call.info.ads.PostCallNativeAds


open class PostCallApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        context = this
    }

    companion object {
        var context: Context? = null

        var adBannerLoadTime: Long = 0L
        var adNativeLoadTime: Long = 0L
        var bannerAdFailed: Boolean = false
        var bannerAdImpression: Boolean = false
        var bannerAdLoaded: Boolean = false
        var bannerAdLoading: Boolean = false
        var bannerAdView: AdView? = null
        var bannerListener: PostCallBannerAds.BannerListener? = null
        var isCallingStart: Boolean = false
        var isNativeFailedGooglePostCall: Boolean = false
        var isNativeGoogleAdImpressionPostCall: Boolean = false
        var isNativeGooglePostCall: Boolean = false
        var isNativePostCallLoading: Boolean = false
        var isReLoadBannerAds: Boolean = false
        var isStartedService: Boolean = false
        var nativeAdsPostCall: NativeAd? = null
        var nativeListener: PostCallNativeAds.NativeListener? = null
        var adNativeExpirationTime: Long = 3600000L
        var adBannerExpirationTime: Long = 3300000L

        fun setOpenAdHide(z: Boolean) {}

        fun isCheckNotNull(): Boolean = nativeListener != null

        fun destroyUnusedNative() {
            val nativeAd = nativeAdsPostCall
            if (nativeAd != null && !isNativeGoogleAdImpressionPostCall) {
                nativeAd.destroy()
                Log.e("PostCall", "NativeTag destroyUnusedNative: destroyed unused preloaded native ad")
            }
            nativeAdsPostCall = null
            isNativeGooglePostCall = false
            isNativeGoogleAdImpressionPostCall = false
            adNativeLoadTime = 0L
            isNativePostCallLoading = false
        }
    }
}
