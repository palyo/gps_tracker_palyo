package com.post.call.info.ui.activity

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.Window
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.post.call.info.utils.Formatter
import com.post.call.info.utils.CALL_COUNTER
import com.post.call.info.utils.CALL_TIME
import com.post.call.info.utils.CALL_TYPE
import com.post.call.info.utils.END_TIME
import com.post.call.info.utils.EXTRA_MOBILE_NUMBER
import com.post.call.info.utils.IS_OPEN_FROM_NOTIFICATION
import com.post.call.info.utils.START_TIME
import com.post.call.info.utils.isBannerLoad
import com.post.call.info.ads.PostCallBannerAds
import com.post.call.info.utils.HomeWatcher
import com.post.call.info.PostCallApplication
import com.post.call.info.PostCallConfig
import com.post.call.info.R
import com.post.call.info.ads.PostCallNativeAds
import com.post.call.info.databinding.PostCallActivityBinding
import com.post.call.info.receiver.PhoneCallReceiver
import com.post.call.info.ui.adapter.QuickMessageItemClickListener
import com.post.call.info.ui.fragment.DefaultMsgFragment
import com.post.call.info.ui.fragment.MessagesFragment
import com.post.call.info.ui.fragment.OptionsFragment
import kotlin.jvm.java

class PostCallActivity : AppCompatActivity(), QuickMessageItemClickListener {

    companion object {
        private var postCallActivity: PostCallActivity? = null
        fun getPostCallActivity(): PostCallActivity? = postCallActivity
    }

    private lateinit var binding: PostCallActivityBinding
    private var bannerAds: PostCallBannerAds? =null
    var nativeADs: PostCallNativeAds? = null

