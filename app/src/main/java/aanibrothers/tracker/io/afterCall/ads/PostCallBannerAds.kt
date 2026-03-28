package aanibrothers.tracker.io.afterCall.ads

import aanibrothers.tracker.io.BuildConfig
import android.content.Context
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.ViewParent
import android.widget.FrameLayout
import aanibrothers.tracker.io.afterCall.PostCallApplication
import aanibrothers.tracker.io.extension.firebaseASOEvent
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError

class PostCallBannerAds {
    private var idBannerAds1: String = ""
    private var idBannerAds2: String = ""

    interface BannerListener {
        fun onBannerAdLoaded()
        fun onBannerFailed()
    }

    fun initBannerListener(bannerListener: BannerListener?) {
        PostCallApplication.setBannerListener(bannerListener)
    }

    fun loadBanner(context: Context, frameLayout: FrameLayout?, view: View?) {
        if (BuildConfig.DEBUG) {
            idBannerAds1 = "ca-app-pub-3940256099942544/6300978111"
            idBannerAds2 = "ca-app-pub-3940256099942544/6300978111"
        } else {
            idBannerAds1 = "ca-app-pub-4852962457779682/6269854553"
            idBannerAds2 = "ca-app-pub-4852962457779682/8241411773"
        }
        if (frameLayout != null && view != null) {
            frameLayout.visibility = View.VISIBLE
            view.visibility = View.VISIBLE
        }
        if (PostCallApplication.getBannerAdLoading()) {
            return
        }
        loadingBannerAds(context, frameLayout, view)
    }

    private fun loadingBannerAds(context: Context, frameLayout: FrameLayout?, view: View?) {
        if (PostCallApplication.getBannerAdView() != null && PostCallApplication.getBannerAdLoaded()) {
            if (frameLayout != null) {
                val bannerAdView = PostCallApplication.getBannerAdView()
                val parent: ViewParent? = bannerAdView?.parent
                if (parent is ViewGroup) {
                    parent.removeView(PostCallApplication.getBannerAdView())
                }
                frameLayout.removeAllViews()
                frameLayout.addView(PostCallApplication.getBannerAdView())
                frameLayout.visibility = View.VISIBLE
                view?.visibility = View.VISIBLE
            }
        } else if (idBannerAds1.isNotEmpty()) {
            val inlineAdSize = getInlineAdSize(context)
            val adView = AdView(context)
            adView.adUnitId = idBannerAds1
            val request = AdRequest.Builder().build()

            adView.setAdSize(inlineAdSize)
            PostCallApplication.setBannerAdImpression(false)
            PostCallApplication.setBannerAdLoaded(false)
            PostCallApplication.setBannerAdFailed(false)
            PostCallApplication.setBannerAdLoading(true)
            PostCallApplication.setBannerAdView(adView)
            adView.loadAd(request)
            if (frameLayout != null && view != null) {
                frameLayout.visibility = View.VISIBLE
                view.visibility = View.VISIBLE
            }
            adView.adListener = object : AdListener() {
                override fun onAdClosed() {
                }

                override fun onAdOpened() {
                }

                override fun onAdLoaded() {
                    val hasFrame = frameLayout != null
                    Log.e("PostCall", "loadingBannerAds1 onAdLoaded: $hasFrame")
                    PostCallApplication.setAdBannerLoadTime(System.currentTimeMillis())
                    PostCallApplication.setBannerAdImpression(false)
                    PostCallApplication.setBannerAdLoading(false)
                    PostCallApplication.setBannerAdLoaded(true)
                    if (frameLayout != null) {
                        frameLayout.removeAllViews()
                        frameLayout.addView(PostCallApplication.getBannerAdView())
                    } else {
                        PostCallApplication.getBannerListener()?.onBannerAdLoaded()
                    }
                }

                override fun onAdFailedToLoad(e: LoadAdError) {
                    val code = e.code
                    context.firebaseASOEvent( "PostBanner1F_$code", emptyMap())
                    val message = e.message
                    Log.e("PostCall", "loadingBannerAds1 onAdFailedToLoad $code $message  ")
                    PostCallApplication.setBannerAdView(null)
                    PostCallApplication.setBannerAdLoaded(false)
                    if (idBannerAds2.isNotEmpty()) {
                        Log.e("PostCall", "loadingBannerAds onAdFailedToLoad reload banner")
                        loadingBannerFailedAds(context, frameLayout, view)
                    } else {
                        PostCallApplication.setBannerAdLoading(false)
                        PostCallApplication.setBannerAdFailed(true)
                        if (frameLayout != null && view != null) {
                            hideBannerView(frameLayout, view)
                        } else {
                            PostCallApplication.getBannerListener()?.onBannerFailed()
                        }
                        if (e.code == 0 || e.code == 2) {
                            PostCallApplication.setReLoadBannerAds(true)
                        }
                    }
                    super.onAdFailedToLoad(e)
                }

                override fun onAdImpression() {
                    super.onAdImpression()
                    context.firebaseASOEvent( "PostBanner1I", emptyMap())
                    PostCallApplication.setBannerAdImpression(true)
                }
            }
        } else if (idBannerAds2.isNotEmpty()) {
            loadingBannerFailedAds(context, frameLayout, view)
        } else {
            PostCallApplication.setBannerAdView(null)
            PostCallApplication.setBannerAdLoading(false)
            PostCallApplication.setBannerAdFailed(false)
            PostCallApplication.setBannerAdLoaded(false)
            if (frameLayout != null && view != null) {
                hideBannerView(frameLayout, view)
            } else {
                PostCallApplication.getBannerListener()?.onBannerFailed()
            }
        }
    }

