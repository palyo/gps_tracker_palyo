package aanibrothers.tracker.io.ui.updates

import aanibrothers.tracker.io.App
import aanibrothers.tracker.io.databinding.ActivityAppPermissionBinding
import aanibrothers.tracker.io.extension.isGrantedOverlay
import aanibrothers.tracker.io.helper.HandleSettingPreview
import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.edit
import coder.apps.space.library.base.BaseActivity
import coder.apps.space.library.extension.beGone
import coder.apps.space.library.extension.go
import coder.apps.space.library.extension.hasPermission
import coder.apps.space.library.extension.hasPermissions

class AppPermissionActivity :
    BaseActivity<ActivityAppPermissionBinding>(ActivityAppPermissionBinding::inflate) {

    private var handlerSettingOverLay: HandleSettingPreview? = null
    private val maxDeniedCount = 1

    private val phonePermissions = arrayOf(Manifest.permission.READ_PHONE_STATE)
    private val notificationPermissions =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            emptyArray()
        }

    private val allPermissions by lazy {
        buildList {
            add(Manifest.permission.READ_PHONE_STATE)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }.toTypedArray()
    }

    private val permissionKeyMap = mapOf(
        Manifest.permission.READ_PHONE_STATE to "phone_state",
        Manifest.permission.POST_NOTIFICATIONS to "post_notifications"
    )

    private val requestPermissionsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            updateSwitchStates()

            result.forEach { (perm, granted) ->
                if (!granted) {
                    val key = permissionKeyMap[perm]
                    if (key != null) incrementDeniedCount(key)
                }
            }

            if (allRuntimeGranted() && !isGrantedOverlay()) {
                openOverlaySettings()
                return@registerForActivityResult
            }

            if (allGranted()) {
                go(HomeActivity::class.java)
            }
        }

    override fun ActivityAppPermissionBinding.initView() {
        updateSwitchStates()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            cardNotificationPermission.beGone()
        }
    }

    override fun ActivityAppPermissionBinding.initExtra() {
        handlerSettingOverLay = HandleSettingPreview(this@AppPermissionActivity)
    }

    override fun ActivityAppPermissionBinding.initListeners() {
        isPhoneAccess.setOnClickListener {
            if (hasPermissions(phonePermissions)) {
                isPhoneAccess.isChecked = true
                return@setOnClickListener
            }
            requestWithLimit("phone_state", phonePermissions)
        }

        isNotificationAccess.setOnClickListener {
            if (notificationPermissions.isEmpty()) {
                isNotificationAccess.isChecked = true
                return@setOnClickListener
            }
            if (hasPermissions(notificationPermissions)) {
                isNotificationAccess.isChecked = true
                return@setOnClickListener
            }
            requestWithLimit("post_notifications", notificationPermissions)
        }

        isOverlayAccess.setOnClickListener {
            if (isGrantedOverlay()) {
                isOverlayAccess.isChecked = true
                return@setOnClickListener
            }
            openOverlaySettings()
        }

        buttonContinue.setOnClickListener {
            when {
                !allRuntimeGranted() -> requestWithLimit("all", allPermissions)
                !isGrantedOverlay() -> openOverlaySettings()
                else -> go(HomeActivity::class.java)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updateSwitchStates()
        if (allGranted()) {
            go(HomeActivity::class.java)
        }
    }

    private fun updateSwitchStates() {
        binding?.apply {
            isPhoneAccess.isChecked = hasPermissions(phonePermissions)
            isNotificationAccess.isChecked =
                notificationPermissions.isEmpty() || hasPermissions(notificationPermissions)
            isOverlayAccess.isChecked = isGrantedOverlay()
        }
    }

    private fun allRuntimeGranted(): Boolean {
        return allPermissions.all { hasPermission(it) }
    }

    private fun allGranted(): Boolean {
        return allRuntimeGranted() && isGrantedOverlay()
    }

    private fun requestWithLimit(key: String, permissions: Array<String>) {
        if (permissions.isEmpty()) return

        val deniedCount = getDeniedCount(key)
        if (deniedCount <= maxDeniedCount) {
            requestPermissionsLauncher.launch(permissions)
        } else {
            openAppSettings()
        }
    }

    private fun openOverlaySettings() {
        val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
            data = Uri.fromParts("package", packageName, null)
        }
        handlerSettingOverLay?.startPollingImeSettings()
        App.isOpenInter = true
        startActivity(intent)
    }

    private fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
        }
        startActivity(intent)
    }

    private fun incrementDeniedCount(key: String) {
        val count = getDeniedCount(key)
        getSharedPreferences("permission_denials", MODE_PRIVATE)
            .edit {
                putInt(key, count + 1)
            }
    }

    private fun getDeniedCount(key: String): Int {
        return getSharedPreferences("permission_denials", MODE_PRIVATE)
            .getInt(key, 0)
    }



    fun invokeSetupWizardOfThisIme() {
        handlerSettingOverLay?.cancelPollingImeSettings()
        val intent = Intent()
        intent.setClass(this@AppPermissionActivity, HomeActivity::class.java)
        intent.flags = 606076928
        startActivity(intent)
    }
}