    private var callTime: String = ""
    private var callType: String = ""
    private var isBannerPostCall: Boolean = false
    private var isFirstOpenScreen: Boolean = false
    private var isOpenFromNotification: Boolean = false
    private var mobileNumber: String = ""


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try { setLockScreen() } catch (_: Exception) {}
        binding = PostCallActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { view, windowInsetsCompat ->
            val insets: Insets = windowInsetsCompat.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(insets.left, insets.top, insets.right, insets.bottom)
            windowInsetsCompat
        }
        disabledOpenAds()
        postCallActivity = this
        manageNavigationBar()
        initData()
        removeActivity()
        clickListeners()
        initAds()
    }

    override fun onQuickMsgItemClick(str: String?) {
        try {
            val intent = Intent("android.intent.action.VIEW", Uri.parse("sms:"))
            intent.putExtra("sms_body", str)
            disabledOpenAds()
            startActivity(intent)
        } catch (_: Exception) {}
    }

    override fun onResume() {
        super.onResume()
        disabledOpenAds()
        if (!isFirstOpenScreen) {
            isFirstOpenScreen = true
        } else if (PostCallApplication.isNativeGooglePostCall && !isBannerPostCall) {
            val hasNative = PostCallApplication.nativeAdsPostCall != null
            Log.e("PostCall", "onResume google show NativeAd-> $hasNative")
            if (PostCallApplication.nativeAdsPostCall != null) {
                if (System.currentTimeMillis() - PostCallApplication.adNativeLoadTime >= PostCallApplication.adNativeExpirationTime) {
                    Log.e("PostCall", "onResume google ads isExpired -> trueee")
                    nativeADs?.onDestroyAdView()
                    loadBannerADs()
                    return
                }
                Log.e("PostCall", "onResume google ads isExpired -> falseee")
            }
        } else {
            val isReLoad = PostCallApplication.isReLoadBannerAds
            Log.e("PostCall", "onResume banner is call isReLoad $isReLoad")
            if (PostCallApplication.isReLoadBannerAds) loadBannerADs()
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
            PostCallApplication.nativeListener = null
        } catch (_: Exception) {}
        if (Build.VERSION.SDK_INT > 24) finishAffinity() else finish()
    }

    private fun initAds() {
        isBannerPostCall = isBannerLoad(intent.getIntExtra(CALL_COUNTER, 0))
        bannerAds = PostCallBannerAds()
        nativeADs = PostCallNativeAds(this)
        binding.cardViewNative.visibility = View.VISIBLE
        nativeADs?.showLoadingLayoutNative(binding.frameNative)
        if (isBannerPostCall) {
            nativeADs?.initNativeListener(object : PostCallNativeAds.NativeListener {
                override fun nativeLoad() {
                    Log.e("PostCall", "NativeTag goggle PS setNativeListener nativeLoad")
                    binding.cardViewNative.visibility = View.VISIBLE
                    nativeADs?.showNative(binding.frameNative, PostCallApplication.nativeAdsPostCall, false)
                }

                override fun nativeFailed() {
                    Log.e("PostCall", "NativeTag goggle PS setNativeListener nativeFailed")
                    binding.frameNative.removeAllViews()
                    binding.frameNative.visibility = View.GONE
                    binding.cardViewNative.visibility = View.GONE
                    if (PostCallApplication.isCallingStart) return
                    loadBannerADs()
                }
            })
        }
        Log.e("PostCall", "nativeLoadPostCall NativeTag isBannerPostCall $isBannerPostCall nativeAdsPostCall-> ${PostCallApplication.nativeAdsPostCall} isNativeFailedGooglePostCall ${PostCallApplication.isNativeFailedGooglePostCall}")
        if (isOpenFromNotification && !isBannerPostCall && PostCallApplication.nativeAdsPostCall == null && !PostCallApplication.isNativeFailedGooglePostCall) {
            Log.e("PostCall", "nativeLoadPostCall NativeTag googleNative call")
            PostCallApplication.isNativeGooglePostCall = true
            nativeADs?.loadNativeAds(false)
        }
        if (PostCallApplication.isNativeGooglePostCall && !isBannerPostCall) {
            callBannerNativeAd()
        } else {
            loadBannerADs()
        }
    }

    private fun callBannerNativeAd() {
        if (PostCallApplication.nativeAdsPostCall != null &&
            !PostCallApplication.isNativePostCallLoading &&
            System.currentTimeMillis() - PostCallApplication.adNativeLoadTime >= PostCallApplication.adNativeExpirationTime) {
            PostCallApplication.adNativeLoadTime = 0L
            PostCallApplication.nativeAdsPostCall = null
            nativeADs?.loadPostNative()
            Log.e("PostCall", "NativeTag PS ad Time expired")
        }
        loadNativeAds(binding.frameNative, binding.cardViewNative)
    }

    fun loadNativeAds(frameLayout: FrameLayout, cardViewADs: View) {
        cardViewADs.visibility = View.VISIBLE
        val isLoading = PostCallApplication.isNativePostCallLoading
        Log.e("PostCall", "NativeTag PS show native google isNativePostCallLoading-> $isLoading")
        if (PostCallApplication.isNativePostCallLoading) {
            Log.e("PostCall", "NativeTag PS loader show")
            return
        }
        Log.e("PostCall", "NativeTag PS else show native google ads ${PostCallApplication.nativeAdsPostCall != null}")
        frameLayout.removeAllViews()
        if (PostCallApplication.nativeAdsPostCall != null) {
            nativeADs?.showNative(frameLayout, PostCallApplication.nativeAdsPostCall, false)
            return
        }
        cardViewADs.visibility = View.GONE
        loadBannerADs()
    }

    fun loadBannerADs() {
        Log.e("PostCall", "NativeTag loadBannerADs isNativeFailedGooglePostCall ${PostCallApplication.isNativeFailedGooglePostCall}")
        if (isFinishing || isDestroyed) return
        if (PostCallApplication.isNativeFailedGooglePostCall || !PostCallApplication.isNativePostCallLoading || PostCallApplication.nativeAdsPostCall == null) {
            val postCallBannerAds = PostCallBannerAds()
            if (binding.frameNative.childCount == 0) {
                nativeADs?.showLoadingLayoutNative(binding.frameNative)
            }
            if (isBannerAdExpired()) {
                postCallBannerAds.onAdExpired(this)
            } else {
                postCallBannerAds.loadBanner(this, binding.frameNative, binding.cardViewNative)
            }
            postCallBannerAds.initBannerListener(object : PostCallBannerAds.BannerListener {
                override fun onBannerAdLoaded() {
                    Log.e("PostCall", "PostAct onBannerAdLoaded")
                    PostCallApplication.destroyUnusedNative()
                    binding.frameNative.removeAllViews()
                    binding.frameNative.addView(PostCallApplication.bannerAdView)
                }

                override fun onBannerFailed() {
                    Log.e("PostCall", "PostAct onBannerFailed")
                    if (PostCallApplication.isCallingStart) return
                    nativeADs?.loadPostNative()
                    nativeADs?.initNativeListener(object : PostCallNativeAds.NativeListener {
                        override fun nativeLoad() {
                            Log.e("PostCall", "NativeTag goggle PS setNativeListener nativeLoad")
                            binding.cardViewNative.visibility = View.VISIBLE
                            nativeADs?.showNative(binding.frameNative, PostCallApplication.nativeAdsPostCall, false)
                        }

                        override fun nativeFailed() {
                            Log.e("PostCall", "NativeTag goggle PS setNativeListener nativeFailed")
                            binding.frameNative.removeAllViews()
                            binding.frameNative.visibility = View.GONE
                            binding.cardViewNative.visibility = View.GONE
                        }
                    })
                }
            })
        }
    }

    private fun isBannerAdExpired(): Boolean {
        if (PostCallApplication.bannerAdView == null ||
            PostCallApplication.bannerAdLoading ||
            System.currentTimeMillis() - PostCallApplication.adBannerLoadTime < PostCallApplication.adBannerExpirationTime) {
            return false
        }
        Log.d("PostCall", "PostAct bannerAD is Expired.....")
        return true
    }

    class ViewPagerAdapter(
        fragmentManager: FragmentManager,
        lifecycle: Lifecycle,
        private val firstFragmentProvider: () -> Fragment
    ) : FragmentStateAdapter(fragmentManager, lifecycle) {

        override fun containsItem(itemId: Long): Boolean = itemId in 0..2
        override fun getItemCount(): Int = 3
        override fun getItemId(position: Int): Long = position.toLong()

        override fun createFragment(position: Int): Fragment = when (position) {
            1 -> MessagesFragment()
            2 -> OptionsFragment()
            else -> firstFragmentProvider()
        }
    }

    private fun removeActivity() {
        val homeWatcher = HomeWatcher(this)
        homeWatcher.setOnHomePressedListener(object : HomeWatcher.OnHomePressedListener {
            override fun onHomeLongPressed() {}
            override fun onHomePressed() {
                nativeAdDestroy()
                finishAffinity()
            }
        })
        homeWatcher.startWatch()
    }

    private fun initData() {
        try {
            isOpenFromNotification = intent.getBooleanExtra(IS_OPEN_FROM_NOTIFICATION, false)
            mobileNumber = intent.getStringExtra(EXTRA_MOBILE_NUMBER) ?: ""
            callTime = intent.getSerializableExtra(CALL_TIME).toString()
            callType = intent.getStringExtra(CALL_TYPE) ?: ""
            Log.w("PostCall", "mobileNumber: $mobileNumber")
            Log.w("PostCall", "startTime: ${intent.getSerializableExtra(START_TIME)}")
            Log.w("PostCall", "endTime: ${intent.getSerializableExtra(END_TIME)}")
            Log.w("PostCall", "callType: $callType")
            val timeDiff = Formatter.getTimeDiff(intent.getLongExtra(START_TIME, 0L), intent.getLongExtra(END_TIME, 0L))
            Log.w("PostCall", "time: $timeDiff")
            try {
                val dataFragmentClass = PostCallConfig.dataFragmentClass
                val factory = supportFragmentManager.fragmentFactory
                val cl = classLoader
                binding.viewPager.adapter = ViewPagerAdapter(
                    supportFragmentManager,
                    lifecycle
                ) {
                    if (dataFragmentClass != null) factory.instantiate(cl, dataFragmentClass.name)
                    else DefaultMsgFragment()
                }
                binding.viewPager.isSaveEnabled = false
            } catch (unused: Exception) {}
            binding.callerTime.text = Formatter.extractTime(callTime)
            binding.callerDuration.text = timeDiff
            binding.callerType.text = callType
            TabLayoutMediator(binding.tbCallerCategory, binding.viewPager) { tab, i ->
                when (i) {
                    0 -> tab.icon = ContextCompat.getDrawable(this, R.drawable.post_ic_menu_tab_post_call)
                    1 -> tab.icon = ContextCompat.getDrawable(this, R.drawable.post_ic_message_tab_post_call)
                    2 -> tab.icon = ContextCompat.getDrawable(this, R.drawable.post_ic_more_tab_post_call)
                }
            }.attach()
            binding.tbCallerCategory.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
                override fun onTabReselected(tab: TabLayout.Tab) {}
                override fun onTabUnselected(tab: TabLayout.Tab) {}
                override fun onTabSelected(tab: TabLayout.Tab) {
                    binding.viewPager.currentItem = tab.position
                }
            })
        } catch (e: Exception) {
            Log.w("PostCall", "POSTCALL ::: Exception-->> ${e.message}")
        }
    }

    private fun clickListeners() {
        binding.callerAppIcon.setOnClickListener { goToApp() }
        binding.mobileIcon.setOnClickListener { callingMethod() }
    }

    private fun goToApp() {
//        startActivity(Intent(this, SplashActivity::class.java))
//        finish()
    }

    private fun callingMethod() {
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:"))
        if (intent.resolveActivity(packageManager) != null) startActivity(intent)
        else startActivity(Intent.createChooser(intent, null))
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        nativeAdDestroy()
        finish()
    }

    override fun onDestroy() {
        nativeAdDestroy()
        super.onDestroy()
    }

    fun nativeAdDestroy() {
        PostCallApplication.isNativeFailedGooglePostCall = false
        PostCallApplication.isReLoadBannerAds = false
        nativeADs?.onDestroyAd()
        Log.e("PostCall", "nativeAdDestroy call  ${PostCallApplication.bannerAdView != null}")
        bannerAds?.onDestroyAd()
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        return try {
            val dispatchResult = super.dispatchTouchEvent(event)
            if (currentFocus is EditText) {
                val iArr = IntArray(2)
                currentFocus?.getLocationOnScreen(iArr)
                val rawX = event.rawX
                val left = currentFocus?.left ?: 0
                val intValue = rawX + left - iArr[0]
                val rawY = (event.rawY + (currentFocus?.top ?: 0)) - iArr[1]
                if (event.action == 1 &&
                    (intValue < (currentFocus?.left ?: 0) || intValue >= (currentFocus?.right ?: 0) ||
                            rawY < (currentFocus?.top ?: 0) || rawY > (currentFocus?.bottom ?: 0))) {
                    val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                    var iBinder: IBinder? = null
                    val window: Window? = window
                    if (window?.currentFocus != null) iBinder = window.currentFocus!!.windowToken
                    imm.hideSoftInputFromWindow(iBinder, 0)
                    currentFocus?.clearFocus()
                }
            }
            dispatchResult
        } catch (_: Exception) {
            super.dispatchTouchEvent(event)
        }
    }

    private fun setLockScreen() {
        if (Build.VERSION.SDK_INT >= 27) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            window.addFlags(6815873)
        }
    }

    private fun manageNavigationBar() {
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                    View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
    }
}
