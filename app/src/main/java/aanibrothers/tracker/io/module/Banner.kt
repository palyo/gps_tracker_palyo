package aanibrothers.tracker.io.module

import aanibrothers.tracker.io.R
import aanibrothers.tracker.io.databinding.*
import aanibrothers.tracker.io.extension.*
import android.Manifest
import android.app.*
import android.util.*
import android.view.*
import coder.apps.space.library.extension.hasPermissions
import com.google.android.gms.ads.*

fun Activity.viewBanner(container: ViewGroup) {
    if (container.childCount > 0) container.removeAllViews()
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
    if (!isPremium && !hasPermissions(arrayOf(Manifest.permission.READ_PHONE_STATE))) adView.adUnitId = BANNER_NON_CDO_ID
    else adView.adUnitId = BANNER_ID
    adView.adListener = object : AdListener() {
        override fun onAdFailedToLoad(loadAdError: LoadAdError) {
            super.onAdFailedToLoad(loadAdError)
            viewBanner(container)
        }

        override fun onAdLoaded() {
            super.onAdLoaded()
            if (container.childCount > 0) container.removeAllViews()
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