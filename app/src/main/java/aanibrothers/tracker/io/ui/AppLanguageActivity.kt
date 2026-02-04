package aanibrothers.tracker.io.ui

import aanibrothers.tracker.io.adapter.LanguageAdapter
import aanibrothers.tracker.io.databinding.ActivityAppLanguageBinding
import aanibrothers.tracker.io.extension.IS_INTRO_ENABLED
import aanibrothers.tracker.io.extension.IS_LANGUAGE_ENABLED
import aanibrothers.tracker.io.extension.IS_SETTINGS
import aanibrothers.tracker.io.extension.SCREEN_PERMISSION
import aanibrothers.tracker.io.module.viewNativeMedium
import aanibrothers.tracker.io.ui.updates.AppPermissionActivity
import aanibrothers.tracker.io.ui.updates.HomeActivity
import aanibrothers.tracker.io.ui.updates.OnboardingActivity
import androidx.activity.OnBackPressedCallback
import androidx.recyclerview.widget.GridLayoutManager
import coder.apps.space.library.R
import coder.apps.space.library.base.BaseActivity
import coder.apps.space.library.extension.go
import coder.apps.space.library.extension.hasPermissions
import coder.apps.space.library.helper.currentLanguage
import java.util.Locale

class AppLanguageActivity :
    BaseActivity<ActivityAppLanguageBinding>(ActivityAppLanguageBinding::inflate) {

    private var language: String = Locale.getDefault().language
    private var isChangeLanguage: Boolean = false
    override fun ActivityAppLanguageBinding.initExtra() {
        updateNavigationBarColor(R.color.colorTransparent)
        language = currentLanguage ?: Locale.getDefault().language
        initAdapter()
        viewNativeMedium(adNative)
    }

    private fun ActivityAppLanguageBinding.initAdapter() {
        recyclerView.apply {
            layoutManager = GridLayoutManager(this@AppLanguageActivity, 1)
            adapter = LanguageAdapter(this@AppLanguageActivity) {
                language = it
                isChangeLanguage = true
            }
        }
    }

    override fun ActivityAppLanguageBinding.initListeners() {
        buttonGo.setOnClickListener {
            currentLanguage = language
            val fromSettings = intent?.getBooleanExtra(IS_SETTINGS, false)
            if (fromSettings == true) {
                go(AppSettingsActivity::class.java, finish = true)
            } else {
                tinyDB?.putBoolean(IS_LANGUAGE_ENABLED, false)
                if (tinyDB?.getBoolean(IS_INTRO_ENABLED, true) == true) {
                    go(OnboardingActivity::class.java, finish = true)
                } else if (!hasPermissions(SCREEN_PERMISSION)) {
                    go(AppPermissionActivity::class.java, finish = true)
                    return@setOnClickListener
                } else {
                    go(HomeActivity::class.java, finish = true)
                }
            }
        }
    }

    override fun ActivityAppLanguageBinding.initView() {
        updateStatusBarColor(R.color.colorPrimary)
        onBackPressedDispatcher.addCallback(
            this@AppLanguageActivity,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    val fromSettings = intent?.getBooleanExtra(IS_SETTINGS, false)
                    if (fromSettings == true) {
                        go(AppSettingsActivity::class.java, finish = true)
                    } else {
                        finish()
                    }
                }
            })
    }
}