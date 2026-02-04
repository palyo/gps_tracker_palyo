package aanibrothers.tracker.io.ui.updates

import aanibrothers.tracker.io.R
import aanibrothers.tracker.io.databinding.ActivityOnboardingBinding
import aanibrothers.tracker.io.extension.IS_INTRO_ENABLED
import aanibrothers.tracker.io.extension.SCREEN_PERMISSION
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager.widget.ViewPager.OnPageChangeListener
import coder.apps.space.library.base.BaseActivity
import coder.apps.space.library.extension.go
import coder.apps.space.library.extension.hasPermissions
import com.bumptech.glide.Glide
import com.google.android.material.imageview.ShapeableImageView

class OnboardingActivity :
    BaseActivity<ActivityOnboardingBinding>(ActivityOnboardingBinding::inflate) {

    private var introViewPagerAdapter: IntroViewPagerAdapter? = null
    private var screens: MutableList<Int> = mutableListOf(
        R.drawable.ic_onboarding1,
        R.drawable.ic_onboarding2,
        R.drawable.ic_onboarding3
    )

    override fun ActivityOnboardingBinding.initExtra() {
        setupPager()
    }

    private fun ActivityOnboardingBinding.setupPager() {
        introViewPagerAdapter = IntroViewPagerAdapter(this@OnboardingActivity)
        viewPager.adapter = introViewPagerAdapter
        buttonNext.setOnClickListener {
            if (viewPager.currentItem < screens.size - 1) {
                viewPager.currentItem++
                updateTabs()
            } else {
                tinyDB?.putBoolean(IS_INTRO_ENABLED, false)
                if (!hasPermissions(SCREEN_PERMISSION)) {
                    go(AppPermissionActivity::class.java, finish = true)
                    return@setOnClickListener
                }
                go(HomeActivity::class.java, finish = true)

            }
        }

        viewPager.addOnPageChangeListener(object : OnPageChangeListener {
            override fun onPageScrolled(
                position: Int,
                positionOffset: Float,
                positionOffsetPixels: Int
            ) {

            }

            override fun onPageSelected(position: Int) {
                when (position) {
                    0 -> {
                        buttonNext.text = getString(R.string.button_next)
                        textTitle.text = getString(R.string.title_onboarding1)
                        textBody.text = getString(R.string.body_onboarding1)
                    }

                    1 -> {
                        buttonNext.text = getString(R.string.button_next)
                        textTitle.text = getString(R.string.title_onboarding2)
                        textBody.text = getString(R.string.body_onboarding2)
                    }

                    2 -> {
                        buttonNext.text = getString(R.string.button_allow_and_continue)
                        textTitle.text = getString(R.string.title_onboarding3)
                        textBody.text = getString(R.string.body_onboarding3)
                    }
                }

                updateTabs()
            }

            override fun onPageScrollStateChanged(state: Int) {

            }
        })
        updateTabs()
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

    }

    class IntroViewPagerAdapter(
        var context: Context,
        var screens: MutableList<Int> = mutableListOf(
            R.drawable.ic_onboarding1,
            R.drawable.ic_onboarding2,
            R.drawable.ic_onboarding3
        )
    ) : PagerAdapter() {
        override fun instantiateItem(container: ViewGroup, position: Int): Any {
            val inflater =
                context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
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
}