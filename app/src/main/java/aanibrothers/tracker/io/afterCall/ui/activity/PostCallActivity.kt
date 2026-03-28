package aanibrothers.tracker.io.afterCall.ui.activity

import aanibrothers.tracker.io.R
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import androidx.core.graphics.Insets
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import aanibrothers.tracker.io.afterCall.Formatter
import aanibrothers.tracker.io.afterCall.HomeWatcher
import aanibrothers.tracker.io.afterCall.PostCallApplication
import aanibrothers.tracker.io.afterCall.PostCallApplication.Companion.getAdNativeExpirationTime
import aanibrothers.tracker.io.afterCall.PostCallApplication.Companion.getAdNativeLoadTime
import aanibrothers.tracker.io.afterCall.PostCallApplication.Companion.getBannerAdView
import aanibrothers.tracker.io.afterCall.PostCallApplication.Companion.getNativeAdsPostCall
import aanibrothers.tracker.io.afterCall.PostCallApplication.Companion.isCallingStart
import aanibrothers.tracker.io.afterCall.PostCallApplication.Companion.isNativeFailedGooglePostCall
import aanibrothers.tracker.io.afterCall.PostCallApplication.Companion.isNativeGooglePostCall
import aanibrothers.tracker.io.afterCall.PostCallApplication.Companion.isNativePostCallLoading
import aanibrothers.tracker.io.afterCall.PostCallApplication.Companion.isReLoadBannerAds
import aanibrothers.tracker.io.afterCall.PostCallApplication.Companion.setAdNativeLoadTime
import aanibrothers.tracker.io.afterCall.PostCallApplication.Companion.setNativeAdsPostCall
import aanibrothers.tracker.io.afterCall.PostCallApplication.Companion.setNativeGooglePostCall
import aanibrothers.tracker.io.afterCall.ads.PostCallBannerAds
import aanibrothers.tracker.io.afterCall.ads.PostCallBannerAds.BannerListener
import aanibrothers.tracker.io.afterCall.ads.PostCallNativeAds
import aanibrothers.tracker.io.afterCall.extensions.CALL_COUNTER
import aanibrothers.tracker.io.afterCall.extensions.CALL_TIME
import aanibrothers.tracker.io.afterCall.extensions.CALL_TYPE
import aanibrothers.tracker.io.afterCall.extensions.END_TIME
import aanibrothers.tracker.io.afterCall.extensions.EXTRA_MOBILE_NUMBER
import aanibrothers.tracker.io.afterCall.extensions.IS_OPEN_FROM_NOTIFICATION
import aanibrothers.tracker.io.afterCall.extensions.START_TIME
import aanibrothers.tracker.io.afterCall.extensions.isBannerLoad
import com.contact.phone.dailer.postCall.receiver.PhoneCallReceiver
import aanibrothers.tracker.io.afterCall.ui.adapter.QuickMessageItemClickListener
import aanibrothers.tracker.io.afterCall.ui.fragment.DefaultMsgFragment
import aanibrothers.tracker.io.afterCall.ui.fragment.MessagesFragment
import aanibrothers.tracker.io.afterCall.ui.fragment.OptionsFragment
import aanibrothers.tracker.io.databinding.ActivityPostCallBinding
import aanibrothers.tracker.io.ui.LauncherActivity
import coder.apps.space.library.base.BaseActivity
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator


class PostCallActivity : BaseActivity<ActivityPostCallBinding>(ActivityPostCallBinding::inflate), QuickMessageItemClickListener {
      private lateinit var bannerAds: PostCallBannerAds
    private lateinit var nativeADs: PostCallNativeAds
    private var callTime: String = "null"
    private var callType: String = "null"
    private var isBannerPostCall = false
    private var isOpenFromNotification = false
    private var mobileNumber: String = "null"
    private var isFirstOpenScreen = false

    companion object {
        private var postCallActivity: PostCallActivity? = null

        fun getPostCallActivity(): PostCallActivity? {
            return postCallActivity
        }
    }

    override fun ActivityPostCallBinding.initExtra() {
        try {
            setLockScreen()
        } catch (unused: Exception) {
        }
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { view, windowInsetsCompat ->
            applyWindowInsets(view, windowInsetsCompat)
        }
        disabledOpenAds()
        postCallActivity = this@PostCallActivity
        manageNavigationBar()
        initData()
        removeActivity()
        initAds()
    }

