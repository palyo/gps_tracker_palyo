package aanibrothers.tracker.io.ui

import aanibrothers.tracker.io.*
import aanibrothers.tracker.io.databinding.*
import aanibrothers.tracker.io.extension.*
import aanibrothers.tracker.io.module.*
import android.content.*
import androidx.activity.*
import coder.apps.space.library.base.*
import coder.apps.space.library.extension.*
import com.calldorado.ui.settings.*

class AppSettingsActivity : BaseActivity<ActivitySettingsBinding>(ActivitySettingsBinding::inflate) {
    override fun ActivitySettingsBinding.initExtra() {}

    override fun ActivitySettingsBinding.initListeners() {
        buttonLanguage.setOnClickListener { go(AppLanguageActivity::class.java, listOf(IS_SETTINGS to true), finish = true) }
        buttonShare.setOnClickListener {
            val app = getString(aanibrothers.tracker.io.R.string.app_name)
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(
                    "android.intent.extra.TEXT", "$app\n\nOpen this Link on Play Store\n\nhttps://play.google.com/store/apps/details?id=${packageName}"
                )
                startActivity(Intent.createChooser(this, "Share Application"))
            }
        }
        buttonPrivacy.setOnClickListener {
            App.isOpenInter = true
            launchUrl(getPolicyLink())
        }
        val consentManager = ConsentManager.getInstance(this@AppSettingsActivity)
        consentManager.isPrivacyOptionsRequired.let {
            buttonManageConsent.beVisibleIf(it)
        }

        buttonManageConsent.setOnClickListener {
            consentManager.showPrivacyOptionsForm(this@AppSettingsActivity) {}
        }

        buttonAboutCallerId.setOnClickListener {
            go(SettingsActivity::class.java)
        }
    }

    override fun ActivitySettingsBinding.initView() {
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
        onBackPressedDispatcher.addCallback { go(DashboardActivity::class.java, finishAll = true) }
    }
}