package com.post.call.info.ads

import android.content.Context
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.post.call.info.PostCallApplication
import com.post.call.info.R
import com.post.call.info.utils.firebaseASOEvent
import com.post.call.info.utils.isDebug
import com.post.call.info.utils.isNetworkOn
import kotlin.math.roundToInt

class PostCallBannerAds {



    private var idBannerAds1: String = ""
    private var idBannerAds2: String = ""

    interface BannerListener {
        fun onBannerAdLoaded()
        fun onBannerFailed()
    }

    fun initBannerListener(bannerListener: BannerListener) {
        PostCallApplication.bannerListener = bannerListener
    }

    fun loadBanner(context: Context, frameLayout: FrameLayout?, view: View?) {
        if (isDebug()) {
            idBannerAds1 = context.getString(R.string.id_live_post_call_banner_debug)
            idBannerAds2 = context.getString(R.string.id_live_post_call_banner_failed_debug)
        } else {
            idBannerAds1 = context.getString(R.string.id_live_post_call_banner)
            idBannerAds2 = context.getString(R.string.id_live_post_call_banner_failed)
        }
        Log.e("PostCall", "BannerTag isAdsOn")
        if (!context.isNetworkOn()) {
            context.firebaseASOEvent("post_b_nw_off")
            Log.e("PostCall", "BannerTag no network, skipping banner load")
            if (frameLayout != null && view != null) hideBannerView(frameLayout, view)
            return
        }
        if (frameLayout != null && view != null) {
            frameLayout.visibility = View.VISIBLE
            view.visibility = View.VISIBLE
        }
        if (PostCallApplication.bannerAdLoading) return
        loadingBannerAds(context, frameLayout, view)
    }

    private fun loadingBannerAds(context: Context, frameLayout: FrameLayout?, view: View?) {
        if (PostCallApplication.bannerAdView != null && PostCallApplication.bannerAdLoaded) {
            if (frameLayout != null) {
                val bannerAdView = PostCallApplication.bannerAdView!!
                (bannerAdView.parent as? ViewGroup)?.removeView(bannerAdView)
                frameLayout.removeAllViews()
                frameLayout.addView(bannerAdView)
                frameLayout.visibility = View.VISIBLE
                view?.visibility = View.VISIBLE
            }
            Log.e("PostCall", "BannerTag loadingBannerAds call return")
        } else if (idBannerAds1.isNotEmpty()) {
            val inlineAdSize = getInlineAdSize(context)
            Log.e("PostCall", "BannerTag 1 loading")
            val adView = AdView(context)
            adView.adUnitId = idBannerAds1
            adView.setAdSize(inlineAdSize)
            PostCallApplication.bannerAdImpression = false
            PostCallApplication.bannerAdLoaded = false
            PostCallApplication.bannerAdFailed = false
            PostCallApplication.bannerAdLoading = true
            PostCallApplication.bannerAdView = adView
            adView.loadAd(AdRequest.Builder().build())
            if (frameLayout != null && view != null) {
                frameLayout.visibility = View.VISIBLE
                view.visibility = View.VISIBLE
            }
            adView.adListener = object : AdListener() {
                override fun onAdLoaded() {
                    Log.e("PostCall", "BannerTag 1 onAdLoaded frameLayout-> ${frameLayout != null}")
                    PostCallApplication.adBannerLoadTime = System.currentTimeMillis()
                    PostCallApplication.bannerAdImpression = false
                    PostCallApplication.bannerAdLoading = false
                    PostCallApplication.bannerAdLoaded = true
                    if (frameLayout != null) {
                        frameLayout.removeAllViews()
                        frameLayout.addView(PostCallApplication.bannerAdView)
                    } else {
                        PostCallApplication.bannerListener?.onBannerAdLoaded()
                    }
                }

                override fun onAdClicked() {}
                override fun onAdClosed() {}
                override fun onAdOpened() {}

                override fun onAdFailedToLoad(e: LoadAdError) {
                    context.firebaseASOEvent("post_b1_fail_${e.code}")
                    Log.e("PostCall", "BannerTag 1 onAdFailedToLoad ${e.code} ${e.message}")
                    PostCallApplication.bannerAdView = null
                    PostCallApplication.bannerAdLoaded = false
                    if (idBannerAds2.isNotEmpty()) {
                        Log.e("PostCall", "BannerTag 1 onAdFailedToLoad reload banner")
                        loadingBannerFailedAds(context, frameLayout, view)
                    } else {
                        PostCallApplication.bannerAdLoading = false
                        PostCallApplication.bannerAdFailed = true
                        if (frameLayout != null && view != null) hideBannerView(frameLayout, view)
                        else PostCallApplication.bannerListener?.onBannerFailed()
                        if (e.code == 0 || e.code == 2) PostCallApplication.isReLoadBannerAds = true
                    }
                }

                override fun onAdImpression() {
                    context.firebaseASOEvent("post_b1_imp")
                    PostCallApplication.bannerAdImpression = true
                }
            }
        } else if (idBannerAds2.isNotEmpty()) {
            loadingBannerFailedAds(context, frameLayout, view)
        } else {
            PostCallApplication.bannerAdView = null
            PostCallApplication.bannerAdLoading = false
            PostCallApplication.bannerAdFailed = false
            PostCallApplication.bannerAdLoaded = false
            if (frameLayout != null && view != null) hideBannerView(frameLayout, view)
            else PostCallApplication.bannerListener?.onBannerFailed()
        }
    }