    override fun ActivityPostCallBinding.initListeners() {
        callerAppIcon.setOnClickListener { goToApp() }
        mobileIcon.setOnClickListener { callingMethod() }
    }

    override fun ActivityPostCallBinding.initView() {

    }

    private fun applyWindowInsets(view: View, insets: WindowInsetsCompat): WindowInsetsCompat {
        val systemInsets: Insets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
        view.setPadding(
            systemInsets.left,
            systemInsets.top,
            systemInsets.right,
            systemInsets.bottom
        )
        return insets
    }

    override fun onQuickMsgItemClick(text: String?) {
        try {
            val intent = Intent("android.intent.action.VIEW", "sms:".toUri())
            intent.putExtra("sms_body", text)
            disabledOpenAds()
            startActivity(intent)
        } catch (unused: Exception) {
        }
    }

    private fun disabledOpenAds() {
        PostCallApplication.setOpenAdHide(true)
    }

    fun finishActivity() {
        PhoneCallReceiver.setCallEnded(false)
        nativeAdDestroy()
        postCallActivity = null
        try {
            PostCallApplication.setNativeListener(null)
        } catch (ignored: Exception) {
        }
        finishAffinity()
    }

    public override fun onResume() {
        super.onResume()
        disabledOpenAds()
        if (!this.isFirstOpenScreen) {
            this.isFirstOpenScreen = true
        } else if (isNativeGooglePostCall() && !this.isBannerPostCall) {
            getNativeAdsPostCall() != null
            if (getNativeAdsPostCall() != null) {
                if (System.currentTimeMillis() - getAdNativeLoadTime() >= getAdNativeExpirationTime()) {
                    Log.e("PostCall", "onResume google ads isExpired -> trueee")
                    nativeADs.onDestroyAdView()
                    binding?.loadBannerADs()
                    return
                }
            }
        } else {
            val isReLoadBannerAds = isReLoadBannerAds()
            if (isReLoadBannerAds) {
                binding?.loadBannerADs()
            }
        }
    }


    private fun ActivityPostCallBinding.initAds() {
        isBannerPostCall =
            isBannerLoad(intent.getIntExtra(CALL_COUNTER, 0))
        bannerAds = PostCallBannerAds()
        nativeADs = PostCallNativeAds(this@PostCallActivity)
        cardViewNative.visibility = View.VISIBLE
        nativeADs.showLoadingLayoutNative(frameNative)
        if (isBannerPostCall) {
            nativeADs.initNativeListener(object : PostCallNativeAds.NativeListener {
                override fun nativeLoad() {
                    Log.e("PostCall", "NativeTag  goggle PS setNativeListener nativeLoad")
                    cardViewNative.visibility = View.VISIBLE
                    nativeADs.showNative(frameNative, getNativeAdsPostCall(), false)
                }

                override fun nativeFailed() {
                    Log.e("PostCall", "NativeTag goggle PS setNativeListener nativeFailed")
                    frameNative.removeAllViews()
                    frameNative.visibility = View.GONE
                    cardViewNative.visibility = View.GONE
                    if (isCallingStart()) {
                        return
                    }
                    loadBannerADs()
                }
            })
        }
        Log.e(
            "PostCall",
            "nativeLoadPostCall NativeTag  isBannerPostCall " + isBannerPostCall + "  nativeAdsPostCall-> " + getNativeAdsPostCall() + "  isNativeFailedGooglePostCall " + isNativeFailedGooglePostCall()
        )
        if (isOpenFromNotification && !isBannerPostCall && getNativeAdsPostCall() == null && !isNativeFailedGooglePostCall()) {
            Log.e("PostCall", "nativeLoadPostCall NativeTag googleNative call ")
            setNativeGooglePostCall(true)
            PostCallNativeAds(this@PostCallActivity).loadNativeAds(false)
        }
        if (isNativeGooglePostCall() && !isBannerPostCall) {
            callBannerNativeAd()
        } else {
            loadBannerADs()
        }
    }

