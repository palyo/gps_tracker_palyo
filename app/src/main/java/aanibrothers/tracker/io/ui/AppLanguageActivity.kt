package aanibrothers.tracker.io.ui

import aanibrothers.tracker.io.adapter.*
import aanibrothers.tracker.io.databinding.*
import aanibrothers.tracker.io.extension.*
import aanibrothers.tracker.io.module.*
import android.*
import androidx.activity.*
import androidx.recyclerview.widget.*
import coder.apps.space.library.R
import coder.apps.space.library.base.*
import coder.apps.space.library.extension.*
import coder.apps.space.library.helper.*
import java.util.*

class AppLanguageActivity : BaseActivity<ActivityAppLanguageBinding>(ActivityAppLanguageBinding::inflate) {

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
                go(DashboardActivity::class.java, finish = true)
            }
        }
    }

    override fun ActivityAppLanguageBinding.initView() {
        updateStatusBarColor(R.color.colorPrimary)
        onBackPressedDispatcher.addCallback(this@AppLanguageActivity, object : OnBackPressedCallback(true) {
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