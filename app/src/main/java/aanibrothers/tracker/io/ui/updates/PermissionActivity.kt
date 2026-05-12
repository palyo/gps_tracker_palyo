package aanibrothers.tracker.io.ui.updates

import aanibrothers.tracker.io.App
import aanibrothers.tracker.io.databinding.ActivityPermissionBinding
import aanibrothers.tracker.io.databinding.LayoutDialogPermissionSettingsBinding
import aanibrothers.tracker.io.extension.CAMERA_PERMISSION
import aanibrothers.tracker.io.extension.LOCATION_PERMISSION
import aanibrothers.tracker.io.extension.STORAGE_PERMISSION
import aanibrothers.tracker.io.extension.hasAllNewPermissions
import aanibrothers.tracker.io.extension.hasCameraPermissions
import aanibrothers.tracker.io.extension.hasLocationPermissions
import aanibrothers.tracker.io.extension.hasStoragePermissions
import aanibrothers.tracker.io.module.getPolicyLink
import aanibrothers.tracker.io.module.viewInterAd
import aanibrothers.tracker.io.module.viewNativeMedium
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.edit
import coder.apps.space.library.base.BaseActivity
import coder.apps.space.library.extension.beGone
import coder.apps.space.library.extension.beVisible
import coder.apps.space.library.extension.go
import coder.apps.space.library.extension.launchUrl
import com.google.android.material.bottomsheet.BottomSheetDialog

class PermissionActivity :
    BaseActivity<ActivityPermissionBinding>(ActivityPermissionBinding::inflate) {

    private val maxDeniedCount = 2
    private var settingsDialog: BottomSheetDialog? = null

    private val permissionKeyMap = mapOf(
        "storage" to STORAGE_PERMISSION,
        "location" to LOCATION_PERMISSION,
        "camera" to CAMERA_PERMISSION
    )

    private val requestPermissionsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            result.forEach { (perm, granted) ->
                if (!granted) {
                    val key = permissionKeyMap.entries.firstOrNull { it.value.contains(perm) }?.key
                    if (key != null) incrementDeniedCount(key)
                }
            }
            refreshUi()
            if (hasAllNewPermissions()) {
                binding?.btnAllowPermission?.performClick()
                return@registerForActivityResult
            }
            if (result.values.any { it }) {
                requestNextMissing()
            }
        }

    override fun ActivityPermissionBinding.initView() {
        refreshUi()
        viewNativeMedium(adNative)
    }

    override fun ActivityPermissionBinding.initExtra() {

    }

    override fun ActivityPermissionBinding.initListeners() {
        clPermissionStorage.setOnClickListener {
            if (hasStoragePermissions()) {
                imgPermission3.isSelected = true
                return@setOnClickListener
            }
            requestWithLimit("storage", STORAGE_PERMISSION)
        }

        clPermissionLocation.setOnClickListener {
            if (hasLocationPermissions()) {
                imgPermission4.isSelected = true
                return@setOnClickListener
            }
            requestWithLimit("location",LOCATION_PERMISSION)
        }

        clPermissionCamera.setOnClickListener {
            if (hasCameraPermissions()) {
                imgPermission2.isSelected = true
                return@setOnClickListener
            }
            requestWithLimit("camera", CAMERA_PERMISSION)
        }

        btnAllowPermission.setOnClickListener {
            if (hasAllNewPermissions()) {
                viewInterAd {
                    go(HomeActivity::class.java, finish = true)
                }
            }
        }

        tvPrivacyPolicy.setOnClickListener {
            App.isOpenInter = true
            launchUrl(getPolicyLink())
        }
    }

    override fun onResume() {
        super.onResume()
        refreshUi()
    }

    override fun onDestroy() {
        super.onDestroy()
        settingsDialog?.dismiss()
        settingsDialog = null
    }

    private fun refreshUi() {
        binding?.apply {
            val storageGranted = hasStoragePermissions()
            val locationGranted = hasLocationPermissions()
            val cameraGranted = hasCameraPermissions()

            imgPermission3.isActivated = storageGranted
            imgPermission4.isActivated = locationGranted
            imgPermission2.isActivated = cameraGranted

            if (storageGranted || locationGranted || cameraGranted) {
                lottieView.beGone()
            } else {
                lottieView.beVisible()
            }

            if (storageGranted && locationGranted && cameraGranted) {
                btnAllowPermission.beVisible()
            } else {
                btnAllowPermission.beGone()
            }
        }
    }

    private fun requestWithLimit(key: String, permissions: Array<String>) {
        if (permissions.isEmpty()) {
            refreshUi()
            return
        }

        val deniedCount = getDeniedCount(key)
        if (deniedCount <= maxDeniedCount) {
            requestPermissionsLauncher.launch(permissions)
        } else {
            showSettingsDialog()
        }
    }

    private fun requestNextMissing() {
        val (key, perms) = when {
            !hasStoragePermissions() -> "storage" to STORAGE_PERMISSION
            !hasLocationPermissions() -> "location" to LOCATION_PERMISSION
            !hasCameraPermissions() -> "camera" to CAMERA_PERMISSION
            else -> return
        }
        if (perms.isEmpty()) return
        if (getDeniedCount(key) <= maxDeniedCount) {
            requestPermissionsLauncher.launch(perms)
        }
    }

    private fun showSettingsDialog() {
        if (isFinishing || settingsDialog?.isShowing == true) return

        val dialog = BottomSheetDialog(
            this,
            coder.apps.space.library.R.style.Theme_Space_BottomSheetDialogTheme
        )
        val sheetBinding = LayoutDialogPermissionSettingsBinding.inflate(layoutInflater)
        dialog.setContentView(sheetBinding.root)

        sheetBinding.btnOpenSettings.setOnClickListener {
            dialog.dismiss()
            openAppSettings()
        }

        dialog.setOnDismissListener {
            if (settingsDialog === dialog) {
                settingsDialog = null
            }
        }

        settingsDialog = dialog
        dialog.show()
    }

    private fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
        }
        startActivity(intent)
    }

    private fun incrementDeniedCount(key: String) {
        val count = getDeniedCount(key)
        getSharedPreferences("permission_denials_new", MODE_PRIVATE)
            .edit {
                putInt(key, count + 1)
            }
    }

    private fun getDeniedCount(key: String): Int {
        return getSharedPreferences("permission_denials_new", MODE_PRIVATE)
            .getInt(key, 0)
    }
}