    private fun ActivityPostCallBinding.callBannerNativeAd() {
        if (getNativeAdsPostCall() != null && !isNativePostCallLoading() && System.currentTimeMillis() - getAdNativeLoadTime() >= getAdNativeExpirationTime()) {
            setAdNativeLoadTime(0L)
            setNativeAdsPostCall(null)
            nativeADs.loadPostNative()
            Log.e("PostCall", "NativeTag PS ad Time expired ")
        }
        loadNativeAds(frameNative, cardViewNative)
    }

    fun loadNativeAds(frameLayout: FrameLayout, cardViewADs: View) {
        cardViewADs.visibility = View.VISIBLE
        val isNativePostCallLoading = isNativePostCallLoading()
        Log.e(
            "PostCall",
            "NativeTag PS show native google isNativePostCallLoading-> " + isNativePostCallLoading
        )
        if (isNativePostCallLoading()) {
            Log.e("PostCall", "NativeTag PS loader show")
            return
        }
        getNativeAdsPostCall() != null
        frameLayout.removeAllViews()
        if (getNativeAdsPostCall() != null) {
            nativeADs.showNative(frameLayout, getNativeAdsPostCall(), false)
            return
        }
        cardViewADs.visibility = View.GONE
        binding?.loadBannerADs()
    }


    fun ActivityPostCallBinding.loadBannerADs() {
        if (isFinishing || isDestroyed) {
            return
        }
        if (isNativeFailedGooglePostCall() || !isNativePostCallLoading() || getNativeAdsPostCall() == null) {
            val postCallBannerAds = PostCallBannerAds()
            if (frameNative.childCount == 0) {
                nativeADs.showLoadingLayoutNative(frameNative)
            }
            if (isBannerAdExpired()) {
                postCallBannerAds.onAdExpired(this@PostCallActivity)
            } else {
                postCallBannerAds.loadBanner(this@PostCallActivity, frameNative, cardViewNative)
            }
            postCallBannerAds.initBannerListener(object : BannerListener {
                override fun onBannerAdLoaded() {
                   frameNative.removeAllViews()
                   frameNative.addView(getBannerAdView())
                }

                override fun onBannerFailed() {
                    if (isCallingStart()) {
                        return
                    }
                    nativeADs.loadPostNative()
                    nativeADs.initNativeListener(object : PostCallNativeAds.NativeListener {
                        override fun nativeLoad() {
                            cardViewNative.visibility = View.VISIBLE
                            nativeADs.showNative(
                                frameNative,
                                getNativeAdsPostCall(),
                                false
                            )
                        }

                        override fun nativeFailed() {
                            frameNative.removeAllViews()
                            frameNative.visibility = View.GONE
                            cardViewNative.visibility = View.GONE
                        }
                    })
                }
            })
        }
    }

    private fun isBannerAdExpired(): Boolean {
        if (getBannerAdView() == null ||
            PostCallApplication.getBannerAdLoading() ||
            System.currentTimeMillis() - PostCallApplication.getAdBannerLoadTime() <
            PostCallApplication.getAdBannerExpirationTime()
        ) {
            return false
        }
        Log.e("PostCall", "PostAct bannerAD is Expired.....")
        return true
    }

