package aanibrothers.tracker.io.module

import aanibrothers.tracker.io.R
import aanibrothers.tracker.io.databinding.AdUnifiedBannerLoadingBinding
import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.view.isNotEmpty
import com.google.android.libraries.ads.mobile.sdk.banner.AdView
import com.google.android.libraries.ads.mobile.sdk.banner.AdSize
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAd
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdRequest
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError

// Bounded retry. The previous implementation called viewBanner(container)
// recursively on every no-fill — an infinite ad-request storm that triggers
// the AdMob "below quality threshold" filter.
private const val MAX_BANNER_RETRIES = 1
private const val BANNER_RETRY_DELAY_MS = 30_000L

fun Activity.viewBanner(container: ViewGroup) {
    viewBannerInternal(container, attempt = 0)
}

private fun Activity.viewBannerInternal(container: ViewGroup, attempt: Int) {
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

    val adId = getAdmobBannerId()
    val adView = AdView(this)
    val adRequest = BannerAdRequest.Builder(adId, adSize).build()
    adView.loadAd(adRequest, object : AdLoadCallback<BannerAd> {
        override fun onAdLoaded(ad: BannerAd) {
            // Next-Gen load callbacks fire on a background thread; touch the
            // view hierarchy only on the UI thread.
            container.post {
                if (container.isNotEmpty()) container.removeAllViews()
                container.addView(adView)
            }
        }

        override fun onAdFailedToLoad(adError: LoadAdError) {
            Log.e(TAG, "Banner failed (attempt $attempt): ${adError.code} ${adError.message}")
            if (attempt >= MAX_BANNER_RETRIES) return
            if (isFinishing || isDestroyed) return
            Handler(Looper.getMainLooper()).postDelayed({
                if (!isFinishing && !isDestroyed) {
                    viewBannerInternal(container, attempt + 1)
                }
            }, BANNER_RETRY_DELAY_MS)
        }
    })
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
    val adView = AdView(this)
    val adRequest = BannerAdRequest.Builder(getAdmobBannerMRECId(), AdSize.MEDIUM_RECTANGLE).build()
    adView.loadAd(adRequest, object : AdLoadCallback<BannerAd> {
        override fun onAdLoaded(ad: BannerAd) {
            viewGroup.post {
                viewGroup.removeAllViews()
                val layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    gravity = Gravity.CENTER
                }
                viewGroup.addView(adView, layoutParams)
            }
        }

        override fun onAdFailedToLoad(adError: LoadAdError) {
            Log.e(TAG, "onAdFailedToLoad:MRECBanner ${adError.message}")
        }
    })
}
