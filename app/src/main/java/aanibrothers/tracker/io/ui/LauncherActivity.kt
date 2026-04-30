package aanibrothers.tracker.io.ui

import aanibrothers.tracker.io.App
import aanibrothers.tracker.io.R
import aanibrothers.tracker.io.databinding.ActivityLauncherBinding
import aanibrothers.tracker.io.extension.IS_INTRO_ENABLED
import aanibrothers.tracker.io.extension.IS_LANGUAGE_ENABLED
import aanibrothers.tracker.io.extension.IS_SPLASH_AD_FAILED
import aanibrothers.tracker.io.extension.hasRequiredAppPermissions
import aanibrothers.tracker.io.module.ConsentManager
import aanibrothers.tracker.io.module.TAG
import aanibrothers.tracker.io.module.loadInterAd
import aanibrothers.tracker.io.module.preloadNative
import aanibrothers.tracker.io.ui.updates.AppPermissionActivity
import aanibrothers.tracker.io.ui.updates.HomeActivity
import aanibrothers.tracker.io.ui.updates.OnboardingActivity
import android.os.Handler
import android.os.Looper
import android.util.Log
import coder.apps.space.library.base.BaseActivity
import coder.apps.space.library.extension.go
import coder.apps.space.library.extension.isNetworkAvailable
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import java.util.concurrent.atomic.AtomicBoolean

class LauncherActivity :
    BaseActivity<ActivityLauncherBinding>(ActivityLauncherBinding::inflate, isFullScreen = true) {

    private var consentManager: ConsentManager? = null
    private val isMobileAdsInitializeCalled = AtomicBoolean(false)
    private val hasNavigated = AtomicBoolean(false)
    private var mSplashInterstitialAd: InterstitialAd? = null

    // Hardcoded splash interstitial unit (preserved from your original code).
    private val SPLASH_INTER_UNIT = "ca-app-pub-4852962457779682/2927637589"

    // Hard ceiling on how long we'll keep the splash visible while waiting
    // for the ad to load. After this, we proceed without the ad.
    private val SPLASH_INTER_TIMEOUT_MS = 6000L

    override fun ActivityLauncherBinding.initView() {
        // Order: gather consent -> init MobileAds -> load splash interstitial
        // -> show it -> goNext. No ad request runs before consent is granted.
        requestConsentForm()
        updateStatusBarColor(R.color.colorTransparent)
    }

    private fun requestConsentForm() {
        if (!isNetworkAvailable()) {
            // Offline -> skip ads entirely.
            Handler(Looper.getMainLooper()).postDelayed({ goNext(8) }, 1500)
            return
        }
        consentManager = ConsentManager.getInstance(this)
        consentManager?.gatherConsent(this) { _ ->
            if (consentManager?.canRequestAds == true) {
                initializeMobileAdsSdk()
            } else {
                // Consent declined / unavailable -> proceed without ads.
                goNext(1)
            }
        }
    }

    private fun initializeMobileAdsSdk() {
        if (isMobileAdsInitializeCalled.getAndSet(true)) return
        try {
            MobileAds.initialize(this) {
                preloadNative()
                loadSplashInterstitial()
            }
        } catch (_: Exception) {
            goNext(2)
        }
    }

    private fun loadSplashInterstitial() {
        // CONFLICT FIX: while a splash interstitial is in flight (loading or
        // showing), the App Open ad must NOT also fire. App.isOpenInter is
        // the shared flag the AppOpen lifecycle observer checks. We set it
        // true here and reset it on every terminal callback below.
        App.isOpenInter = true

        // Hard timeout — if the ad doesn't load in SPLASH_INTER_TIMEOUT_MS
        // we give up and continue. Without this the user can sit on the
        // splash screen indefinitely, which is itself a quality red flag.
        Handler(Looper.getMainLooper()).postDelayed({
            if (!hasNavigated.get() && mSplashInterstitialAd == null) {
                tinyDB?.putBoolean(IS_SPLASH_AD_FAILED, true)
                App.isOpenInter = false
                goNext(3)
            }
        }, SPLASH_INTER_TIMEOUT_MS)

        InterstitialAd.load(
            this,
            SPLASH_INTER_UNIT,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    if (hasNavigated.get() || isFinishing || isDestroyed) {
                        // Already moved on (timeout fired) -> drop the ad,
                        // don't show after we've left the splash.
                        App.isOpenInter = false
                        return
                    }
                    mSplashInterstitialAd = interstitialAd
                    Log.e(TAG, "onAdLoaded:SplashInter")
                    showSplashInterstitial()
                }

                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Log.e(TAG, "onAdFailedToLoad:SplashInter ${adError.code} ${adError.message}")
                    mSplashInterstitialAd = null
                    tinyDB?.putBoolean(IS_SPLASH_AD_FAILED, true)
                    App.isOpenInter = false
                    goNext(4)
                }
            }
        )
    }

    private fun showSplashInterstitial() {
        val ad = mSplashInterstitialAd
        if (ad == null || isFinishing || isDestroyed) {
            App.isOpenInter = false
            goNext(5)
            return
        }
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdClicked() {}

            override fun onAdDismissedFullScreenContent() {
                Log.e(TAG, "onAdDismissedFullScreenContent: ")
                mSplashInterstitialAd = null
                tinyDB?.putBoolean(IS_SPLASH_AD_FAILED, false)
                // Splash interstitial done — release the AppOpen suppression
                // flag so future foregrounds can show AppOpen normally.
                App.isOpenInter = false
                goNext(6)
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                mSplashInterstitialAd = null
                tinyDB?.putBoolean(IS_SPLASH_AD_FAILED, true)
                App.isOpenInter = false
                goNext(7)
            }

            override fun onAdImpression() {}

            override fun onAdShowedFullScreenContent() {
                tinyDB?.putBoolean(IS_SPLASH_AD_FAILED, false)
                // Keep isOpenInter=true while showing — released on dismiss.
            }
        }
        ad.show(this)
    }

    private fun goNext(int: Int) {
        Log.e(TAG, "goNext: $int" )
        loadInterAd()
        if (hasNavigated.getAndSet(true)) return
        when {
            tinyDB?.getBoolean(IS_LANGUAGE_ENABLED, true) == true ->
                go(AppLanguageActivity::class.java, finish = true)
            tinyDB?.getBoolean(IS_INTRO_ENABLED, true) == true ->
                go(OnboardingActivity::class.java, finish = true)
            !hasRequiredAppPermissions() ->
                go(AppPermissionActivity::class.java, finish = true)
            else ->
                go(HomeActivity::class.java, finish = true)
        }
    }

    override fun ActivityLauncherBinding.initListeners() {}

    override fun ActivityLauncherBinding.initExtra() {
        // Splash interstitial is now loaded AFTER consent inside
        // loadSplashInterstitial(). Loading here would fire an ad request
        // before consent, violating GDPR / Google's policy.
    }

    override fun onDestroy() {
        // Safety: don't leave AppOpen suppressed forever if killed mid-flight.
        if (App.isOpenInter && mSplashInterstitialAd == null) {
            App.isOpenInter = false
        }
        super.onDestroy()
    }
}
