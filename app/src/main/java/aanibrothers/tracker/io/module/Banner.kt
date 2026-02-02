package aanibrothers.tracker.io.module

import aanibrothers.tracker.io.R
import aanibrothers.tracker.io.databinding.AdUnifiedBannerLoadingBinding
import android.app.Activity
import android.content.Context
import android.util.DisplayMetrics
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.view.isNotEmpty
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError

fun Activity.viewBanner(container: ViewGroup) {
    if (container.isNotEmpty()) container.removeAllViews()
    val adSize = getAdSize()
    val height = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, (adSize.height + 2).toFloat(), resources.displayMetrics).toInt()
    val params: ViewGroup.LayoutParams = container.layoutParams
    container.setBackgroundResource(R.color.colorCardBackground)
    val view = AdUnifiedBannerLoadingBinding.inflate(LayoutInflater.from(applicationContext), null, false)
    view.root.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
    params.height = height
    container.layoutParams = params
    container.addView(view.root)
    val adView = AdView(this)
    adView.setAdSize(adSize)
    val adId = getAdmobBannerId()
    adView.adUnitId = adId

    adView.adListener = object : AdListener() {
        override fun onAdFailedToLoad(loadAdError: LoadAdError) {
            super.onAdFailedToLoad(loadAdError)
            viewBanner(container)
        }

        override fun onAdLoaded() {
            super.onAdLoaded()
            if (container.isNotEmpty()) container.removeAllViews()
            container.addView(adView)
        }
    }
    adView.loadAd(AdRequest.Builder().build())
}

private fun Activity.getAdSize(): AdSize {
    val display = this.windowManager.defaultDisplay
    val outMetrics = DisplayMetrics()
    display.getMetrics(outMetrics)
    val widthPixels = outMetrics.widthPixels.toFloat()
    val density = outMetrics.density
    val adWidth = (widthPixels / density).toInt()
    return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(this, adWidth)
}


fun Context.viewMRECBanner(viewGroup: AdsView) {
    val mAdView = AdView(this)
    mAdView.adUnitId = getAdmobBannerMRECId()
    mAdView.setAdSize(AdSize.MEDIUM_RECTANGLE)
    mAdView.adListener = object : AdListener() {
        override fun onAdLoaded() {
            super.onAdLoaded()
            viewGroup.removeAllViews()
            val layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.CENTER
            }
            viewGroup.addView(mAdView, layoutParams)
        }

        override fun onAdFailedToLoad(p0: LoadAdError) {
            super.onAdFailedToLoad(p0)
            Log.e(TAG, "onAdFailedToLoad:MRECBanner ${p0.message}")
        }
    }
    mAdView.loadAd(AdRequest.Builder().build())
}