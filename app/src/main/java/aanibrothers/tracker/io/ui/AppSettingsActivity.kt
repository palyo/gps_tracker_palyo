package aanibrothers.tracker.io.ui

import aanibrothers.tracker.io.App
import aanibrothers.tracker.io.R
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
            val app = getString(R.string.app_name)
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(
                    Intent.EXTRA_TEXT,
                    getString(R.string.message_share_app, app, packageName)
                )
                startActivity(Intent.createChooser(this, getString(R.string.chooser_share_app)))
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
