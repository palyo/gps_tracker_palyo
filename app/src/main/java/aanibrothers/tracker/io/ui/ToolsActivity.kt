package aanibrothers.tracker.io.ui

import aanibrothers.tracker.io.databinding.ActivityToolsBinding
import aanibrothers.tracker.io.extension.LOCATION_PERMISSION
import aanibrothers.tracker.io.extension.isLocationEnabled
import aanibrothers.tracker.io.module.viewBanner
import android.content.IntentSender
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import coder.apps.space.library.base.BaseActivity
import coder.apps.space.library.extension.go
import coder.apps.space.library.extension.hasPermissions
import coder.apps.space.library.extension.log
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.LocationSettingsResponse
import com.google.android.gms.tasks.Task

class ToolsActivity : BaseActivity<ActivityToolsBinding>(ActivityToolsBinding::inflate) {

    private var pendingAction: (() -> Unit)? = null
    private val TAG = ToolsActivity::class.java.simpleName

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
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

    override fun ActivityToolsBinding.initExtra() {
        viewBanner(adNative)
    }

    override fun ActivityToolsBinding.initListeners() {
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
    }

    private fun checkAndPromptLocationSettings() {
        val locationRequest = LocationRequest.create().apply {
            interval = 10000
            fastestInterval = 5000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }
        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
        val client = LocationServices.getSettingsClient(this@ToolsActivity)
        val task: Task<LocationSettingsResponse> = client.checkLocationSettings(builder.build())

        task.addOnSuccessListener { response: LocationSettingsResponse ->
            TAG.log("All location settings are satisfied.")
        }

        task.addOnFailureListener { exception ->
            if (exception is ResolvableApiException) {
                try {
                    exception.startResolutionForResult(this@ToolsActivity, 4634)
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

    override fun ActivityToolsBinding.initView() {
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
        onBackPressedDispatcher.addCallback { finish() }
    }
}