package aanibrothers.tracker.io.module

import android.app.*
import android.content.*
import android.util.Log
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.*

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
    val adRequest = AdRequest.Builder().build()
    val start = System.currentTimeMillis()
    InterstitialAd.load(
        this, adUnitId, adRequest,
        object : InterstitialAdLoadCallback() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                val seconds = (System.currentTimeMillis() - start) / 1000.0
                Log.e("AdTiming", "Inter onAdFailedToLoad in $seconds seconds")
                Log.e(TAG, "onAdFailedToLoad:Inter: ${adError.code} ${adError.message}")
                isLoadingAd = false
                admobInterstitialAd = null
                listener?.invoke(true)
            }

            override fun onAdLoaded(interstitialAd: InterstitialAd) {
                val seconds = (System.currentTimeMillis() - start) / 1000.0
                isLoadingAd = false
                admobInterstitialAd = interstitialAd
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
    ad.fullScreenContentCallback = object : FullScreenContentCallback() {
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

        override fun onAdFailedToShowFullScreenContent(adError: AdError) {
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
    if (isWithinFrequencyCap()) {
        listener?.invoke(true)
        return
    }
    if (admobInterstitialAd != null) {
        displayInter(listener)
    } else if (!isLoadingAd) {
        listener?.invoke(true)
        loadInterAd()
    } else {
        listener?.invoke(true)
    }
}