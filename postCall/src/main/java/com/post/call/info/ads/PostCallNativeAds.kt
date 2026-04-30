package com.post.call.info.ads

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.VideoOptions
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.nativead.NativeAdView
import com.post.call.info.PostCallApplication
import com.post.call.info.R
import com.post.call.info.utils.firebaseASOEvent
import com.post.call.info.utils.isDebug
import com.post.call.info.utils.isNetworkOn

class PostCallNativeAds(var context: Context) {

    private val idNativeAds1: String = if (isDebug()) {
        context.getString(R.string.id_live_post_call_native_debug)
    } else {
        context.getString(R.string.id_live_post_call_native)
    }

    interface NativeListener {
        fun nativeFailed()
        fun nativeLoad()
    }

    fun initNativeListener(nativeListener: NativeListener) {
        PostCallApplication.nativeListener = nativeListener
    }

    fun loadPostNative() {
        Log.e("PostCall", "NativeTag loading fun call")
        loadNativeAds(false)
    }

    fun loadNativeAds(useSecondId: Boolean = false) {
        PostCallApplication.isNativePostCallLoading = true
        PostCallApplication.isNativeFailedGooglePostCall = false
        PostCallApplication.isNativeGoogleAdImpressionPostCall = false
        if (!context.isNetworkOn()) {
            context.firebaseASOEvent("post_n_nw_off")
            Log.e("PostCall", "NativeTag no network, skipping native load")
            PostCallApplication.isNativePostCallLoading = false
            PostCallApplication.isNativeFailedGooglePostCall = true
            PostCallApplication.nativeListener?.nativeFailed()
            return
        }
        if (idNativeAds1.isNotEmpty()) {
            val nativeAdOptions = NativeAdOptions.Builder()
                .setMediaAspectRatio(NativeAdOptions.NATIVE_MEDIA_ASPECT_RATIO_ANY)
                .setVideoOptions(VideoOptions.Builder().setStartMuted(true).build())
                .build()

            val adLoader = AdLoader.Builder(context, idNativeAds1)
                .forNativeAd { nativeAd ->
                    Log.e("PostCall", "NativeTag is Loaded")
                    PostCallApplication.nativeAdsPostCall = nativeAd
                    PostCallApplication.isNativePostCallLoading = false
                    if (PostCallApplication.isCheckNotNull()) {
                        PostCallApplication.nativeListener?.nativeLoad()
                    }
                    PostCallApplication.adNativeLoadTime = System.currentTimeMillis()
                }
                .withAdListener(object : AdListener() {
                    override fun onAdFailedToLoad(adError: LoadAdError) {
                        context.firebaseASOEvent("post_n_fail_${adError.code}")
                        Log.e("PostCall", "NativeTag onAdFailedToLoad code--> ${adError.code} message--> ${adError.message}")
                        PostCallApplication.adNativeLoadTime = 0L
                        PostCallApplication.nativeAdsPostCall = null
                        PostCallApplication.isNativeFailedGooglePostCall = true
                        PostCallApplication.isNativePostCallLoading = false
                        PostCallApplication.nativeListener?.nativeFailed()
                    }

                    override fun onAdClicked() {
                        PostCallApplication.setOpenAdHide(true)
                    }

                    override fun onAdImpression() {
                        context.firebaseASOEvent("post_n_imp")
                        PostCallApplication.isNativeGoogleAdImpressionPostCall = true
                        PostCallApplication.nativeAdsPostCall = null
                        Log.e("PostCall", "NativeTag onAdImpression----")
                    }

                    override fun onAdClosed() {}
                })
                .withNativeAdOptions(nativeAdOptions)
                .build()

            adLoader.loadAd(AdRequest.Builder().build())
        } else {
            PostCallApplication.nativeAdsPostCall = null
            PostCallApplication.isNativePostCallLoading = false
        }
    }

    fun populateUnifiedNativeAdView(nativeAd: NativeAd, adView: NativeAdView, hideMedia: Boolean): NativeAdView {
        val headline = adView.findViewById<TextView>(R.id.ad_headline)
        headline.text = nativeAd.headline
        adView.headlineView = headline

        val body = adView.findViewById<TextView>(R.id.ad_body)
        body.text = nativeAd.body
        adView.bodyView = body

        val icon = adView.findViewById<ImageView>(R.id.ad_app_icon)
        icon.setImageDrawable(nativeAd.icon?.drawable)
        adView.iconView = icon
        icon.visibility = if (nativeAd.icon == null) View.GONE else View.VISIBLE

        val cta = adView.findViewById<TextView>(R.id.ad_call_to_action)
        cta.text = nativeAd.callToAction
        adView.callToActionView = cta

        val relativeLayout = adView.findViewById<RelativeLayout>(R.id.iv_relative)
        if (hideMedia) {
            relativeLayout.visibility = View.GONE
        } else {
            adView.mediaView = adView.findViewById<MediaView>(R.id.ad_media)
            relativeLayout.visibility = View.VISIBLE
        }
        Log.e("PostCall", "NativeTag data adicon-> ${nativeAd.icon?.drawable != null}")
        adView.setNativeAd(nativeAd)
        return adView
    }

    fun showLoadingLayoutNative(frameLayout: FrameLayout) {
        val inflate = LayoutInflater.from(context).inflate(R.layout.post_admob_native_loading, null) as FrameLayout
        (inflate.findViewById<ShimmerFrameLayout>(R.id.shimmer_view_container)).startLayoutAnimation()
        frameLayout.visibility = View.VISIBLE
        frameLayout.removeAllViews()
        frameLayout.addView(inflate)
    }

    fun showNative(frameLayout: FrameLayout, nativeAd: NativeAd?, hideMedia: Boolean) {
        Log.e("PostCall", "NativeTag showNative-->> ${nativeAd != null}")
        if (nativeAd == null) {
            frameLayout.visibility = View.GONE
            frameLayout.removeAllViews()
            return
        }
        val nativeAdView = LayoutInflater.from(context).inflate(R.layout.post_admob_native, null) as NativeAdView
        try {
            populateUnifiedNativeAdView(nativeAd, nativeAdView, hideMedia)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        frameLayout.removeAllViews()
        frameLayout.addView(nativeAdView)
        frameLayout.visibility = View.VISIBLE
    }

    fun onDestroyAd() {
        val isImpression = PostCallApplication.isNativeGoogleAdImpressionPostCall
        Log.e("PostCall", "NativeTag google onDestroyAd isNativeGoogleAdImpressionPostCall $isImpression")
        if (isImpression) {
            PostCallApplication.nativeAdsPostCall = null
            PostCallApplication.isNativeGooglePostCall = false
            PostCallApplication.isNativeGoogleAdImpressionPostCall = false
        }
    }

    fun onDestroyAdView() {
        PostCallApplication.nativeAdsPostCall = null
        PostCallApplication.isNativeGooglePostCall = false
    }
}
