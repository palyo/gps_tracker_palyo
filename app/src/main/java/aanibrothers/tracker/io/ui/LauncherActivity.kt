package aanibrothers.tracker.io.ui

import aanibrothers.tracker.io.App.Companion.appOpenManager
import aanibrothers.tracker.io.databinding.*
import aanibrothers.tracker.io.extension.*
import aanibrothers.tracker.io.module.*
import android.animation.*
import android.os.*
import android.view.*
import android.view.animation.*
import coder.apps.space.library.base.*
import coder.apps.space.library.extension.*
import com.google.android.material.imageview.*
import java.util.concurrent.atomic.*

class LauncherActivity : BaseActivity<ActivityLauncherBinding>(ActivityLauncherBinding::inflate) {

    private var consentManager: ConsentManager? = null
    private val isMobileAdsInitializeCalled = AtomicBoolean(false)
    private var zoomAnimator: ObjectAnimator? = null

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
            }, 3000)
        }
    }

    private fun gotoDashboard() {
        viewAppOpen(isWait = true) {
            if (tinyDB?.getBoolean(IS_LANGUAGE_ENABLED, true) == true) {
                go(AppLanguageActivity::class.java, finish = true)
            } else {
                go(DashboardActivity::class.java, finish = true)
            }
        }
    }

    private fun initializeMobileAdsSdk() {
        if (isMobileAdsInitializeCalled.getAndSet(true)) return
        appOpenManager = AppOpenManager()
        loadInterAd()
        Handler(mainLooper).postDelayed({
            gotoDashboard()
        }, 3000)
    }

    override fun ActivityLauncherBinding.initListeners() {}

    override fun ActivityLauncherBinding.initExtra() {}
}