package aanibrothers.tracker.io.ui

import aanibrothers.tracker.io.R
import aanibrothers.tracker.io.cdo.*
import aanibrothers.tracker.io.databinding.*
import aanibrothers.tracker.io.extension.*
import aanibrothers.tracker.io.module.*
import aanibrothers.tracker.io.ui.*
import android.Manifest
import android.annotation.*
import android.content.*
import android.net.*
import android.os.*
import android.provider.*
import android.text.*
import android.text.method.*
import android.text.style.*
import android.view.*
import android.widget.*
import androidx.activity.*
import androidx.activity.result.*
import androidx.activity.result.contract.*
import androidx.viewpager.widget.*
import androidx.viewpager.widget.ViewPager.OnPageChangeListener
import coder.apps.space.library.base.*
import coder.apps.space.library.extension.*
import coder.apps.space.library.helper.*
import com.bumptech.glide.*
import com.google.android.material.imageview.*

class NewPermissionActivity : BaseActivity<ActivityNewPermissionBinding>(ActivityNewPermissionBinding::inflate) {
    private var permissionFlow: Int = 0
    private var introViewPagerAdapter: IntroViewPagerAdapter? = null
    private var settingOverLay: HandleSettingPreview? = null
    private val permissions = arrayOf(Manifest.permission.READ_PHONE_STATE)
    private var screens: MutableList<Int> = mutableListOf(R.drawable.ic_intro_1, R.drawable.ic_intro_2, R.drawable.ic_intro_3)
    private var displayOverLauncher: ActivityResultLauncher<Intent> = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        settingOverLay?.cancelPollingImeSettings()
        val isMoreNeeded = !hasOverlayPermission() || !hasPermissions(permissions)
        onBoardingSuccess(isMoreNeeded = isMoreNeeded)
    }
    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permission ->
        if (!hasOverlayPermission()) {
            permissionFlow = 1
            Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
                settingOverLay?.startPollingImeSettings()
                displayOverLauncher.launch(this)
                Handler(mainLooper).postDelayed({
                    startActivity(Intent(this@NewPermissionActivity, PromptActivity::class.java))
                }, 100L)
            }
            return@registerForActivityResult
        }
        val isMoreNeeded = !hasOverlayPermission() || !hasPermissions(permissions)
        onBoardingSuccess(isMoreNeeded = isMoreNeeded)
    }

    override fun ActivityNewPermissionBinding.initExtra() {
        setupPager()
    }

    private fun ActivityNewPermissionBinding.setupPager() {
        introViewPagerAdapter = IntroViewPagerAdapter(this@NewPermissionActivity)
        viewPager.adapter = introViewPagerAdapter
        buttonNext.setOnClickListener {
            if (viewPager.currentItem < screens.size - 1) {
                viewPager.currentItem++
                updateTabs()
            } else {
                initPermission()
            }
        }

        viewPager.addOnPageChangeListener(object : OnPageChangeListener {
            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {

            }

            override fun onPageSelected(position: Int) {
                when (position) {
                    0 -> {
                        isTermsAgree.beGone()
                        buttonNext.text = getString(R.string.button_next)
                        textBody.text = getString(R.string.text_intro_page_1)
                    }

                    1 -> {
                        isTermsAgree.beGone()
                        buttonNext.text = getString(R.string.button_next)
                        textBody.text = getString(R.string.text_intro_page_2)
                    }

                    2 -> {
                        isTermsAgree.beVisible()
                        buttonNext.text = getString(R.string.button_allow_and_continue)
                        val baseText = getString(R.string.message_policy)
                        val spannableString = SpannableString(baseText)
                        val linkText = "Privacy Policy"
                        val startIndex = baseText.indexOf(linkText)
                        val endIndex = startIndex + linkText.length
                        val clickableSpan: ClickableSpan = object : ClickableSpan() {
                            override fun onClick(widget: View) {
                                launchUrl(getPolicyLink())
                            }
                        }
                        spannableString.setSpan(clickableSpan, startIndex, endIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                        textBody.text = spannableString
                        textBody.movementMethod = LinkMovementMethod.getInstance()
                    }
                }

                updateTabs()
            }

            override fun onPageScrollStateChanged(state: Int) {

            }
        })
        updateTabs()
    }

    private fun ActivityNewPermissionBinding.updateTabs() {
        when (viewPager.currentItem) {
            0 -> {
                view1.isSelected = true
                view2.isSelected = false
                view3.isSelected = false
            }

            1 -> {
                view1.isSelected = true
                view2.isSelected = true
                view3.isSelected = false
            }

            2 -> {
                view1.isSelected = true
                view2.isSelected = true
                view3.isSelected = true
            }
        }
    }

    private fun ActivityNewPermissionBinding.initPermission() {
        if (!isTermsAgree.isChecked) {
            Toast.makeText(this@NewPermissionActivity, "Please agree with our terms and privacy", Toast.LENGTH_SHORT).show()
            return
        }
        if (!hasPermissions(permissions)) {
            permissionLauncher.launch(permissions)
            return
        }
        if (!hasOverlayPermission()) {
            permissionFlow = 1
            Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
                settingOverLay?.startPollingImeSettings()
                displayOverLauncher.launch(this)
                Handler(mainLooper).postDelayed({
                    startActivity(Intent(this@NewPermissionActivity, PromptActivity::class.java))
                }, 500L)
            }
            return
        }
        onBoardingSuccess(isMoreNeeded = false)
    }

    override fun ActivityNewPermissionBinding.initListeners() {

    }

    override fun ActivityNewPermissionBinding.initView() {
        updateStatusBarColor(coder.apps.space.library.R.color.colorBackground)
        updateNavigationBarColor(R.color.colorCardBackground)
        settingOverLay = HandleSettingPreview(this@NewPermissionActivity)
        onBackPressedDispatcher.addCallback {
            finish()
        }
    }

    private fun onBoardingSuccess(isMoreNeeded: Boolean) {
        tinyDB?.putBoolean(IS_INTRO_ENABLED, false)
        if (isMoreNeeded) {
            go(AppPermissionActivity::class.java, finish = true)
            return
        }
        eulaAccepted()
        if (tinyDB?.getBoolean(IS_LANGUAGE_ENABLED, true) == true) {
            go(AppLanguageActivity::class.java, finish = true)
        } else {
            go(DashboardActivity::class.java, finish = true)
        }
    }

    class IntroViewPagerAdapter(var context: Context, var screens: MutableList<Int> = mutableListOf(R.drawable.ic_intro_1, R.drawable.ic_intro_3, R.drawable.ic_intro_2)) : PagerAdapter() {
        override fun instantiateItem(container: ViewGroup, position: Int): Any {
            val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            val layoutScreen: View = inflater.inflate(R.layout.layout_intro_screen, null)
            val imgSlide = layoutScreen.findViewById<ShapeableImageView>(R.id.intro_view)
            Glide.with(layoutScreen.context).load(screens[position]).into(imgSlide)
            container.addView(layoutScreen)
            return layoutScreen
        }

        override fun getCount(): Int {
            return screens.size
        }

        override fun isViewFromObject(view: View, o: Any): Boolean {
            return view == o
        }

        override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
            container.removeView(`object` as View)
        }
    }

    @SuppressLint("WrongConstant")
    fun invokeSetupWizardOfThisIme() {
        settingOverLay?.cancelPollingImeSettings()
        Intent(this@NewPermissionActivity, NewPermissionActivity::class.java).apply {
            flags = 606076928
            startActivity(this)
        }
    }

    class HandleSettingPreview internal constructor(activity: NewPermissionActivity) : LeakGuardHandlerWrapper<NewPermissionActivity>(activity) {
        fun cancelPollingImeSettings() {
            removeMessages(0)
        }

        override fun handleMessage(message: Message) {
            val ownerInstance = ownerInstance
            if (ownerInstance != null && message.what == 0) {
                if (ownerInstance.permissionFlow == 1 && ownerInstance.hasOverlayPermission()) {
                    ownerInstance.invokeSetupWizardOfThisIme()
                } else {
                    startPollingImeSettings()
                }
            }
        }

        fun startPollingImeSettings() {
            sendMessageDelayed(obtainMessage(0), 100L)
        }
    }
}