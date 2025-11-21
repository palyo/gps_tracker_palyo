package aanibrothers.tracker.io.ui

import aanibrothers.tracker.io.databinding.*
import aanibrothers.tracker.io.extension.*
import aanibrothers.tracker.io.module.*
import android.*
import android.content.*
import android.os.*
import android.widget.*
import androidx.activity.*
import androidx.activity.result.contract.*
import coder.apps.space.library.base.*
import coder.apps.space.library.extension.*
import com.google.android.gms.common.api.*
import com.google.android.gms.location.*
import com.google.android.gms.tasks.*

class DashboardActivity : BaseActivity<ActivityDashboardBinding>(ActivityDashboardBinding::inflate) {

    private var pendingAction: (() -> Unit)? = null
    private val TAG = DashboardActivity::class.java.simpleName
    private val notificationLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        if (permissions.containsValue(false)) {
            incrementPermissionsDeniedCount("notification")
        }
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!hasPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS))) {
                notificationLauncher.launch(arrayOf(Manifest.permission.POST_NOTIFICATIONS))
            }
        }

        viewBanner(adNative)
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
        val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)
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
}