    fun loadingBannerFailedAds(context: Context, frameLayout: FrameLayout?, view: View?) {
        if (idBannerAds2.isNotEmpty()) {
            PostCallApplication.bannerAdLoading = true
            val inlineAdSize = getInlineAdSize(context)
            Log.e("PostCall", "BannerTag 2 loading")
            val adView = AdView(context)
            adView.setAdUnitId(idBannerAds2)
            adView.setAdSize(inlineAdSize)
            adView.loadAd(AdRequest.Builder().build())
            PostCallApplication.bannerAdView = adView
            if (frameLayout != null && view != null) {
                frameLayout.visibility = View.VISIBLE
                view.visibility = View.VISIBLE
            }
            adView.adListener = object : AdListener() {
                override fun onAdLoaded() {
                    PostCallApplication.adBannerLoadTime = System.currentTimeMillis()
                    PostCallApplication.bannerAdImpression = false
                    PostCallApplication.bannerAdLoading = false
                    PostCallApplication.bannerAdLoaded = true
                    if (frameLayout != null) {
                        frameLayout.removeAllViews()
                        frameLayout.addView(PostCallApplication.bannerAdView)
                    } else {
                        PostCallApplication.bannerListener?.onBannerAdLoaded()
                    }
                }

                override fun onAdClicked() {}
                override fun onAdClosed() {}
                override fun onAdOpened() {}

                override fun onAdFailedToLoad(e: LoadAdError) {
                    context.firebaseASOEvent("post_b2_fail_${e.code}")
                    Log.e("PostCall", "BannerTag 2 onAdFailedToLoad ${e.code} ${e.message}")
                    PostCallApplication.bannerAdView = null
                    PostCallApplication.bannerAdLoaded = false
                    PostCallApplication.bannerAdLoading = false
                    PostCallApplication.bannerAdFailed = true
                    PostCallApplication.bannerListener?.onBannerFailed()
                    if (e.code == 0 || e.code == 2) PostCallApplication.isReLoadBannerAds = true
                }

                override fun onAdImpression() {
                    context.firebaseASOEvent("post_b2_imp")
                    PostCallApplication.bannerAdImpression = true
                }
            }
            return
        }
        if (frameLayout != null && view != null) hideBannerView(frameLayout, view)
        else PostCallApplication.bannerListener?.onBannerFailed()
        PostCallApplication.bannerAdView = null
        PostCallApplication.bannerAdLoading = false
        PostCallApplication.bannerAdFailed = false
        PostCallApplication.bannerAdLoaded = false
    }

    private fun getInlineAdSize(context: Context): AdSize {
        val displayMetrics: DisplayMetrics = context.resources.displayMetrics
        val widthDp = (displayMetrics.widthPixels / displayMetrics.density).roundToInt()
        return AdSize.getPortraitInlineAdaptiveBannerAdSize(context, widthDp)
    }

    fun hideBannerView(frameLayout: FrameLayout, view: View) {
        frameLayout.removeAllViews()
        frameLayout.visibility = View.GONE
        view.visibility = View.GONE
    }

    fun onAdExpired(context: Context) {
        PostCallApplication.bannerAdView?.destroy()
        PostCallApplication.bannerAdView = null
        PostCallApplication.bannerAdImpression = false
        PostCallApplication.bannerAdLoaded = false
        PostCallApplication.bannerAdFailed = false
        PostCallApplication.adBannerLoadTime = 0L
        loadBanner(context, null, null)
    }

    fun onDestroyAd() {
        if (PostCallApplication.bannerAdView == null || !PostCallApplication.bannerAdImpression) return
        PostCallApplication.bannerAdView?.destroy()
        PostCallApplication.bannerAdView = null
        PostCallApplication.bannerAdImpression = false
        PostCallApplication.bannerAdLoaded = false
        PostCallApplication.bannerAdFailed = false
        PostCallApplication.adBannerLoadTime = 0L
    }
}
