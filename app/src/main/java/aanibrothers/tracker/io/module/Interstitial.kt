package aanibrothers.tracker.io.module

import android.app.*
import android.content.*
import android.os.*
import android.util.Log
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.*

const val TAG = "ADMOB_TAG"
private var admobInterstitialAd: InterstitialAd? = null
private var isLoadingAd = false

fun Context.loadInterAd(listener: ((result: Boolean) -> Unit)? = null) {
    if (isLoadingAd) return

    isLoadingAd = true
    val adRequest = AdRequest.Builder().build()
    val start = System.currentTimeMillis()
    InterstitialAd.load(
        this, getAdmobInterId(), adRequest,
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
    if (!isFinishing && !isDestroyed) {
        admobInterstitialAd?.show(this)
        admobInterstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdClicked() {
            }

            override fun onAdDismissedFullScreenContent() {
                listener?.invoke(false)
                admobInterstitialAd = null
                loadInterAd()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                listener?.invoke(true)
                admobInterstitialAd = null
                loadInterAd()
            }

            override fun onAdImpression() {

            }

            override fun onAdShowedFullScreenContent() {

            }
        }
    }
}


fun Activity.viewInterAdWithLogic(listener: ((result: Boolean) -> Unit)? = null) {
    currentAdLevel++
    if (currentAdLevel % 2 == 0) {
        viewInterAd {
            listener?.invoke(true)
        }
    } else {
        listener?.invoke(true)
    }
}

fun Activity.viewInterAd(listener: ((result: Boolean) -> Unit)? = null) {
    if (admobInterstitialAd != null) {
        displayInter(listener)
    } else if (!isLoadingAd) {
        listener?.invoke(true)
        loadInterAd()
    } else {
        listener?.invoke(true)
    }
}