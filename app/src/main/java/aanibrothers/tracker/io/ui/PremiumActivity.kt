package aanibrothers.tracker.io.ui

import aanibrothers.tracker.io.R
import aanibrothers.tracker.io.databinding.*
import aanibrothers.tracker.io.module.*
import android.os.*
import android.util.*
import android.widget.*
import androidx.activity.*
import androidx.lifecycle.*
import coder.apps.space.library.base.*
import coder.apps.space.library.extension.*
import com.limurse.iap.*

class PremiumActivity : BaseActivity<ActivityPremiumBinding>(ActivityPremiumBinding::inflate) {
    private var iapConnector: IapConnector? = null
    private var isPremiumPlan = 3
    private val subsList = listOf("subscription_weekly", "subscription_monthly", "subscription_yearly")
    val isBillingClientConnected: MutableLiveData<Boolean> = MutableLiveData()
    private var restoreKey: String? = null

    override fun ActivityPremiumBinding.initExtra() {
        updateNavigationBarColor(coder.apps.space.library.R.color.colorTransparent)
        isBillingClientConnected.value = false
        cardWeekly.isSelected = false
        cardMonthly.isSelected = false
        cardYearly.isSelected = true

        iapConnector = IapConnector(
            context = this@PremiumActivity,
            nonConsumableKeys = kotlin.collections.arrayListOf(),
            consumableKeys = kotlin.collections.arrayListOf(),
            subscriptionKeys = subsList,
            key = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAksulvV0bG0KFijCqcxyyXddjpZmHv8C4D333QaHhEsEc29dnBSdFqAxvGM0M+3JMyh6aLWTraVnrsPf/p05aROcZXs2zjOwZFsrBdgtUkQKuFPVXof1IRhJdUov7vkSiB/uOuhTIzPf9W54Gwm1SpPZbuIg4XaW0BhQBUQQ2nlgV8MWR/jhfWdzpXd81ZMrm4y7jzDqq3cezDhoQ1O/s4xurtOy+derb9TD45t7hsoXUO8Bhkp61gV/e6f7xfTGJJiJHbNclrY7adFOIZRO0ODOTCb0oiJMobzEddulugXtbNvi7ijQKgNdSuMBLjm26NFb8ZzUjoFQlEO+GE1xRYQIDAQAB",
            enableLogging = true
        )

        iapConnector?.addBillingClientConnectionListener(object : BillingClientConnectionListener {
            override fun onConnected(status: Boolean, billingResponseCode: Int) {
                isBillingClientConnected.value = status
            }
        })

        isBillingClientConnected.observe(this@PremiumActivity) { connected ->
            when (connected) {
                true -> {
                    buttonBuy.isEnabled = true
                    buttonBuy.setOnClickListener {
                        val inAppId = when (isPremiumPlan) {
                            1 -> "subscription_weekly"
                            2 -> "subscription_monthly"
                            else -> "subscription_yearly"
                        }
                        iapConnector?.subscribe(this@PremiumActivity, inAppId)
                    }
                }

                else -> {
                    buttonBuy.isEnabled = false
                }
            }
        }

        iapConnector?.addSubscriptionListener(object : SubscriptionServiceListener {
            override fun onPricesUpdated(iapKeyPrices: Map<String, DataWrappers.ProductDetails>) {
                iapKeyPrices.forEach { (key, u) ->
                    u.offers?.forEach { offers ->
                        offers.pricingPhases.forEach {
                            when (key) {
                                "subscription_weekly" -> textWeeklyPrice.text = it.price
                                "subscription_monthly" -> textMonthlyPrice.text = it.price
                                else -> textYearlyPrice.text = it.price
                            }
                        }
                    }
                }
            }

            override fun onPurchaseFailed(purchaseInfo: DataWrappers.PurchaseInfo?, billingResponseCode: Int?) {
                Log.e("TAG", "onPurchaseFailed: $billingResponseCode")
            }

            override fun onSubscriptionPurchased(purchaseInfo: DataWrappers.PurchaseInfo) {
                Log.e("TAG", "onSubscriptionPurchased: $purchaseInfo")
                purchased()
            }

            override fun onSubscriptionRestored(purchaseInfo: DataWrappers.PurchaseInfo) {
                Log.e("TAG", "onSubscriptionRestored: $purchaseInfo")
                restored(purchaseInfo)
            }
        })

        buttonClose.apply {
            beInvisible()
            Handler(Looper.getMainLooper()).postDelayed({
                beVisible()
            }, 3000)
        }
    }

    override fun ActivityPremiumBinding.initListeners() {
        cardWeekly.setOnClickListener {
            isPremiumPlan = 1
            cardWeekly.isSelected = true
            cardMonthly.isSelected = false
            cardYearly.isSelected = false
        }

        cardMonthly.setOnClickListener {
            isPremiumPlan = 2
            cardWeekly.isSelected = false
            cardMonthly.isSelected = true
            cardYearly.isSelected = false
        }

        cardYearly.setOnClickListener {
            isPremiumPlan = 3
            cardWeekly.isSelected = false
            cardMonthly.isSelected = false
            cardYearly.isSelected = true
        }

        buttonRestore.setOnClickListener {
            if (restoreKey.isNullOrEmpty()) {
                Toast.makeText(this@PremiumActivity, "You don't have any Subscription", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            updatePremium()
        }

        buttonClose.setOnClickListener {
            goNext()
        }
    }

    override fun ActivityPremiumBinding.initView() {
        updateStatusBarColor(R.color.colorPrimary)
        onBackPressedDispatcher.addCallback(this@PremiumActivity, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finish()
            }
        })
    }

    fun purchased() {
        updatePremium()
    }

    fun restored(purchaseInfo: DataWrappers.PurchaseInfo) {
        restoreKey = purchaseInfo.sku
    }

    private fun updatePremium() {
        isPremium = true
        Toast.makeText(this@PremiumActivity, "You are successfully Subscribed", Toast.LENGTH_SHORT).show()
        onGo()
    }

    private fun goNext() {
        viewInterAd {
            go(DashboardActivity::class.java, finish = true)
        }
    }

    fun onGo() {
        go(DashboardActivity::class.java, finish = true)
    }
}