package aanibrothers.tracker.io.ui

import aanibrothers.tracker.io.App.Companion.appOpenManager
import aanibrothers.tracker.io.databinding.*
import aanibrothers.tracker.io.extension.*
import aanibrothers.tracker.io.module.*
import android.*
import android.animation.*
import android.os.*
import android.util.*
import android.view.*
import android.view.animation.*
import coder.apps.space.library.base.*
import coder.apps.space.library.extension.*
import com.google.android.material.imageview.*
import com.limurse.iap.*
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
        isPremium = false
        val iapConnector = IapConnector(
            context = this@LauncherActivity,
            nonConsumableKeys = arrayListOf(),
            consumableKeys = arrayListOf(),
            subscriptionKeys = listOf("subscription_weekly", "subscription_monthly", "subscription_yearly"),
            key = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAksulvV0bG0KFijCqcxyyXddjpZmHv8C4D333QaHhEsEc29dnBSdFqAxvGM0M+3JMyh6aLWTraVnrsPf/p05aROcZXs2zjOwZFsrBdgtUkQKuFPVXof1IRhJdUov7vkSiB/uOuhTIzPf9W54Gwm1SpPZbuIg4XaW0BhQBUQQ2nlgV8MWR/jhfWdzpXd81ZMrm4y7jzDqq3cezDhoQ1O/s4xurtOy+derb9TD45t7hsoXUO8Bhkp61gV/e6f7xfTGJJiJHbNclrY7adFOIZRO0ODOTCb0oiJMobzEddulugXtbNvi7ijQKgNdSuMBLjm26NFb8ZzUjoFQlEO+GE1xRYQIDAQAB",
            enableLogging = true
        )
        iapConnector.addSubscriptionListener(object : SubscriptionServiceListener {
            override fun onPricesUpdated(iapKeyPrices: Map<String, DataWrappers.ProductDetails>) {}

            override fun onPurchaseFailed(purchaseInfo: DataWrappers.PurchaseInfo?, billingResponseCode: Int?) {}

            override fun onSubscriptionPurchased(purchaseInfo: DataWrappers.PurchaseInfo) {}

            override fun onSubscriptionRestored(purchaseInfo: DataWrappers.PurchaseInfo) {
                Log.e("TAG", "onSubscriptionRestored: ${purchaseInfo.isAutoRenewing}")
                isPremium = purchaseInfo.isAutoRenewing
            }
        })

        init { requestConsentForm() }
    }

    override fun onPause() {
        super.onPause()
        stopZoomAnimation()
    }

    private fun requestConsentForm() {
        if (!isPremium && isNetworkAvailable()) {
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
        if (isPremium) {
            if (tinyDB?.getBoolean(IS_LANGUAGE_ENABLED, true) == true) {
                go(AppLanguageActivity::class.java, finish = true)
            } else {
                go(DashboardActivity::class.java, finish = true)
            }
        } else if (!isPremium && !hasPermissions(arrayOf(Manifest.permission.READ_PHONE_STATE)) && !hasOverlayPermission()) {
            if (shouldCallFunction()) {
                saveLastFunctionCallTime()
                go(AppPermissionActivity::class.java, finish = true)
            } else {
                viewAppOpen(isWait = true) {
                    if (tinyDB?.getBoolean(IS_LANGUAGE_ENABLED, true) == true) {
                        go(AppLanguageActivity::class.java, finish = true)
                    } else {
                        go(DashboardActivity::class.java, finish = true)
                    }
                }
            }
        } else if (!hasPermissions(arrayOf(Manifest.permission.READ_PHONE_STATE)) || !hasOverlayPermission()) {
            go(AppPermissionActivity::class.java, finish = true)
        } else {
            if (appOpenCount >= 1 && !isPremium) {
                viewAppOpen(isWait = true) {
                    if (tinyDB?.getBoolean(IS_LANGUAGE_ENABLED, true) == true) {
                        go(AppLanguageActivity::class.java, finish = true)
                    } else {
                        go(DashboardActivity::class.java, finish = true)
                    }
                }
            } else {
                if (tinyDB?.getBoolean(IS_LANGUAGE_ENABLED, true) == true) {
                    go(AppLanguageActivity::class.java, finish = true)
                } else {
                    go(DashboardActivity::class.java, finish = true)
                }
            }
        }
    }

    private fun initializeMobileAdsSdk() {
        if (isMobileAdsInitializeCalled.getAndSet(true)) {
            return
        }

        if (!isPremium) {
            appOpenManager = AppOpenManager()
            if (!hasPermissions(arrayOf(Manifest.permission.READ_PHONE_STATE)) || !hasOverlayPermission()) {
                loadInterAd()
            }
        }
        Handler(mainLooper).postDelayed({
            gotoDashboard()
        }, 3000)
    }

    override fun ActivityLauncherBinding.initListeners() {}

    override fun ActivityLauncherBinding.initExtra() {}
}