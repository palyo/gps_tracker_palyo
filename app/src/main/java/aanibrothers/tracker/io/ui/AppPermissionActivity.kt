package aanibrothers.tracker.io.ui

import aanibrothers.tracker.io.cdo.*
import aanibrothers.tracker.io.databinding.*
import aanibrothers.tracker.io.extension.*
import android.*
import android.annotation.*
import android.content.*
import android.net.*
import android.os.*
import android.provider.*
import androidx.activity.result.*
import androidx.activity.result.contract.*
import coder.apps.space.library.base.*
import coder.apps.space.library.extension.*
import coder.apps.space.library.helper.*

class AppPermissionActivity : BaseActivity<ActivityAppPermissionBinding>(ActivityAppPermissionBinding::inflate) {
    private val maxDeniedCount = 2
    var permissionFlow = 0
    private var isContinues = false
    private var settingOverLay: HandleSettingPreview? = null
    private val permissions = arrayOf(Manifest.permission.READ_PHONE_STATE)
    private var displayOverLauncher: ActivityResultLauncher<Intent> = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        binding?.updateUI()
        if (!isContinues) {
            checkNGo()
            return@registerForActivityResult
        }
        if (hasOverlayPermission()) {
            settingOverLay?.cancelPollingImeSettings()
            initPermissions()
        } else {
            settingOverLay?.cancelPollingImeSettings()
        }
    }
    private val appSettingsLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        binding?.updateUI()
        if (!isContinues) {
            checkNGo()
            return@registerForActivityResult
        }
        settingOverLay?.cancelPollingImeSettings()
        initPermissions()
    }
    private val phoneStateLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        binding?.updateUI()
        if (permissions.containsValue(false)) {
            incrementPermissionsDeniedCount("phone_state")
            if (isContinues) {
                if (!hasPermissions(this.permissions)) {
                    viewPermissions {
                        val phoneDeniedCount = getPermissionsDeniedCount("phone_state")
                        if (phoneDeniedCount < maxDeniedCount && !hasPermissions(arrayOf(Manifest.permission.READ_PHONE_STATE))) {
                            requestPhoneState()
                            return@viewPermissions
                        }
                        val appSettingsIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", packageName, null)
                        }
                        appSettingsLauncher.launch(appSettingsIntent)
                    }
                    return@registerForActivityResult
                } else {
                    initPermissions()
                }
            }
        } else {
            if (!isContinues) {
                checkNGo()
                return@registerForActivityResult
            }
            if (hasPermissions(arrayOf(Manifest.permission.READ_PHONE_STATE))) {
                initPermissions()
            }
        }
    }

    private fun requestPhoneState() {
        phoneStateLauncher.launch(arrayOf(Manifest.permission.READ_PHONE_STATE))
    }

    override fun ActivityAppPermissionBinding.initView() {
        updateStatusBarColor(coder.apps.space.library.R.color.colorPrimary)
        updateNavigationBarColor(coder.apps.space.library.R.color.colorPrimary)
        settingOverLay = HandleSettingPreview(this@AppPermissionActivity)
    }

    override fun onResume() {
        super.onResume()
        binding?.updateUI()
    }

    fun ActivityAppPermissionBinding.updateUI() {
        isPhoneState.isChecked = hasPermissions(arrayOf(Manifest.permission.READ_PHONE_STATE))
        isOverlay.isChecked = hasOverlayPermission()

    }

    override fun ActivityAppPermissionBinding.initListeners() {
        buttonContinue.setOnClickListener {
            isContinues = true
            initPermissions()
        }

        if (hasPermissions(arrayOf(Manifest.permission.READ_PHONE_STATE))) {
            isPhoneState.beGone()
            textPhoneState.beGone()
            bodyPhoneState.beGone()
        }

        if (hasOverlayPermission()) {
            isOverlay.beGone()
            textOverlay.beGone()
            bodyOverlay.beGone()
        }

        isPhoneState.setOnClickListener {
            isContinues = false
            val phoneDeniedCount = getPermissionsDeniedCount("phone_state")
            if (hasPermissions(arrayOf(Manifest.permission.READ_PHONE_STATE))) {
                isPhoneState.isChecked = true
                return@setOnClickListener
            }
            if (phoneDeniedCount < maxDeniedCount && !hasPermissions(arrayOf(Manifest.permission.READ_PHONE_STATE))) {
                requestPhoneState()
                return@setOnClickListener
            }

            if (!hasPermissions(arrayOf(Manifest.permission.READ_PHONE_STATE))) {
                viewPermissions {
                    appSettingsLauncher.launch(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", packageName, null)
                    })
                }
                return@setOnClickListener
            }
        }

        isOverlay.setOnClickListener {
            isContinues = false
            if (hasOverlayPermission()) {
                isOverlay.isChecked = true
                return@setOnClickListener
            }
            if (!hasOverlayPermission()) {
                permissionFlow = 1
                Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
                    displayOverLauncher.launch(this)
                    settingOverLay?.startPollingImeSettings()
                    Handler(mainLooper).postDelayed({
                        startActivity(Intent(this@AppPermissionActivity, PromptActivity::class.java))
                    }, 500L)
                }
                return@setOnClickListener
            }
        }

    }

    override fun ActivityAppPermissionBinding.initExtra() {}

    private fun initPermissions() {
        val phoneDeniedCount = getPermissionsDeniedCount("phone_state")
        if (phoneDeniedCount < maxDeniedCount && !hasPermissions(arrayOf(Manifest.permission.READ_PHONE_STATE))) {
            requestPhoneState()
            return
        }

        if (!hasPermissions(permissions)) {
            viewPermissions {
                val appSettingsIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", packageName, null)
                }
                appSettingsLauncher.launch(appSettingsIntent)
            }
            return
        }

        if (!hasOverlayPermission()) {
            permissionFlow = 1
            Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
                settingOverLay?.startPollingImeSettings()
                displayOverLauncher.launch(this)
                Handler(mainLooper).postDelayed({
                    startActivity(Intent(this@AppPermissionActivity, PromptActivity::class.java))
                }, 500L)
            }
            return
        }

        eulaAccepted()
        if (tinyDB?.getBoolean(IS_LANGUAGE_ENABLED, true) == true) {
            go(AppLanguageActivity::class.java, finish = true)
        } else {
            go(DashboardActivity::class.java, finish = true)
        }
    }

    private fun checkNGo() {
        if (hasPermissions(permissions) && hasOverlayPermission()) {
            eulaAccepted()
            if (tinyDB?.getBoolean(IS_LANGUAGE_ENABLED, true) == true) {
                go(AppLanguageActivity::class.java, finish = true)
            } else {
                go(DashboardActivity::class.java, finish = true)
            }
        }
    }

    @SuppressLint("WrongConstant")
    fun invokeSetupWizardOfThisIme() {
        settingOverLay?.cancelPollingImeSettings()
        Intent(this@AppPermissionActivity, AppPermissionActivity::class.java).apply {
            flags = 606076928
            startActivity(this)
        }
    }

    class HandleSettingPreview internal constructor(activity: AppPermissionActivity) : LeakGuardHandlerWrapper<AppPermissionActivity>(activity) {
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
            sendMessageDelayed(obtainMessage(0), 200L)
        }
    }
}