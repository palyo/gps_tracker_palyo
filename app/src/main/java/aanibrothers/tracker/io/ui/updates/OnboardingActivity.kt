package aanibrothers.tracker.io.ui.updates

import aanibrothers.tracker.io.R
import aanibrothers.tracker.io.databinding.ActivityOnboardingBinding
import aanibrothers.tracker.io.extension.IS_INTRO_ENABLED
import aanibrothers.tracker.io.extension.hasAllNewPermissions
import aanibrothers.tracker.io.extension.hasRequiredAppPermissions
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.viewpager.widget.ViewPager.OnPageChangeListener
import coder.apps.space.library.base.BaseActivity
import coder.apps.space.library.extension.go
import com.ironsource.fa

class OnboardingActivity :
    BaseActivity<ActivityOnboardingBinding>(ActivityOnboardingBinding::inflate,
        isFullScreen = true,
        isFullScreenIncludeNav = false) {

    private val pageCount = 3

    override fun ActivityOnboardingBinding.initExtra() {
        setupPager()
    }

    private fun ActivityOnboardingBinding.setupPager() {
        viewPager.adapter = OnboardingPagerAdapter(supportFragmentManager)

        viewPager.addOnPageChangeListener(object : OnPageChangeListener {
            override fun onPageScrolled(
                position: Int,
                positionOffset: Float,
                positionOffsetPixels: Int
            ) {
            }

            override fun onPageSelected(position: Int) {
                updateTabs()
            }

            override fun onPageScrollStateChanged(state: Int) {
            }
        })
        updateTabs()
    }

    fun goToNext() {
        val viewPager = binding?.viewPager
        if ((viewPager?.currentItem?:0) < pageCount - 1) {
            viewPager?.currentItem = (viewPager.currentItem?:0) + 1
            return
        }
        tinyDB?.putBoolean(IS_INTRO_ENABLED, false)
        if (!hasAllNewPermissions()) {
            go(PermissionActivity::class.java, finish = true)
            return
        }
        go(HomeActivity::class.java, finish = true)
    }

    private fun ActivityOnboardingBinding.updateTabs() {
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

    override fun ActivityOnboardingBinding.initListeners() {

    }

    override fun ActivityOnboardingBinding.initView() {
        updateStatusBarColor(R.color.colorTransparent)
        updateNavigationBarColor(R.color.colorBlack)
    }

    class OnboardingPagerAdapter(fm: FragmentManager) :
        FragmentStatePagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

        override fun getCount(): Int = 3

        override fun getItem(position: Int): Fragment = when (position) {
            0 -> FirstFragment()
            1 -> SecondFragment()
            else -> ThirdFragment()
        }
    }
}
