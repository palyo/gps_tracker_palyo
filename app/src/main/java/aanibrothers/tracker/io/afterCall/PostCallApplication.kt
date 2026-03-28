package aanibrothers.tracker.io.afterCall

import android.content.Context
import androidx.multidex.MultiDexApplication
import aanibrothers.tracker.io.afterCall.ads.PostCallBannerAds
import aanibrothers.tracker.io.afterCall.ads.PostCallNativeAds
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.nativead.NativeAd

open class PostCallApplication : MultiDexApplication() {
    override fun onCreate() {
        super.onCreate()
        appContext = this
    }

    companion object {
        private var appContext: Context? = null
        private var adBannerLoadTime: Long = 0L
        private var adNativeLoadTime: Long = 0L
        private var bannerAdFailed = false
        private var bannerAdImpression = false
        private var bannerAdLoaded = false
        private var bannerAdLoading = false
        private var bannerAdView: AdView? = null
        private var bannerListener: PostCallBannerAds.BannerListener? = null
        private var isCallingStart = false
        private var isNativeFailedGooglePostCall = false
        private var isNativeGoogleAdImpressionPostCall = false
        private var isNativeGooglePostCall = false
        private var isNativePostCallLoading = false
        private var isReLoadBannerAds = false
        private var isStartedService = false
        private var nativeAdsPostCall: NativeAd? = null
        private var nativeListener: PostCallNativeAds.NativeListener? = null
        private var adNativeExpirationTime: Long = 3600000L
        private var adBannerExpirationTime: Long = 3300000L

        fun getContext(): Context? {
            return appContext
        }

        fun setOpenAdHide(z: Boolean) {
        }

        fun isCallingStart(): Boolean {
            return isCallingStart
        }

        fun setCallingStart(z: Boolean) {
            isCallingStart = z
        }

        fun isStartedService(): Boolean {
            return isStartedService
        }

        fun setStartedService(z: Boolean) {
            isStartedService = z
        }

        fun getNativeAdsPostCall(): NativeAd? {
            return nativeAdsPostCall
        }

        fun setNativeAdsPostCall(nativeAd: NativeAd?) {
            nativeAdsPostCall = nativeAd
        }

        fun isNativePostCallLoading(): Boolean {
            return isNativePostCallLoading
        }

        fun setNativePostCallLoading(z: Boolean) {
            isNativePostCallLoading = z
        }

        fun getAdNativeLoadTime(): Long {
            return adNativeLoadTime
        }

        fun setAdNativeLoadTime(j: Long) {
            adNativeLoadTime = j
        }

        fun getAdNativeExpirationTime(): Long {
            return adNativeExpirationTime
        }

        fun getNativeListener(): PostCallNativeAds.NativeListener? {
            return nativeListener
        }

        fun setNativeListener(listener: PostCallNativeAds.NativeListener?) {
            nativeListener = listener
        }

        fun isNativeFailedGooglePostCall(): Boolean {
            return isNativeFailedGooglePostCall
        }

        fun setNativeFailedGooglePostCall(z: Boolean) {
            isNativeFailedGooglePostCall = z
        }

        fun isNativeGooglePostCall(): Boolean {
            return isNativeGooglePostCall
        }

        fun setNativeGooglePostCall(z: Boolean) {
            isNativeGooglePostCall = z
        }

        fun isNativeGoogleAdImpressionPostCall(): Boolean {
            return isNativeGoogleAdImpressionPostCall
        }

        fun setNativeGoogleAdImpressionPostCall(z: Boolean) {
            isNativeGoogleAdImpressionPostCall = z
        }

        fun getBannerAdView(): AdView? {
            return bannerAdView
        }

        fun setBannerAdView(adView: AdView?) {
            bannerAdView = adView
        }

        fun getBannerListener(): PostCallBannerAds.BannerListener? {
            return bannerListener
        }

        fun setBannerListener(listener: PostCallBannerAds.BannerListener?) {
            bannerListener = listener
        }

        fun getBannerAdLoading(): Boolean {
            return bannerAdLoading
        }

        fun setBannerAdLoading(z: Boolean) {
            bannerAdLoading = z
        }

        fun getBannerAdLoaded(): Boolean {
            return bannerAdLoaded
        }

        fun setBannerAdLoaded(z: Boolean) {
            bannerAdLoaded = z
        }

        fun getBannerAdFailed(): Boolean {
            return bannerAdFailed
        }

        fun setBannerAdFailed(z: Boolean) {
            bannerAdFailed = z
        }

        fun getBannerAdImpression(): Boolean {
            return bannerAdImpression
        }

        fun setBannerAdImpression(z: Boolean) {
            bannerAdImpression = z
        }

        fun getAdBannerLoadTime(): Long {
            return adBannerLoadTime
        }

        fun setAdBannerLoadTime(j: Long) {
            adBannerLoadTime = j
        }

        fun getAdBannerExpirationTime(): Long {
            return adBannerExpirationTime
        }

        fun setAdBannerExpirationTime(j: Long) {
            adBannerExpirationTime = j
        }

        fun isReLoadBannerAds(): Boolean {
            return isReLoadBannerAds
        }

        fun setReLoadBannerAds(z: Boolean) {
            isReLoadBannerAds = z
        }

        fun isCheckNotNull(): Boolean {
            return getNativeListener() != null
        }
    }
}
