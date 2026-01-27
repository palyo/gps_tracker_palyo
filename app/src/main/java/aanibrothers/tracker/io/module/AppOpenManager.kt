package aanibrothers.tracker.io.module

import aanibrothers.tracker.io.*
import android.app.*
import android.os.*
import com.google.android.gms.ads.*
import com.google.android.gms.ads.appopen.*
import java.util.*

class AppOpenManager {
    companion object {
        var openIds = listOf<String>()
        private val TAG = "AppOpenManager"
        var isShowingAd = false
    }

    private var appOpenAd: AppOpenAd? = null
    private var loadCallback: AppOpenAd.AppOpenAdLoadCallback? = null
    private var loadTime: Long = 0
    private var listeners: ((result: Boolean) -> Unit)? = null
    private var isLoading = false
    private var isLoaded = false
    private var isFailed = false
    private var progressDialog: Dialog? = null
    private var dialogShow: Boolean = false

    init {
        initAd()
    }

    fun initAd() {
        App.getAppContext().apply {
            val adId = getAdmobOpenId()
            openIds = mutableListOf(adId)
            loadOpen()
        }
    }

    fun loadOpen(index: Int? = 0) {
        if ((openIds.size - 1) < (index ?: 0)) {
            return
        }
        fetchAd(openIds[index ?: 0]) {
            loadOpen(index = (index ?: 0) + 1)
        }
    }

    fun showAdIfAvailable(isWait: Boolean, isDialogShown: Boolean = false, listener: ((result: Boolean) -> Unit)?) {
        this.listeners = listener
        this.dialogShow = isDialogShown
        if (!isShowingAd && isAdAvailable()) {
            if (dialogShow) {
                val currentActivity = App.currentActivity
                if (currentActivity != null && !currentActivity.isDestroyed && !currentActivity.isFinishing) {
                    progressDialog?.let {
                        if (it.isShowing) {
                            try {
                                it.dismiss()
                            } catch (e: IllegalArgumentException) {
                            }
                        }
                    }
                }
            }
            val fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    listeners?.invoke(false)
                    appOpenAd = null
                    isShowingAd = false
                    listeners = null
                    initAd()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    listeners?.invoke(true)
                    listeners = null
                }

                override fun onAdShowedFullScreenContent() {
                    isShowingAd = true
                }
            }
            appOpenAd?.fullScreenContentCallback = fullScreenContentCallback
            isLoaded = false
            App.currentActivity?.let {
                appOpenAd?.show(it)
            }
        } else {
            if (!isWait) {
                if (dialogShow) {
                    if (App.currentActivity != null && App.currentActivity?.isDestroyed?.not() == true && App.currentActivity?.isFinishing?.not() == true && progressDialog != null && progressDialog?.isShowing == true) {
                        try {
                            progressDialog?.dismiss()
                        } catch (e: Exception) {
                        }
                    }
                }
                listeners?.invoke(true)
                listeners = null
                initAd()
                return
            }
            if (isLoading) {
                if (dialogShow) {
                    progressDialog = App.currentActivity?.let { showProgressDialog(it) }
                }
                object : CountDownTimer(4500, 10) {
                    override fun onTick(millisUntilFinished: Long) {
                    }

                    override fun onFinish() {
                        if (dialogShow) {
                            if (App.currentActivity != null && App.currentActivity?.isFinishing?.not() == true && progressDialog != null && progressDialog?.isShowing == true) {
                                progressDialog?.dismiss()
                            }
                        }
                        if (!isShowingAd && !isLoaded) {
                            listeners?.invoke(true)
                            listeners = null
                        }
                    }
                }.start()
                return
            } else if (isFailed) {
                if (dialogShow) {
                    if (App.currentActivity != null && App.currentActivity?.isDestroyed?.not() == true && App.currentActivity?.isFinishing?.not() == true && progressDialog != null && progressDialog?.isShowing == true) {
                        progressDialog?.dismiss()
                    }
                }
                listeners?.invoke(true)
                listeners = null
                return
            }

            initAd()
            object : CountDownTimer(4500, 10) {
                override fun onTick(millisUntilFinished: Long) {
                }

                override fun onFinish() {
                    if (dialogShow) {
                        if (App.currentActivity != null && App.currentActivity?.isFinishing?.not() == true && progressDialog != null && progressDialog?.isShowing == true) {
                            progressDialog?.dismiss()
                        }
                    }
                    if (!isShowingAd) {
                        listeners?.invoke(true)
                        listeners = null
                    }
                }
            }.start()
        }
    }

    fun fetchAd(admobId: String? = "", listener: ((result: Boolean) -> Unit)? = null) {
        if (isAdAvailable()) {
            return
        }

        loadCallback = object : AppOpenAd.AppOpenAdLoadCallback() {
            override fun onAdLoaded(ad: AppOpenAd) {
                super.onAdLoaded(ad)
                isLoading = false
                isFailed = false
                appOpenAd = ad
                loadTime = Date().time
                if (listeners != null) {
                    isLoaded = true
                    showAdIfAvailable(false, isDialogShown = dialogShow, listeners)
                }
            }

            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                super.onAdFailedToLoad(loadAdError)
                isFailed = true
                isLoading = false
                if (dialogShow) {
                    if (App.currentActivity != null && App.currentActivity?.isDestroyed?.not() == true && App.currentActivity?.isFinishing?.not() == true && progressDialog != null && progressDialog?.isShowing == true) {
                        progressDialog?.dismiss()
                    }
                }
                listener?.invoke(true)
                if (listeners != null) listeners?.invoke(true)
                listeners = null
            }
        }
        val request = getAdRequest()
        try {
            App.getAppContext().let {
                AppOpenAd.load(it, admobId ?: "", request, loadCallback as AppOpenAd.AppOpenAdLoadCallback)
            }
        } catch (ex: Exception) {
            isFailed = true
            isLoading = false
            if (dialogShow) {
                if (App.currentActivity != null && App.currentActivity?.isFinishing?.not() == true && progressDialog != null && progressDialog?.isShowing == true) {
                    progressDialog?.dismiss()
                }
            }
            if (listeners != null) listeners?.invoke(true)
            listeners = null
        }
        isLoading = true
        isFailed = false
    }

    private fun getAdRequest(): AdRequest {
        return AdRequest.Builder().build()
    }

    private fun wasLoadTimeLessThanNHoursAgo(numHours: Long): Boolean {
        val dateDifference = Date().time - loadTime
        val numMilliSecondsPerHour = 3600000
        return (dateDifference < (numMilliSecondsPerHour * numHours))
    }

    private fun isAdAvailable(): Boolean {
        return appOpenAd != null && wasLoadTimeLessThanNHoursAgo(4)
    }

    fun onFinish() {
        if (!isShowingAd && isLoading) {
            if (dialogShow) {
                if (App.currentActivity != null && App.currentActivity?.isFinishing?.not() == true && progressDialog != null && progressDialog?.isShowing == true) {
                    progressDialog?.dismiss()
                }
            }
            listeners?.invoke(true)
            listeners = null
        }
    }
}