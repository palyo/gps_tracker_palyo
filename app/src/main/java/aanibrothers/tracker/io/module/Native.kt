package aanibrothers.tracker.io.module

import aanibrothers.tracker.io.databinding.AdUnifiedBannerBinding
import aanibrothers.tracker.io.databinding.AdUnifiedMediumBinding
import android.app.Activity
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.VideoOptions
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions


var nativeAd: NativeAd? = null

fun Activity.preloadNative() {
    try {
        val start = System.currentTimeMillis()
        val adLoader: AdLoader = AdLoader.Builder(this, getAdmobNativeId()).forNativeAd {
            val seconds = (System.currentTimeMillis() - start) / 1000.0
            Log.e("AdTiming", "preloadNative loaded in $seconds seconds")
            nativeAd = it
            Log.e(TAG, "preloadNative:onLoaded ")
        }.withAdListener(object : AdListener() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                Log.e(TAG, "onAdFailedToLoad:preloadNative: ${adError.code} ${adError.message}")
            }
        }).withNativeAdOptions(
            NativeAdOptions.Builder().setVideoOptions(
                VideoOptions.Builder().setStartMuted(true).build()
            ).build()
        ).build()
        adLoader.loadAd(AdRequest.Builder().build())
    } catch (_: Exception) {
    }
}

fun Activity.viewNativeBanner(container: AdsView) {
    if (nativeAd != null) {
        populateAdViewBanner(nativeAd, container = container)
        preloadNative()
    } else {
        loadNative(object : OnNativeLoad {
            override fun onLoad(nativeAd: NativeAd) {
                populateAdViewBanner(nativeAd, container = container)
                preloadNative()
            }

            override fun onFail() {
            }
        })
    }
}

fun Activity.viewNativeMedium(container: AdsView) {
    if (nativeAd != null) {
        populateAdViewMedium(nativeAd, container = container)
        preloadNative()
    } else {
        loadNative(object : OnNativeLoad {
            override fun onLoad(nativeAd: NativeAd) {
                populateAdViewMedium(nativeAd, container = container)
                preloadNative()
            }

            override fun onFail() {
            }
        })
    }
}

fun Context.populateAdViewMedium(unifiedNativeAd: NativeAd?, container: AdsView) {
    val layoutInflater: LayoutInflater = LayoutInflater.from(this)
    val binding = AdUnifiedMediumBinding.inflate(layoutInflater)
    binding.unified.iconView = binding.adAppIcon
    binding.unified.mediaView = binding.adMedia
    binding.unified.headlineView = binding.adHeadline
    binding.unified.bodyView = binding.adBody
    binding.unified.callToActionView = binding.adCallToAction

    binding.unified.starRatingView = binding.adStars
    unifiedNativeAd?.starRating.let {
        if (it != null && it > 0.0) (binding.unified.starRatingView as RatingBar?)?.rating = it.toFloat() else (binding.unified.starRatingView as RatingBar?)?.rating = 0.0f
    }
    (binding.unified.headlineView as TextView?)?.text = unifiedNativeAd?.headline
    if (unifiedNativeAd?.body == null) {
        binding.unified.bodyView?.visibility = View.INVISIBLE
    } else {
        binding.unified.bodyView?.visibility = View.VISIBLE
        (binding.unified.bodyView as TextView?)?.text = unifiedNativeAd.body
    }
    if (unifiedNativeAd?.callToAction == null) {
        binding.unified.callToActionView?.visibility = View.INVISIBLE
    } else {
        binding.unified.callToActionView?.visibility = View.VISIBLE
        (binding.unified.callToActionView as TextView?)?.text = unifiedNativeAd.callToAction
    }
    if (unifiedNativeAd?.icon == null) {
        binding.unified.iconView?.visibility = View.GONE
    } else {
        (binding.unified.iconView as ImageView?)?.setImageDrawable(unifiedNativeAd.icon?.drawable)
        binding.unified.iconView?.visibility = View.VISIBLE
    }
    try {
        if (unifiedNativeAd != null) {
            binding.unified.setNativeAd(unifiedNativeAd)
        }
    } catch (e2: Exception) {
        e2.printStackTrace()
    }
    container.removeAllViews()
    container.addView(binding.root)
}

fun Context.populateAdViewBanner(unifiedNativeAd: NativeAd?, container: AdsView) {
    val layoutInflater: LayoutInflater = LayoutInflater.from(this)
    val binding = AdUnifiedBannerBinding.inflate(layoutInflater)
    binding.unified.iconView = binding.adAppIcon
    binding.unified.headlineView = binding.adHeadline
    binding.unified.bodyView = binding.adBody
    binding.unified.callToActionView = binding.adCallToAction

    (binding.unified.headlineView as TextView?)?.text = unifiedNativeAd?.headline
    if (unifiedNativeAd?.body == null) {
        binding.unified.bodyView?.visibility = View.INVISIBLE
    } else {
        binding.unified.bodyView?.visibility = View.VISIBLE
        (binding.unified.bodyView as TextView?)?.text = unifiedNativeAd.body
    }
    if (unifiedNativeAd?.callToAction == null) {
        binding.unified.callToActionView?.visibility = View.INVISIBLE
    } else {
        binding.unified.callToActionView?.visibility = View.VISIBLE
        (binding.unified.callToActionView as TextView?)?.text = unifiedNativeAd.callToAction
    }
    if (unifiedNativeAd?.icon == null) {
        binding.unified.iconView?.visibility = View.GONE
    } else {
        (binding.unified.iconView as ImageView?)?.setImageDrawable(unifiedNativeAd.icon?.drawable)
        binding.unified.iconView?.visibility = View.VISIBLE
    }
    try {
        if (unifiedNativeAd != null) {
            binding.unified.setNativeAd(unifiedNativeAd)
        }
    } catch (e2: Exception) {
        e2.printStackTrace()
    }
    container.removeAllViews()
    container.addView(binding.root)
}

fun Context.loadNative(onNativeLoad: OnNativeLoad? = null) {
    try {
        val start = System.currentTimeMillis()
        val adLoader: AdLoader = AdLoader.Builder(this, getAdmobNativeId()).forNativeAd {
            val seconds = (System.currentTimeMillis() - start) / 1000.0
            Log.e("AdTiming", "loadNative loaded in $seconds seconds")
            onNativeLoad?.onLoad(it)
            Log.e(TAG, "Admob:NativeLoaded ")
        }.withAdListener(object : AdListener() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                Log.e(TAG, "onAdFailedToLoad:Native: ${adError.code} ${adError.message}")
                onNativeLoad?.onFail()
            }
        }).withNativeAdOptions(
            NativeAdOptions.Builder().setVideoOptions(
                VideoOptions.Builder().setStartMuted(true).build()
            ).build()
        ).build()
        adLoader.loadAd(AdRequest.Builder().build())
    } catch (_: Exception) {
    }
}

interface OnNativeLoad {
    fun onLoad(nativeAd: NativeAd)
    fun onFail()
}