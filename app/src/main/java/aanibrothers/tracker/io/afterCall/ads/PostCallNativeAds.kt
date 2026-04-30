package aanibrothers.tracker.io.afterCall.ads

import aanibrothers.tracker.io.BuildConfig
import aanibrothers.tracker.io.R
import aanibrothers.tracker.io.extension.firebaseASOEvent
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import aanibrothers.tracker.io.afterCall.PostCallApplication
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView

class PostCallNativeAds(var context: Context) {
    private var idNativeAds1: String = ""
    private var idNativeAds2: String = ""

    interface NativeListener {
        fun nativeFailed()
        fun nativeLoad()
    }

    init {
        if (BuildConfig.DEBUG) {
            idNativeAds1 = "ca-app-pub-3940256099942544/2247696110"
            idNativeAds2 = "ca-app-pub-3940256099942544/2247696110"
        } else {
            idNativeAds1 = "ca-app-pub-4852962457779682/4420084208"
            idNativeAds2 = "ca-app-pub-4852962457779682/4420084208"
        }
    }

    fun initNativeListener(nativeListener: NativeListener?) {
        PostCallApplication.setNativeListener(nativeListener)
    }

    fun loadPostNative() {
        loadNativeAds()
    }

    fun loadNativeAds(useSecondId: Boolean = false) {
        PostCallApplication.setNativePostCallLoading(true)
        PostCallApplication.setNativeFailedGooglePostCall(false)
        PostCallApplication.setNativeGoogleAdImpressionPostCall(false)
        val adUnitId = if (useSecondId) idNativeAds2 else idNativeAds1
        if (adUnitId.isNotEmpty()) {
            val adLoader = AdLoader.Builder(context, adUnitId)
                .forNativeAd { nativeAd ->
                    handleNativeLoaded(nativeAd)
                }
                .withAdListener(object : AdListener() {
                    override fun onAdFailedToLoad(adError: LoadAdError) {
                        val code = adError.code
                        val message = adError.message
                        context.firebaseASOEvent( "PostNativeF_$code", emptyMap())
                        Log.e("PostCall", "loadNativeAds onAdFailedToLoad code--> $code message--> $message ")
                        PostCallApplication.setAdNativeLoadTime(0L)
                        PostCallApplication.setNativeAdsPostCall(null)
                        PostCallApplication.setNativeFailedGooglePostCall(true)
                        PostCallApplication.setNativePostCallLoading(false)
                        PostCallApplication.getNativeListener()?.nativeFailed()
                    }

                    override fun onAdClicked() {
                        super.onAdClicked()
                        PostCallApplication.setOpenAdHide(true)
                    }

                    override fun onAdImpression() {
                        super.onAdImpression()
                        context.firebaseASOEvent( "PostNativeI", emptyMap())
                        PostCallApplication.setNativeGoogleAdImpressionPostCall(true)
                        PostCallApplication.setNativeAdsPostCall(null)
                        Log.e("PostCall", "NativeTag onAdImpression")
                    }
                })
                .build()
            adLoader.loadAd(AdRequest.Builder().build())
        } else {
            PostCallApplication.setNativeAdsPostCall(null)
            PostCallApplication.setNativePostCallLoading(false)
        }
    }

    private fun handleNativeLoaded(nativeAd: NativeAd) {
        PostCallApplication.setNativeAdsPostCall(nativeAd)
        PostCallApplication.setNativePostCallLoading(false)
        if (PostCallApplication.isCheckNotNull()) {
            PostCallApplication.getNativeListener()?.nativeLoad()
        }
        PostCallApplication.setAdNativeLoadTime(System.currentTimeMillis())
    }

    fun populateUnifiedNativeAdView(nativeAd: NativeAd, adView: NativeAdView, hideMedia: Boolean) {
        val textView = adView.findViewById<TextView>(R.id.ad_headline)
        textView.text = nativeAd.headline
        adView.headlineView = textView
        val textView2 = adView.findViewById<TextView>(R.id.ad_body)
        textView2.text = nativeAd.body
        adView.bodyView = textView2
        val imageView = adView.findViewById<ImageView>(R.id.ad_app_icon)
        val icon = nativeAd.icon
        imageView.setImageDrawable(icon?.drawable)
        adView.iconView = imageView
        imageView.visibility = if (nativeAd.icon == null) View.GONE else View.VISIBLE
        val textView3 = adView.findViewById<TextView>(R.id.ad_call_to_action)
        textView3.text = nativeAd.callToAction
        adView.callToActionView = textView3
        val relativeLayout = adView.findViewById<RelativeLayout>(R.id.iv_relative)
        if (hideMedia) {
            relativeLayout.visibility = View.GONE
        } else {
            adView.mediaView = adView.findViewById<MediaView>(R.id.ad_media)
            relativeLayout.visibility = View.VISIBLE
        }
        val isIcon = nativeAd.icon?.drawable != null
        adView.setNativeAd(nativeAd)
    }

    fun isNetworkOn(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkCapabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
        if (networkCapabilities != null && !networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) && !networkCapabilities.hasTransport(
                NetworkCapabilities.TRANSPORT_WIFI
            )
        ) {
            networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
        }
        return false
    }

    fun showLoadingLayoutNative(frameLayout: FrameLayout) {
        val inflate = LayoutInflater.from(context).inflate(
            R.layout.post_admob_native_tt_big_loading,
            null as ViewGroup?
        )
        val frameLayout2 = inflate as FrameLayout
        frameLayout2.findViewById<ShimmerFrameLayout>(R.id.shimmer_view_container).startLayoutAnimation()
        frameLayout.visibility = View.VISIBLE
        frameLayout.removeAllViews()
        frameLayout.addView(frameLayout2)
    }

    fun showNative(frameLayout: FrameLayout, nativeAd: NativeAd?, hideMedia: Boolean) {
        val hasAd = nativeAd != null
        Log.e("PostCall", "NativeTag showNative: $hasAd")
        if (nativeAd == null) {
            frameLayout.visibility = View.GONE
            frameLayout.removeAllViews()
            return
        }
        val inflate = LayoutInflater.from(context).inflate(
            R.layout.post_admob_native_tt_big,
            null as ViewGroup?
        )
        val nativeAdView = inflate as NativeAdView
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
        if (PostCallApplication.isNativeGoogleAdImpressionPostCall()) {
            PostCallApplication.setNativeAdsPostCall(null)
            PostCallApplication.setNativeGooglePostCall(false)
            PostCallApplication.setNativeGoogleAdImpressionPostCall(false)
        }
    }

    fun onDestroyAdView() {
        PostCallApplication.setNativeAdsPostCall(null)
        PostCallApplication.setNativeGooglePostCall(false)
    }
}
