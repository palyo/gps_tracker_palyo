package aanibrothers.tracker.io.module

import aanibrothers.tracker.io.databinding.*
import android.content.*
import android.view.*
import android.widget.*
import com.google.android.gms.ads.*
import com.google.android.gms.ads.nativead.*

private const val TAG = "Native"

fun Context.viewNativeBanner(container: ViewGroup) {
    container.removeAllViews()
    val view = AdUnifiedBannerLoadingBinding.inflate(LayoutInflater.from(applicationContext), null, false)
    view.root.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
    container.removeAllViews()
    container.addView(view.root)
    val adLoader = AdLoader.Builder(this, NATIVE_ID)
        .forNativeAd { nativeAd: NativeAd ->
            val layoutInflater: LayoutInflater = LayoutInflater.from(this@viewNativeBanner)
            val binding = AdUnifiedBannerBinding.inflate(layoutInflater)
            populateAdViewBanner(nativeAd, binding)
            container.removeAllViews()
            container.addView(binding.root)
        }
        .withAdListener(object : AdListener() {
            override fun onAdFailedToLoad(adError: LoadAdError) {

            }
        })
        .withNativeAdOptions(NativeAdOptions.Builder().setAdChoicesPlacement(NativeAdOptions.ADCHOICES_TOP_RIGHT).build()).build()
    adLoader.loadAd(AdRequest.Builder().build())
}

fun populateAdViewBanner(unifiedNativeAd: NativeAd?, binding: AdUnifiedBannerBinding) {
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
        (binding.unified.callToActionView as TextView?)?.text =
            unifiedNativeAd.callToAction
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
}

fun Context.viewNativeMedium(container: ViewGroup) {
    container.removeAllViews()
    val view = AdUnifiedMediumLoadingBinding.inflate(LayoutInflater.from(applicationContext), null, false)
    view.root.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
    container.removeAllViews()
    container.addView(view.root)
    val adLoader = AdLoader.Builder(this, NATIVE_ID)
        .forNativeAd { nativeAd: NativeAd ->
            val layoutInflater: LayoutInflater = LayoutInflater.from(this@viewNativeMedium)
            val binding = AdUnifiedMediumBinding.inflate(layoutInflater)
            populateAdViewMedium(nativeAd, binding)
            container.removeAllViews()
            container.addView(binding.root)
        }
        .withAdListener(object : AdListener() {
            override fun onAdFailedToLoad(adError: LoadAdError) {

            }
        })
        .withNativeAdOptions(NativeAdOptions.Builder().setMediaAspectRatio(MediaAspectRatio.LANDSCAPE).setAdChoicesPlacement(NativeAdOptions.ADCHOICES_TOP_RIGHT).build()).build()
    adLoader.loadAd(AdRequest.Builder().build())
}

fun populateAdViewMedium(unifiedNativeAd: NativeAd?, binding: AdUnifiedMediumBinding) {
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
        (binding.unified.callToActionView as TextView?)?.text =
            unifiedNativeAd.callToAction
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
}