    fun loadingBannerFailedAds(context: Context, frameLayout: FrameLayout?, view: View?) {
        if (idBannerAds2.isNotEmpty()) {
            PostCallApplication.setBannerAdLoading(true)
            val inlineAdSize = getInlineAdSize(context)
            val adView = AdView(context)
            adView.adUnitId = idBannerAds2
            val request = AdRequest.Builder().build()

            adView.setAdSize(inlineAdSize)
            adView.loadAd(request)
            PostCallApplication.setBannerAdView(adView)
            if (frameLayout != null && view != null) {
                frameLayout.visibility = View.VISIBLE
                view.visibility = View.VISIBLE
            }
            adView.adListener = object : AdListener() {
                override fun onAdClosed() {
                }

                override fun onAdOpened() {
                }

                override fun onAdLoaded() {
                    Log.e(
                        "PostCall",
                        "BannerAds2 onAdLoaded}"
                    )
                    PostCallApplication.setAdBannerLoadTime(System.currentTimeMillis())
                    PostCallApplication.setBannerAdImpression(false)
                    PostCallApplication.setBannerAdLoading(false)
                    PostCallApplication.setBannerAdLoaded(true)
                    if (frameLayout != null) {
                        frameLayout.removeAllViews()
                        frameLayout.addView(PostCallApplication.getBannerAdView())
                    } else {
                        PostCallApplication.getBannerListener()?.onBannerAdLoaded()
                    }
                }

                override fun onAdClicked() {
                    super.onAdClicked()
                }

                override fun onAdFailedToLoad(e: LoadAdError) {
                    val code = e.code
                    val message = e.message
                    context.firebaseASOEvent("PostBanner2F_$code", emptyMap())
                    Log.e(
                        "PostCall",
                        "BannerAds2 onAdFailedToLoad $code $message  ${PostCallApplication.getBannerListener()}"
                    )
                    PostCallApplication.setBannerAdView(null)
                    PostCallApplication.setBannerAdLoaded(false)
                    PostCallApplication.setBannerAdLoading(false)
                    PostCallApplication.setBannerAdFailed(true)
                    if (PostCallApplication.getBannerListener() != null) {
                        PostCallApplication.getBannerListener()?.onBannerFailed()
                    } else {
                        PostCallNativeAds(context).loadPostNative()
                    }
                    super.onAdFailedToLoad(e)
                }

                override fun onAdImpression() {
                    super.onAdImpression()
                    context.firebaseASOEvent( "PostBanner2I", emptyMap())
                    PostCallApplication.setBannerAdImpression(true)
                }
            }
            return
        }
        if (frameLayout != null && view != null) {
            hideBannerView(frameLayout, view)
        } else {
            PostCallApplication.getBannerListener()?.onBannerFailed()
        }
        PostCallApplication.setBannerAdView(null)
        PostCallApplication.setBannerAdLoading(false)
        PostCallApplication.setBannerAdFailed(false)
        PostCallApplication.setBannerAdLoaded(false)
    }

    private fun getInlineAdSize(context: Context): AdSize {
        return AdSize.getPortraitInlineAdaptiveBannerAdSize(context, -1)
    }

    fun hideBannerView(frameLayout: FrameLayout?, view: View?) {
        if (frameLayout == null || view == null) {
            return
        }
        frameLayout.removeAllViews()
        frameLayout.visibility = View.GONE
        view.visibility = View.GONE
    }

    fun onAdExpired(context: Context) {
        if (PostCallApplication.getBannerAdView() != null) {
            val bannerAdView = PostCallApplication.getBannerAdView()
            bannerAdView?.destroy()
            PostCallApplication.setBannerAdView(null)
            PostCallApplication.setBannerAdImpression(false)
            PostCallApplication.setBannerAdLoaded(false)
            PostCallApplication.setBannerAdFailed(false)
            PostCallApplication.setAdBannerLoadTime(0L)
            loadBanner(context, null, null)
        }
    }

    fun onDestroyAd() {
        if (PostCallApplication.getBannerAdView() == null || !PostCallApplication.getBannerAdImpression()) {
            return
        }
        val bannerAdView = PostCallApplication.getBannerAdView()
        bannerAdView?.destroy()
        PostCallApplication.setBannerAdView(null)
        PostCallApplication.setBannerAdImpression(false)
        PostCallApplication.setBannerAdLoaded(false)
        PostCallApplication.setBannerAdFailed(false)
        PostCallApplication.setAdBannerLoadTime(0L)
    }
}
