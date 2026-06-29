package aanibrothers.tracker.io.module

import aanibrothers.tracker.io.analytics.AdPlacement
import aanibrothers.tracker.io.analytics.Analytics
import aanibrothers.tracker.io.analytics.AnalyticsEvent
import aanibrothers.tracker.io.databinding.AdUnifiedBannerBinding
import aanibrothers.tracker.io.databinding.AdUnifiedMediumBinding
import aanibrothers.tracker.io.databinding.AdUnifiedSmallBinding
import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdLoader
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdLoaderCallback
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdRequest


var nativeAd: NativeAd? = null

fun Activity.preloadNative() {
    try {
        val start = System.currentTimeMillis()
        val request = NativeAdRequest
            .Builder(getAdmobNativeId(), listOf(NativeAd.NativeAdType.NATIVE))
            .build()
        NativeAdLoader.load(request, object : NativeAdLoaderCallback {
            override fun onNativeAdLoaded(ad: NativeAd) {
                val seconds = (System.currentTimeMillis() - start) / 1000.0
                Log.e("AdTiming", "preloadNative loaded in $seconds seconds")
                nativeAd = ad
                Log.e(TAG, "preloadNative:onLoaded ")
            }

            override fun onAdFailedToLoad(adError: LoadAdError) {
                Log.e(TAG, "onAdFailedToLoad:preloadNative: ${adError.code} ${adError.message}")
            }
        })
    } catch (_: Exception) {
    }
}

fun Activity.viewNativeBanner(container: AdsView) =
    viewNativeBanner(container = container, placement = AdPlacement.UNKNOWN)

fun Activity.viewNativeMedium(container: AdsView) =
    viewNativeMedium(container = container, placement = AdPlacement.UNKNOWN)

fun Activity.viewNativeSmall(container: AdsView) =
    viewNativeSmall(container = container, placement = AdPlacement.UNKNOWN)

/**
 * Placement-aware overloads — prefer these at call sites so AdMob revenue
 * dashboards can break down impressions by screen.
 *
 * Each overload emits ad_impression_custom the moment populateAdView* runs.
 * If the native cache was hot, that's a synchronous impression. If we had to
 * load fresh, the event fires inside onLoad — still on the main thread, still
 * after the ad view is populated.
 */
fun Activity.viewNativeBanner(container: AdsView, placement: String) {
    val emit = {
        Analytics.log(AnalyticsEvent.AdImpression(placement = placement, format = "native_banner"))
    }
    if (nativeAd != null) {
        populateAdViewBanner(nativeAd, container = container)
        emit()
        preloadNative()
    } else {
        loadNative(object : OnNativeLoad {
            override fun onLoad(nativeAd: NativeAd) {
                populateAdViewBanner(nativeAd, container = container)
                emit()
                preloadNative()
            }

            override fun onFail() {
                Analytics.log(
                    AnalyticsEvent.AdFailedToLoad(
                        placement = placement,
                        format = "native_banner",
                        errorCode = -1
                    )
                )
            }
        })
    }
}

fun Activity.viewNativeMedium(container: AdsView, placement: String) {
    val emit = {
        Analytics.log(AnalyticsEvent.AdImpression(placement = placement, format = "native_medium"))
    }
    if (nativeAd != null) {
        populateAdViewMedium(nativeAd, container = container)
        emit()
        preloadNative()
    } else {
        loadNative(object : OnNativeLoad {
            override fun onLoad(nativeAd: NativeAd) {
                populateAdViewMedium(nativeAd, container = container)
                emit()
                preloadNative()
            }

            override fun onFail() {
                Analytics.log(
                    AnalyticsEvent.AdFailedToLoad(
                        placement = placement,
                        format = "native_medium",
                        errorCode = -1
                    )
                )
            }
        })
    }
}

fun Activity.viewNativeSmall(container: AdsView, placement: String) {
    val emit = {
        Analytics.log(AnalyticsEvent.AdImpression(placement = placement, format = "native_small"))
    }
    if (nativeAd != null) {
        populateAdViewSmall(nativeAd, container = container)
        emit()
        preloadNative()
    } else {
        loadNative(object : OnNativeLoad {
            override fun onLoad(nativeAd: NativeAd) {
                populateAdViewSmall(nativeAd, container = container)
                emit()
                preloadNative()
            }

            override fun onFail() {
                Analytics.log(
                    AnalyticsEvent.AdFailedToLoad(
                        placement = placement,
                        format = "native_small",
                        errorCode = -1
                    )
                )
            }
        })
    }
}

fun Context.populateAdViewMedium(unifiedNativeAd: NativeAd?, container: AdsView) {
    val layoutInflater: LayoutInflater = LayoutInflater.from(this)
    val binding = AdUnifiedMediumBinding.inflate(layoutInflater)
    val ad = unifiedNativeAd ?: return
    binding.unified.iconView = binding.icon
    binding.unified.headlineView = binding.primary
    binding.unified.bodyView = binding.body
    binding.unified.callToActionView = binding.cta

    binding.unified.starRatingView = binding.ratingBar
    ad.starRating.let {
        if (it != null && it > 0.0) (binding.unified.starRatingView as RatingBar?)?.rating = it.toFloat() else (binding.unified.starRatingView as RatingBar?)?.rating = 0.0f
    }
    (binding.unified.headlineView as TextView?)?.text = ad.headline
    if (ad.body == null) {
        binding.unified.bodyView?.visibility = View.INVISIBLE
    } else {
        binding.unified.bodyView?.visibility = View.VISIBLE
        (binding.unified.bodyView as TextView?)?.text = ad.body
    }
    if (ad.callToAction == null) {
        binding.unified.callToActionView?.visibility = View.INVISIBLE
    } else {
        binding.unified.callToActionView?.visibility = View.VISIBLE
        (binding.unified.callToActionView as TextView?)?.text = ad.callToAction
    }
    if (ad.icon == null) {
        binding.unified.iconView?.visibility = View.GONE
    } else {
        (binding.unified.iconView as ImageView?)?.setImageDrawable(ad.icon?.drawable)
        binding.unified.iconView?.visibility = View.VISIBLE
    }
    try {
        binding.unified.registerNativeAd(ad, binding.mediaView)
    } catch (e2: Exception) {
        e2.printStackTrace()
    }
    container.removeAllViews()
    container.addView(binding.root)
}

