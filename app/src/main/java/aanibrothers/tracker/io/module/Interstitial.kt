package aanibrothers.tracker.io.module

import android.app.*
import android.content.*
import android.os.*
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.*

private const val TAG = "AdmobInterstitial"
private var admobInterstitialAd: InterstitialAd? = null
private var isLoadingAd = false

fun Context.loadInterAd(listener: ((result: Boolean) -> Unit)? = null) {
    if (isLoadingAd) return

    isLoadingAd = true
    val adRequest = AdRequest.Builder().build()
    InterstitialAd.load(
        this, INTER_ID, adRequest,
        object : InterstitialAdLoadCallback() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                isLoadingAd = false
                admobInterstitialAd = null
                listener?.invoke(true)
            }

            override fun onAdLoaded(interstitialAd: InterstitialAd) {
                isLoadingAd = false
                admobInterstitialAd = interstitialAd
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

fun Activity.viewInterAdForce(listener: ((result: Boolean) -> Unit)? = null) {
    if (admobInterstitialAd != null) {
        displayInter(listener)
    } else {
        loadInterAd()
        object : CountDownTimer((5 * 1000).toLong(), 1000) {
            override fun onTick(millisUntilFinished: Long) {
                if (admobInterstitialAd != null) {
                    this.cancel()
                    this.onFinish()
                }
            }

            override fun onFinish() {
                if (admobInterstitialAd != null) {
                    displayInter(listener)
                } else {
                    listener?.invoke(true)
                    loadInterAd()
                }
            }
        }.start()
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