package aanibrothers.tracker.io.more

import aanibrothers.tracker.io.databinding.ActivityProbBinding
import aanibrothers.tracker.io.extension.IS_INTRO_ENABLED
import aanibrothers.tracker.io.extension.IS_LANGUAGE_ENABLED
import aanibrothers.tracker.io.extension.hasAllNewPermissions
import aanibrothers.tracker.io.extension.isLocationEnabled
import aanibrothers.tracker.io.module.viewNativeSmall
import aanibrothers.tracker.io.ui.AppLanguageActivity
import aanibrothers.tracker.io.ui.updates.HomeActivity
import aanibrothers.tracker.io.ui.updates.OnboardingActivity
import aanibrothers.tracker.io.ui.updates.PermissionActivity
import android.app.LocaleManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import coder.apps.space.library.extension.go
import coder.apps.space.library.helper.TinyDB
import kotlin.jvm.java

class ProbActivity : AppCompatActivity() {
    lateinit var binding: ActivityProbBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityProbBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applyBarInsets()
        initListener()
        loadAd()

    }

    /** Pad for the status/navigation bars (edge-to-edge on Android 15/16). */
    private fun applyBarInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
    }

    private fun initListener() {
        binding.apply {
            tvSecondaryAction.setOnClickListener {
                Intent(this@ProbActivity, ProbQuestionActivity::class.java).apply {
                    startActivity(this)
                    finish()
                }
            }
            btnAction1.setOnClickListener {
                goToHome()
            }
            btnAction2.setOnClickListener {
                goToHome()
            }
            btnPrimaryAction.setOnClickListener {
                goToHome()
            }
            btnBack.setOnClickListener {
                goToHome()
            }
        }


    }

    fun goToHome() {
        when {
            TinyDB(this).getBoolean(IS_LANGUAGE_ENABLED, true) ->
                go(AppLanguageActivity::class.java, finish = true)
            TinyDB(this).getBoolean(IS_INTRO_ENABLED, true) ->
                go(OnboardingActivity::class.java, finish = true)
            !hasAllNewPermissions() || !isLocationEnabled() ->
                go(PermissionActivity::class.java, finish = true)
            else ->
                go(HomeActivity::class.java, finish = true)
        }
    }

    fun loadAd() {
        viewNativeSmall(binding.nativeAdView)
    }

}