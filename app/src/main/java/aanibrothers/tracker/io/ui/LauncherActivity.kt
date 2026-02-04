package aanibrothers.tracker.io.ui

import aanibrothers.tracker.io.R
import aanibrothers.tracker.io.databinding.ActivityLauncherBinding
import aanibrothers.tracker.io.extension.IS_INTRO_ENABLED
import aanibrothers.tracker.io.extension.IS_LANGUAGE_ENABLED
import aanibrothers.tracker.io.extension.SCREEN_PERMISSION
import aanibrothers.tracker.io.module.ConsentManager
import aanibrothers.tracker.io.module.TAG
import aanibrothers.tracker.io.module.init
import aanibrothers.tracker.io.module.loadInterAd
import aanibrothers.tracker.io.module.preloadNative
import aanibrothers.tracker.io.ui.updates.AppPermissionActivity
import aanibrothers.tracker.io.ui.updates.HomeActivity
import aanibrothers.tracker.io.ui.updates.OnboardingActivity
import android.os.Handler
import android.util.Log
import coder.apps.space.library.base.BaseActivity
import coder.apps.space.library.extension.go
import coder.apps.space.library.extension.hasPermissions
import coder.apps.space.library.extension.isNetworkAvailable
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import java.util.concurrent.atomic.AtomicBoolean

class LauncherActivity :
    BaseActivity<ActivityLauncherBinding>(ActivityLauncherBinding::inflate, isFullScreen = true) {

    private var consentManager: ConsentManager? = null
    private val isMobileAdsInitializeCalled = AtomicBoolean(false)
    var mSplashInterstitialAd: InterstitialAd? = null

    override fun ActivityLauncherBinding.initView() {
        init { requestConsentForm() }
        updateStatusBarColor(R.color.colorTransparent)
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
            mSplashInterstitialAd?.fullScreenContentCallback =
                object : FullScreenContentCallback() {
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

    private fun goNext() {
        loadInterAd()
        if (tinyDB?.getBoolean(IS_LANGUAGE_ENABLED, true) == true) {
            go(AppLanguageActivity::class.java, finish = true)
        } else if (tinyDB?.getBoolean(IS_INTRO_ENABLED, true) == true) {
            go(OnboardingActivity::class.java, finish = true)
        } else if (!hasPermissions(SCREEN_PERMISSION)) {
            go(AppPermissionActivity::class.java, finish = true)
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
        InterstitialAd.load(
            this@LauncherActivity,
            "ca-app-pub-4852962457779682/2927637589",
            adRequest,
            object : InterstitialAdLoadCallback() {
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