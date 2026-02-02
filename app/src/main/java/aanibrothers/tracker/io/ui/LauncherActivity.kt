package aanibrothers.tracker.io.ui

import aanibrothers.tracker.io.App.Companion.appOpenManager
import aanibrothers.tracker.io.databinding.*
import aanibrothers.tracker.io.extension.*
import aanibrothers.tracker.io.module.*
import aanibrothers.tracker.io.ui.updates.HomeActivity
import android.animation.*
import android.os.*
import android.util.Log
import android.view.*
import android.view.animation.*
import coder.apps.space.library.base.*
import coder.apps.space.library.extension.*
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.material.imageview.*
import java.util.concurrent.atomic.*

class LauncherActivity : BaseActivity<ActivityLauncherBinding>(ActivityLauncherBinding::inflate) {

    private var consentManager: ConsentManager? = null
    private val isMobileAdsInitializeCalled = AtomicBoolean(false)
    private var zoomAnimator: ObjectAnimator? = null
    var mSplashInterstitialAd: InterstitialAd? = null

    private fun startZoomAnimation(imageView: ShapeableImageView) {
        zoomAnimator = ObjectAnimator.ofPropertyValuesHolder(
            imageView,
            PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, 1.1f, 1f),
            PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, 1.1f, 1f)
        ).apply {
            duration = 1500L
            repeatCount = ObjectAnimator.INFINITE
            repeatMode = ObjectAnimator.REVERSE
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }
    }

    private fun stopZoomAnimation() {
        zoomAnimator?.cancel()
    }

    override fun ActivityLauncherBinding.initView() {
        startZoomAnimation(imageApp)
        init { requestConsentForm() }
    }

    override fun onPause() {
        super.onPause()
        stopZoomAnimation()
    }

    private fun requestConsentForm() {
        preloadNative()
        if (isNetworkAvailable()) {
            consentManager = ConsentManager.getInstance(this)
            consentManager?.gatherConsent(this) { consentError ->
                if (consentManager?.canRequestAds == true) {
                    try {
                        initializeMobileAdsSdk()
                    } catch (_: Exception) {
                    }
                }
            }
        } else {
            Handler(mainLooper).postDelayed({
                gotoDashboard()
            }, 4000)
        }
    }

    private fun gotoDashboard() {
        if (mSplashInterstitialAd != null) {
            mSplashInterstitialAd?.show(this)
            mSplashInterstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdClicked() {}

                override fun onAdDismissedFullScreenContent() {
                    mSplashInterstitialAd = null
                    goNext()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    mSplashInterstitialAd = null
                    goNext()
                }

                override fun onAdImpression() {
                }

                override fun onAdShowedFullScreenContent() {
                }
            }
        } else {
            goNext()
        }
    }

    private fun goNext(){
        loadInterAd()
        if (tinyDB?.getBoolean(IS_LANGUAGE_ENABLED, true) == true) {
            go(AppLanguageActivity::class.java, finish = true)
        } else {
            go(HomeActivity::class.java, finish = true)
        }
    }

    private fun initializeMobileAdsSdk() {
        if (isMobileAdsInitializeCalled.getAndSet(true)) return
        Handler(mainLooper).postDelayed({
            gotoDashboard()
        }, 4000)
    }

    override fun ActivityLauncherBinding.initListeners() {}

    override fun ActivityLauncherBinding.initExtra() {
        val adRequest = AdRequest.Builder().build()
        val start = System.currentTimeMillis()
        InterstitialAd.load(this@LauncherActivity, "ca-app-pub-4852962457779682/2927637589", adRequest, object : InterstitialAdLoadCallback() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                val seconds = (System.currentTimeMillis() - start) / 1000.0
                Log.e("AdTiming", "SplashInter onAdFailedToLoad in $seconds seconds")
                Log.e(TAG, "onAdFailedToLoad:SplashInter: ${adError.code} ${adError.message}")
                mSplashInterstitialAd = null
            }

            override fun onAdLoaded(interstitialAd: InterstitialAd) {
                val seconds = (System.currentTimeMillis() - start) / 1000.0
                mSplashInterstitialAd = interstitialAd
                Log.e("AdTiming", "SplashInter loaded in $seconds seconds")
                Log.e(TAG, "onAdLoaded:SplashInter ")
            }
        })
    }
}