    class ViewPagerAdapter(fragmentManager: FragmentManager, lifecycle: Lifecycle) :
        FragmentStateAdapter(fragmentManager, lifecycle) {
        override fun containsItem(itemId: Long): Boolean {
            return 0 <= itemId && itemId < 3
        }

        override fun getItemCount(): Int {
            return 3
        }

        override fun getItemId(position: Int): Long {
            return position.toLong()
        }

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> DefaultMsgFragment()
                1 -> MessagesFragment()
                2 -> OptionsFragment()
                else -> DefaultMsgFragment()
            }
        }
    }

    private fun removeActivity() {
        val homeWatcher = HomeWatcher(this)
        homeWatcher.setOnHomePressedListener(object : HomeWatcher.OnHomePressedListener {
            override fun onHomeLongPressed() {
            }

            override fun onHomePressed() {
                nativeAdDestroy()
                finishAffinity()
            }
        })
        homeWatcher.startWatch()
    }

    private fun ActivityPostCallBinding.initData() {
        try {
            isOpenFromNotification = intent.getBooleanExtra(IS_OPEN_FROM_NOTIFICATION, false)
            mobileNumber = intent.getStringExtra(EXTRA_MOBILE_NUMBER) ?: "null"
            callTime = intent.getSerializableExtra(CALL_TIME)?.toString() ?: "null"
            callType = intent.getStringExtra(CALL_TYPE) ?: "null"
            val timeDiff = Formatter.getTimeDiff(
                intent.getLongExtra(START_TIME, 0L),
                intent.getLongExtra(END_TIME, 0L)
            )
            try {
                viewPager.adapter = ViewPagerAdapter(supportFragmentManager, lifecycle)
                viewPager.isSaveEnabled = false
            } catch (unused: Exception) {
            }
            callerTime.text = Formatter.extractTime(callTime)
            callerDuration.text = timeDiff
            callerType.text = callType
            TabLayoutMediator(tbCallerCategory, viewPager) { tab, position ->
                when (position) {
                    0 -> tab.icon =
                        ContextCompat.getDrawable(this@PostCallActivity, R.drawable.post_ic_menu_tab_post_call)

                    1 -> tab.icon =
                        ContextCompat.getDrawable(this@PostCallActivity, R.drawable.post_ic_message_tab_post_call)

                    2 -> tab.icon =
                        ContextCompat.getDrawable(this@PostCallActivity, R.drawable.post_ic_more_tab_post_call)
                }
            }.attach()
            tbCallerCategory.addOnTabSelectedListener(object :
                TabLayout.OnTabSelectedListener {
                override fun onTabReselected(tab: TabLayout.Tab) {
                }

                override fun onTabUnselected(tab: TabLayout.Tab) {
                }

                override fun onTabSelected(tab: TabLayout.Tab) {
                    viewPager.currentItem = tab.position
                }
            })
        } catch (e: Exception) {
            Log.e("PostCall", "POSTCALL ::: Exception-->>  ${e.message}")
        }
    }

    private fun goToApp() {
        startActivity(Intent(this, LauncherActivity::class.java))
        finish()
    }

    private fun callingMethod() {
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:"))
        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
        } else {
            startActivity(Intent.createChooser(intent, null))
        }
    }

    override fun onBackPressed() {
        nativeAdDestroy()
        super.onBackPressed()
        finish()
    }

    override fun onDestroy() {
        nativeAdDestroy()
        super.onDestroy()
    }

    fun nativeAdDestroy() {
        PostCallApplication.setNativeFailedGooglePostCall(false)
        PostCallApplication.setReLoadBannerAds(false)
        PostCallApplication.setNativeListener(null)
        PostCallApplication.setBannerListener(null)
        if (this::nativeADs.isInitialized) {
            nativeADs.onDestroyAd()
        }
        val hasBanner = getBannerAdView() != null
        Log.e("PostCall", "nativeAdDestroy call  $hasBanner")
        if (this::bannerAds.isInitialized) {
            bannerAds.onDestroyAd()
        }
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        return try {
            val focusView = currentFocus
            val result = super.dispatchTouchEvent(event)
            if (focusView is EditText) {
                val currentFocusView = currentFocus
                if (currentFocusView != null) {
                    val location = IntArray(2)
                    currentFocusView.getLocationOnScreen(location)
                    val rawX = event.rawX
                    val rawY = event.rawY
                    val adjustedX = (rawX + currentFocusView.left) - location[0]
                    val adjustedY = (rawY + currentFocusView.top) - location[1]
                    if (event.action == MotionEvent.ACTION_UP &&
                        (adjustedX < currentFocusView.left ||
                                adjustedX >= currentFocusView.right ||
                                adjustedY < currentFocusView.top ||
                                adjustedY > currentFocusView.bottom)
                    ) {
                        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                        val token: IBinder? = window?.currentFocus?.windowToken
                        imm.hideSoftInputFromWindow(token, 0)
                        focusView.clearFocus()
                    }
                }
            }
            Log.e("", "onFocusChange dispatchTouchEvent ret $result")
            result
        } catch (unused: Exception) {
            super.dispatchTouchEvent(event)
        }
    }

    private fun setLockScreen() {
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        window.addFlags(6815873)
        return
    }

    private fun manageNavigationBar() {
        window.decorView.systemUiVisibility = 514
    }
}