fun Context.populateAdViewSmall(unifiedNativeAd: NativeAd?, container: AdsView) {
    val layoutInflater: LayoutInflater = LayoutInflater.from(this)
    val binding = AdUnifiedSmallBinding.inflate(layoutInflater)
    val ad = unifiedNativeAd ?: return
    binding.unified.iconView = binding.adAppIcon
    binding.unified.headlineView = binding.adHeadline
    binding.unified.bodyView = binding.adBody
    binding.unified.callToActionView = binding.adCallToAction

    binding.unified.starRatingView = binding.adStars
    ad.starRating.let {
        if (it != null && it > 0.0) (binding.unified.starRatingView as RatingBar?)?.rating = it.toFloat() else (binding.unified.starRatingView as RatingBar?)?.rating = 0.0f
    }
    (binding.unified.headlineView as TextView?)?.text = ad.headline
    if (ad.body == null) {
        binding.unified.bodyView?.visibility = View.INVISIBLE
    } else {
        binding.unified.bodyView?.visibility = View.VISIBLE
        (binding.unified.bodyView as TextView?)?.text = ad.body
    }
    if (ad.callToAction == null) {
        binding.unified.callToActionView?.visibility = View.INVISIBLE
    } else {
        binding.unified.callToActionView?.visibility = View.VISIBLE
        (binding.unified.callToActionView as TextView?)?.text = ad.callToAction
    }
    if (ad.icon == null) {
        binding.unified.iconView?.visibility = View.GONE
    } else {
        (binding.unified.iconView as ImageView?)?.setImageDrawable(ad.icon?.drawable)
        binding.unified.iconView?.visibility = View.VISIBLE
    }
    try {
        binding.unified.registerNativeAd(ad, null)
    } catch (e2: Exception) {
        e2.printStackTrace()
    }
    container.removeAllViews()
    container.addView(binding.root)
}

fun Context.populateAdViewBanner(unifiedNativeAd: NativeAd?, container: AdsView) {
    val layoutInflater: LayoutInflater = LayoutInflater.from(this)
    val binding = AdUnifiedBannerBinding.inflate(layoutInflater)
    val ad = unifiedNativeAd ?: return
    binding.unified.iconView = binding.adAppIcon
    binding.unified.headlineView = binding.adHeadline
    binding.unified.bodyView = binding.adBody
    binding.unified.callToActionView = binding.adCallToAction

    (binding.unified.headlineView as TextView?)?.text = ad.headline
    if (ad.body == null) {
        binding.unified.bodyView?.visibility = View.INVISIBLE
    } else {
        binding.unified.bodyView?.visibility = View.VISIBLE
        (binding.unified.bodyView as TextView?)?.text = ad.body
    }
    if (ad.callToAction == null) {
        binding.unified.callToActionView?.visibility = View.INVISIBLE
    } else {
        binding.unified.callToActionView?.visibility = View.VISIBLE
        (binding.unified.callToActionView as TextView?)?.text = ad.callToAction
    }
    if (ad.icon == null) {
        binding.unified.iconView?.visibility = View.GONE
    } else {
        (binding.unified.iconView as ImageView?)?.setImageDrawable(ad.icon?.drawable)
        binding.unified.iconView?.visibility = View.VISIBLE
    }
    try {
        binding.unified.registerNativeAd(ad, null)
    } catch (e2: Exception) {
        e2.printStackTrace()
    }
    container.removeAllViews()
    container.addView(binding.root)
}

fun Context.loadNative(onNativeLoad: OnNativeLoad? = null) {
    try {
        val start = System.currentTimeMillis()
        val request = NativeAdRequest
            .Builder(getAdmobNativeId(), listOf(NativeAd.NativeAdType.NATIVE))
            .build()
        NativeAdLoader.load(request, object : NativeAdLoaderCallback {
            override fun onNativeAdLoaded(ad: NativeAd) {
                val seconds = (System.currentTimeMillis() - start) / 1000.0
                Log.e("AdTiming", "loadNative loaded in $seconds seconds")
                // Next-Gen load callbacks fire on a background thread; the
                // onLoad handler populates views, so hop to the UI thread.
                Handler(Looper.getMainLooper()).post {
                    onNativeLoad?.onLoad(ad)
                }
                Log.e(TAG, "Admob:NativeLoaded ")
            }

            override fun onAdFailedToLoad(adError: LoadAdError) {
                Log.e(TAG, "onAdFailedToLoad:Native: ${adError.code} ${adError.message}")
                Handler(Looper.getMainLooper()).post {
                    onNativeLoad?.onFail()
                }
            }
        })
    } catch (_: Exception) {
    }
}

interface OnNativeLoad {
    fun onLoad(nativeAd: NativeAd)
    fun onFail()
}
