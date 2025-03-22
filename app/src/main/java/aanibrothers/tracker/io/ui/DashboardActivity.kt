package aanibrothers.tracker.io.ui

import aanibrothers.tracker.io.databinding.*
import aanibrothers.tracker.io.extension.*
import aanibrothers.tracker.io.module.*
import android.*
import android.content.*
import android.os.*
import androidx.activity.*
import androidx.activity.result.contract.*
import coder.apps.space.library.base.*
import coder.apps.space.library.extension.*
import com.google.android.gms.common.api.*
import com.google.android.gms.location.*
import com.google.android.gms.tasks.*

class DashboardActivity : BaseActivity<ActivityDashboardBinding>(ActivityDashboardBinding::inflate) {
    private val TAG = DashboardActivity::class.java.simpleName
    private val notificationLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        if (permissions.containsValue(false)) {
            incrementPermissionsDeniedCount("notification")
        }
    }

    override fun ActivityDashboardBinding.initExtra() {
        val mode = intent.getStringExtra("mode")
        val goto = intent.getStringExtra("goto")
        if (mode == "aftercall") {
            when (goto) {
                "map" -> go(MapActivity::class.java)
                "compass" -> go(CompassActivity::class.java)
                "speedometer" -> go(SpeedViewActivity::class.java)
                "route" -> go(RouteActivity::class.java)
                "area" -> go(AreaCalcActivity::class.java)
                "near" -> go(NearActivity::class.java)
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!hasPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS))) {
                notificationLauncher.launch(arrayOf(Manifest.permission.POST_NOTIFICATIONS))
            }
        }

        if (!isPremium) viewBanner(adNative) else adNative.beGone()
    }

    override fun ActivityDashboardBinding.initListeners() {
        mapBanner.setOnClickListener {
            if (!isLocationEnabled()) {
                checkAndPromptLocationSettings()
                return@setOnClickListener
            }
            go(MapActivity::class.java)
        }
        actionMap.setOnClickListener {
            if (!isLocationEnabled()) {
                checkAndPromptLocationSettings()
                return@setOnClickListener
            }
            go(MapActivity::class.java)
        }
        actionVoice.setOnClickListener {
            if (!isLocationEnabled()) {
                checkAndPromptLocationSettings()
                return@setOnClickListener
            }
            go(MapActivity::class.java, listOf("is_voice_navigation" to true))
        }
        actionRouteFinder.setOnClickListener {
            if (!isLocationEnabled()) {
                checkAndPromptLocationSettings()
                return@setOnClickListener
            }
            go(RouteActivity::class.java)
        }
        actionSpeedometer.setOnClickListener {
            if (!isLocationEnabled()) {
                checkAndPromptLocationSettings()
                return@setOnClickListener
            }
            go(SpeedViewActivity::class.java)
        }
        actionCompass.setOnClickListener {
            if (!isLocationEnabled()) {
                checkAndPromptLocationSettings()
                return@setOnClickListener
            }
            go(CompassActivity::class.java)
        }
        actionNear.setOnClickListener {
            if (!isLocationEnabled()) {
                checkAndPromptLocationSettings()
                return@setOnClickListener
            }
            go(NearActivity::class.java)
        }
        actionArea.setOnClickListener {
            if (!isLocationEnabled()) {
                checkAndPromptLocationSettings()
                return@setOnClickListener
            }
            go(AreaCalcActivity::class.java)
        }
        actionGpsCamera.setOnClickListener {
            if (!isLocationEnabled()) {
                checkAndPromptLocationSettings()
                return@setOnClickListener
            }
            go(GPSCameraActivity::class.java)
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
            } else {
            }
        }
    }

    override fun ActivityDashboardBinding.initView() {
        onBackPressedDispatcher.addCallback {
            finish()
        }
    }
}