package aanibrothers.tracker.io.ui

import aanibrothers.tracker.io.App
import aanibrothers.tracker.io.databinding.ActivitySettingsBinding
import aanibrothers.tracker.io.extension.IS_SETTINGS
import aanibrothers.tracker.io.module.ConsentManager
import aanibrothers.tracker.io.module.getPolicyLink
import aanibrothers.tracker.io.ui.updates.HomeActivity
import android.content.Intent
import androidx.activity.addCallback
import coder.apps.space.library.base.BaseActivity
import coder.apps.space.library.extension.beVisibleIf
import coder.apps.space.library.extension.go
import coder.apps.space.library.extension.launchUrl

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

        switchCall.setOnCheckedChangeListener { button, isChecked ->
            if (button.isPressed) {
                tinyDB?.getBoolean("ShowAfterCall", isChecked)
            }
        }
    }

    override fun ActivitySettingsBinding.initView() {
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
        onBackPressedDispatcher.addCallback { go(HomeActivity::class.java, finishAll = true) }
    }

    override fun onResume() {
        super.onResume()
        binding?.switchCall?.isChecked = tinyDB?.getBoolean("ShowAfterCall", true) == true

    }
}