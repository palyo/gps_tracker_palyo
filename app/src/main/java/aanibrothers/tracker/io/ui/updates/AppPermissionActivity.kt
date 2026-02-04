package aanibrothers.tracker.io.ui.updates

import aanibrothers.tracker.io.databinding.ActivityAppPermissionBinding
import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
import coder.apps.space.library.base.BaseActivity
import coder.apps.space.library.extension.beGone
import coder.apps.space.library.extension.go
import coder.apps.space.library.extension.hasPermission
import coder.apps.space.library.extension.hasPermissions
import androidx.core.content.edit

class AppPermissionActivity :
    BaseActivity<ActivityAppPermissionBinding>(ActivityAppPermissionBinding::inflate) {

    private val maxDeniedCount = 1

    private val allPermissions by lazy {
        arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            *storagePermissions()
        )
    }

    private fun storagePermissions(): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            emptyArray()
        } else {
            arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
    }

    private val cameraPermissions = arrayOf(Manifest.permission.CAMERA)

    private val locationPermissions = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )

    private val permissionKeyMap = mapOf(
        Manifest.permission.CAMERA to "camera",
        Manifest.permission.ACCESS_FINE_LOCATION to "location",
        Manifest.permission.ACCESS_COARSE_LOCATION to "location",
        Manifest.permission.WRITE_EXTERNAL_STORAGE to "storage"
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

            if (allGranted()) {
                go(HomeActivity::class.java)
            }
        }

    override fun ActivityAppPermissionBinding.initView() {
        updateSwitchStates()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            cardStoragePermission.beGone()
        }
    }

    override fun ActivityAppPermissionBinding.initExtra() {}

    override fun ActivityAppPermissionBinding.initListeners() {
        isCameraAccess.setOnClickListener {
            if (hasPermissions(cameraPermissions)) {
                isCameraAccess.isChecked = true
                return@setOnClickListener
            }
            requestWithLimit("camera", cameraPermissions)
        }

        isLocationAccess.setOnClickListener {
            if (hasPermissions(locationPermissions)) {
                isLocationAccess.isChecked = true
                return@setOnClickListener
            }
            requestWithLimit("location", locationPermissions)
        }

        isStorageAccess.setOnClickListener {
            if (storagePermissions().isEmpty()) {
                isStorageAccess.isChecked = true
                return@setOnClickListener
            }
            requestWithLimit("storage", storagePermissions())
        }

        buttonContinue.setOnClickListener {
            if (!allGranted()) {
                requestWithLimit("all", allPermissions)
            } else {
                go(HomeActivity::class.java)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updateSwitchStates()
    }

    private fun updateSwitchStates() {
        binding?.apply {
            isCameraAccess.isChecked = hasPermissions(cameraPermissions)
            isLocationAccess.isChecked = hasPermissions(locationPermissions)
            isStorageAccess.isChecked =
                storagePermissions().isEmpty() || hasPermissions(storagePermissions())
        }
    }

    private fun allGranted(): Boolean {
        return allPermissions.all { hasPermission(it) }
    }

    private fun requestWithLimit(key: String, permissions: Array<String>) {
        val deniedCount = getDeniedCount(key)
        if (deniedCount <= maxDeniedCount) {
            requestPermissionsLauncher.launch(permissions)
        } else {
            openAppSettings()
        }
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
}
