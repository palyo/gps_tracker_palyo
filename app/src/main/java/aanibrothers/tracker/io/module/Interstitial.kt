package aanibrothers.tracker.io.module

import aanibrothers.tracker.io.analytics.AdPlacement
import aanibrothers.tracker.io.analytics.Analytics
import aanibrothers.tracker.io.analytics.AnalyticsEvent
import android.app.*
import android.content.*
import android.util.Log
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.AdRequest
import com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAd
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAdEventCallback

const val TAG = "ADMOB_TAG"
private var admobInterstitialAd: InterstitialAd? = null
private var isLoadingAd = false

// Frequency cap: AdMob's quality threshold expects a reasonable gap between
// full-screen ads. Showing an interstitial on every back-press (the old
// behaviour) is one of the most-flagged abusive patterns.
private const val MIN_INTERSTITIAL_INTERVAL_MS = 45_000L  // 45 seconds
private var lastInterstitialShownAt = 0L

private fun isWithinFrequencyCap(): Boolean {
    if (lastInterstitialShownAt == 0L) return false
    return System.currentTimeMillis() - lastInterstitialShownAt < MIN_INTERSTITIAL_INTERVAL_MS
}

fun Context.loadInterAd(listener: ((result: Boolean) -> Unit)? = null) {
    if (isLoadingAd || admobInterstitialAd != null) return
    val adUnitId = getAdmobInterId()
    if (adUnitId.isBlank()) {
        listener?.invoke(true)
        return
    }
    isLoadingAd = true
    val start = System.currentTimeMillis()
    InterstitialAd.load(
        AdRequest.Builder(adUnitId).build(),
        object : AdLoadCallback<InterstitialAd> {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                val seconds = (System.currentTimeMillis() - start) / 1000.0
                Log.e("AdTiming", "Inter onAdFailedToLoad in $seconds seconds")
                Log.e(TAG, "onAdFailedToLoad:Inter: ${adError.code} ${adError.message}")
                isLoadingAd = false
                admobInterstitialAd = null
                Analytics.log(
                    AnalyticsEvent.AdFailedToLoad(
                        placement = AdPlacement.UNKNOWN,
                        format = "interstitial",
                        errorCode = adError.code.ordinal
                    )
                )
                listener?.invoke(true)
            }

            override fun onAdLoaded(ad: InterstitialAd) {
                val seconds = (System.currentTimeMillis() - start) / 1000.0
                isLoadingAd = false
                admobInterstitialAd = ad
                Log.e("AdTiming", "Inter loaded in $seconds seconds")
                Log.e(TAG, "onAdLoaded:Inter ")
                listener?.invoke(false)
            }
        })
}

private fun Activity.displayInter(listener: ((result: Boolean) -> Unit)? = null) {
    if (isFinishing || isDestroyed) {
        listener?.invoke(true)
        return
    }
    val ad = admobInterstitialAd ?: run {
        listener?.invoke(true)
        return
    }
    ad.adEventCallback = object : InterstitialAdEventCallback {
        override fun onAdClicked() {}

        override fun onAdDismissedFullScreenContent() {
            admobInterstitialAd = null
            lastInterstitialShownAt = System.currentTimeMillis()
            listener?.invoke(false)
            loadInterAd()
            // Do NOT auto-reload here — let the next call site request on
            // demand. Auto-reload after every dismiss caused excess ad
            // requests with low impression-to-request ratios.
        }

        override fun onAdFailedToShowFullScreenContent(fullScreenContentError: FullScreenContentError) {
            admobInterstitialAd = null
            listener?.invoke(true)
            loadInterAd()
        }

        override fun onAdImpression() {}

        override fun onAdShowedFullScreenContent() {
            lastInterstitialShownAt = System.currentTimeMillis()
        }
    }
    ad.show(this)
}

fun Activity.viewInterAd(listener: ((result: Boolean) -> Unit)? = null) {
    viewInterAd(placement = AdPlacement.UNKNOWN, listener = listener)
}

/**
 * Placement-aware overload. Prefer this at call sites so AdMob revenue can be
 * broken down by screen in the Firebase / BigQuery dashboards.
 *
 * Emits:
 *   - ad_capped              when frequency cap suppressed a show
 *   - ad_impression_custom   when an interstitial actually displayed
 */
fun Activity.viewInterAd(
    placement: String,
    listener: ((result: Boolean) -> Unit)? = null
) {
    if (isWithinFrequencyCap()) {
        Analytics.log(AnalyticsEvent.AdCapped(placement = placement))
        listener?.invoke(true)
        return
    }
    if (admobInterstitialAd != null) {
        Analytics.log(AnalyticsEvent.AdImpression(placement = placement, format = "interstitial"))
        displayInter(listener)
    } else if (!isLoadingAd) {
        listener?.invoke(true)
        loadInterAd()
    } else {
        listener?.invoke(true)
    }
}
