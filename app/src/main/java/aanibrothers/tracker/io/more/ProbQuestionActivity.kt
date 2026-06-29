package aanibrothers.tracker.io.more

import aanibrothers.tracker.io.analytics.Analytics
import aanibrothers.tracker.io.analytics.AnalyticsEvent
import aanibrothers.tracker.io.databinding.ActivityProbQuestionBinding
import aanibrothers.tracker.io.extension.IS_LANGUAGE_ENABLED
import aanibrothers.tracker.io.extension.hasAllNewPermissions
import aanibrothers.tracker.io.extension.isLocationEnabled
import aanibrothers.tracker.io.extension.isOnboardingEnabled
import aanibrothers.tracker.io.module.viewNativeMedium
import aanibrothers.tracker.io.ui.AppLanguageActivity
import aanibrothers.tracker.io.ui.updates.HomeActivity
import aanibrothers.tracker.io.ui.updates.OnboardingActivity
import aanibrothers.tracker.io.ui.updates.PermissionActivity
import android.app.LocaleManager
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import coder.apps.space.library.extension.go
import coder.apps.space.library.helper.TinyDB
import kotlin.jvm.java

class ProbQuestionActivity : AppCompatActivity() {
    lateinit var binding: ActivityProbQuestionBinding
    private var selectedReason: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityProbQuestionBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        initView()
        reloadAd()
        initListener()
    }

    private fun initView() {
        binding.apply {
            rvOptions.layoutManager = LinearLayoutManager(this@ProbQuestionActivity)
            val arrayOf = arrayOf(
                "App is not responsive",
                "Not as feature rich as expected",
                "Too many ads",
                "I found another app that is more suitable",
                "Interface is difficult to use"
            )
            btnUninstall.isSelected = false
            btnUninstall.isEnabled = false
            rvOptions.adapter = ProbAdapter(
                this@ProbQuestionActivity,
                arrayOf,
                object : ProbAdapter.OnItemClickListener {
                    override fun onItemClicked(reason: String) {
                        selectedReason = reason
                        Analytics.log(AnalyticsEvent.UninstallReasonSelected(reason = reason))
                        btnUninstall.isSelected = true
                        btnUninstall.isEnabled = true
                        reloadAd()
                    }
                })
        }
    }

    private fun initListener() {
        binding.apply {
            btnUninstall.setOnClickListener {
                Analytics.log(
                    AnalyticsEvent.UninstallFlowAction(
                        screen = "reasons", button = "uninstall", action = "uninstall"
                    )
                )
                Analytics.log(AnalyticsEvent.UninstallConfirmed)
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = "package:${packageName}".toUri()
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivity(intent)
                finish()
            }
            btnBack.setOnClickListener {
                Analytics.log(
                    AnalyticsEvent.UninstallFlowAction(
                        screen = "reasons", button = "back", action = "keep_app"
                    )
                )
                goToHome()
            }
            btnCancel.setOnClickListener {
                Analytics.log(
                    AnalyticsEvent.UninstallFlowAction(
                        screen = "reasons", button = "cancel", action = "keep_app"
                    )
                )
                goToHome()
            }
            btnHome.setOnClickListener {
                Analytics.log(
                    AnalyticsEvent.UninstallFlowAction(
                        screen = "reasons", button = "home", action = "keep_app"
                    )
                )
                goToHome()
            }
        }
    }

    fun goToHome() {
        when {
            TinyDB(this).getBoolean(IS_LANGUAGE_ENABLED, true) ->
                go(AppLanguageActivity::class.java, finish = true)
            isOnboardingEnabled() ->
                go(OnboardingActivity::class.java, finish = true)
            !hasAllNewPermissions() || !isLocationEnabled() ->
                go(PermissionActivity::class.java, finish = true)
            else ->
                go(HomeActivity::class.java, finish = true)
        }
    }


    fun reloadAd() {
        viewNativeMedium(binding.nativeAdView)
    }
}