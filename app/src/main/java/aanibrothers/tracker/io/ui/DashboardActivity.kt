package aanibrothers.tracker.io.ui

import aanibrothers.tracker.io.App
import aanibrothers.tracker.io.App.Companion.appOpenManager
import aanibrothers.tracker.io.databinding.ActivityDashboardBinding
import aanibrothers.tracker.io.extension.LOCATION_PERMISSION
import aanibrothers.tracker.io.extension.PERMISSION_MAIN
import aanibrothers.tracker.io.extension.isGrantedOverlay
import aanibrothers.tracker.io.extension.isLocationEnabled
import aanibrothers.tracker.io.module.AppOpenManager
import aanibrothers.tracker.io.module.viewBanner
import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.IntentSender
import android.net.Uri
import android.os.Message
import android.provider.Settings
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import coder.apps.space.library.base.BaseActivity
import coder.apps.space.library.extension.go
import coder.apps.space.library.extension.hasPermissions
import coder.apps.space.library.extension.log
import coder.apps.space.library.helper.LeakGuardHandlerWrapper
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.LocationSettingsResponse
import com.google.android.gms.tasks.Task
import kotlin.jvm.java

class DashboardActivity : BaseActivity<ActivityDashboardBinding>(ActivityDashboardBinding::inflate) {

    private var pendingAction: (() -> Unit)? = null
    private val TAG = DashboardActivity::class.java.simpleName
    private var handlerSettingOverLay: HandleSettingPreview? = null

    private val permissions = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        val allPermissionsGranted = permissions.all { entry -> entry.value }
        if (!allPermissionsGranted) {
            if (shouldShowRequestPermissionRationale(Manifest.permission.READ_PHONE_STATE)) {
                showPermissionExplanationDialog("We need the phone state,notification permission to detecting state of call. Please grant the permission.")
            } else {
                showFallbackDialog()
            }
        }else if (!isGrantedOverlay()) {
            sendToSettings()
        }
    }

    private fun showPermissionExplanationDialog(message: String) {
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setTitle("Phone and Notification Permission Required").setMessage(message).setPositiveButton("OK") { dialog, which -> permissions.launch(PERMISSION_MAIN) }.show()
    }


    private fun showFallbackDialog() {
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setTitle("Phone and Notification Permission Denied").setMessage("The app cannot function properly without that permission. Please enable the permission in the app settings.").setPositiveButton("Open Settings") { dialog, which -> openAppSettings() }.show()
    }

    private fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        intent.setData(Uri.parse("package:$packageName"))
        startActivity(intent)
    }

    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        val allGranted = permissions.values.all { it }
        if (!allGranted) {
            pendingAction = null
            Toast.makeText(this, "Location permissions required", Toast.LENGTH_SHORT).show()
            incrementPermissionsDeniedCount("PERMISSION_LOCATION")
        } else {
            pendingAction?.invoke()
            pendingAction = null
        }
    }

    override fun ActivityDashboardBinding.initExtra() {
        handlerSettingOverLay = HandleSettingPreview(this@DashboardActivity)
        appOpenManager = AppOpenManager()
        if (!hasPermissions(PERMISSION_MAIN)) {
            permissions.launch(PERMISSION_MAIN)
        }else if (!isGrantedOverlay()) {
            sendToSettings()
        }

        viewBanner(adNative)
    }

    private fun sendToSettings() {
        val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
        handlerSettingOverLay?.startPollingImeSettings()
        App.isOpenInter=true
        checkOverlay.launch(intent)
    }

    private var checkOverlay: ActivityResultLauncher<Intent> = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
    }

    override fun ActivityDashboardBinding.initListeners() {
        mapBanner.setOnClickListener {
            checkLocationPermissionAndProceed {
                go(MapActivity::class.java)
            }
        }
        actionMap.setOnClickListener {
            checkLocationPermissionAndProceed {
                go(MapActivity::class.java)
            }
        }
        actionVoice.setOnClickListener {
            checkLocationPermissionAndProceed {
                go(MapActivity::class.java, listOf("is_voice_navigation" to true))
            }
        }
        actionRouteFinder.setOnClickListener {
            checkLocationPermissionAndProceed {
                go(RouteActivity::class.java)
            }
        }
        actionSpeedometer.setOnClickListener {
            checkLocationPermissionAndProceed {
                go(SpeedViewActivity::class.java)
            }
        }
        actionCompass.setOnClickListener {
            checkLocationPermissionAndProceed {
                go(CompassActivity::class.java)
            }
        }
        actionNear.setOnClickListener {
            checkLocationPermissionAndProceed {
                go(NearActivity::class.java)
            }
        }
        actionArea.setOnClickListener {
            checkLocationPermissionAndProceed {
                go(AreaCalcActivity::class.java)
            }
        }
        actionGpsCamera.setOnClickListener {
            checkLocationPermissionAndProceed {
                go(GPSCameraActivity::class.java)
            }
        }
        buttonSettings.setOnClickListener {
            go(AppSettingsActivity::class.java)
        }
    }

    private fun checkAndPromptLocationSettings() {
        val locationRequest = LocationRequest.create().apply {
            interval = 10000
            fastestInterval = 5000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }
        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
        val client = LocationServices.getSettingsClient(this@DashboardActivity)
        val task: Task<LocationSettingsResponse> = client.checkLocationSettings(builder.build())

        task.addOnSuccessListener { response: LocationSettingsResponse ->
            TAG.log("All location settings are satisfied.")
        }

        task.addOnFailureListener { exception ->
            if (exception is ResolvableApiException) {
                try {
                    exception.startResolutionForResult(this@DashboardActivity, 4634)
                } catch (_: IntentSender.SendIntentException) {
                }
            }
        }
    }

    private fun checkLocationPermissionAndProceed(action: () -> Unit) {
        if (!hasPermissions(LOCATION_PERMISSION)) {
            pendingAction = {
                if (isLocationEnabled()) {
                    action()
                } else {
                    checkAndPromptLocationSettings()
                }
            }
            permissionLauncher.launch(LOCATION_PERMISSION)
        } else if (!isLocationEnabled()) {
            checkAndPromptLocationSettings()
        } else {
            action()
        }
    }

    override fun ActivityDashboardBinding.initView() {
        onBackPressedDispatcher.addCallback {
            finish()
        }
    }

    class HandleSettingPreview internal constructor(activity: DashboardActivity) : LeakGuardHandlerWrapper<DashboardActivity>(activity) {

        fun cancelPollingImeSettings() {
            removeMessages(0)
        }

        override fun handleMessage(message: Message) {
            val ownerInstance = ownerInstance
            if (ownerInstance != null && message.what == 0) {
                if (Settings.canDrawOverlays(ownerInstance)) {
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

    private fun invokeSetupWizardOfThisIme() {
        handlerSettingOverLay?.cancelPollingImeSettings()
        val intent = Intent()
        intent.setClass(this@DashboardActivity, DashboardActivity::class.java)
        intent.flags = 606076928
        startActivity(intent)
    }
}