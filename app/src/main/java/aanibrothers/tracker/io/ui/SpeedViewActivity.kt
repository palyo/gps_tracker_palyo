package aanibrothers.tracker.io.ui

import aanibrothers.tracker.io.R
import aanibrothers.tracker.io.databinding.*
import aanibrothers.tracker.io.extension.*
import aanibrothers.tracker.io.module.isPremium
import aanibrothers.tracker.io.module.viewBanner
import aanibrothers.tracker.io.module.viewInterAdWithLogic
import aanibrothers.tracker.io.module.viewNativeBanner
import android.Manifest
import android.animation.*
import android.annotation.*
import android.graphics.*
import android.graphics.drawable.*
import android.location.*
import android.os.*
import android.view.*
import android.widget.*
import androidx.activity.*
import androidx.activity.result.contract.*
import androidx.core.content.*
import androidx.core.content.res.*
import coder.apps.space.library.base.*
import coder.apps.space.library.extension.*
import com.google.android.gms.location.*
import com.google.android.gms.location.LocationRequest

class SpeedViewActivity : BaseActivity<ActivitySpeedViewBinding>(ActivitySpeedViewBinding::inflate) {
    private var fusedLocationClient: FusedLocationProviderClient? = null
    private var locationCallback: LocationCallback? = null
    private val speedWindow = ArrayList<Float>(5)
    private var speedSum = 0f
    private var lastLocation: Location? = null
    private var lastMovingTime = System.currentTimeMillis()
    private val locationPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        if (permissions.containsValue(false)) {
            incrementPermissionsDeniedCount("PERMISSION_LOCATION")
            Toast.makeText(this, "Location permission required", Toast.LENGTH_SHORT).show()
        } else {
            startLocationUpdates()
        }
    }

    override fun ActivitySpeedViewBinding.initExtra() {
        speedometer.speedTextTypeface = ResourcesCompat.getFont(this@SpeedViewActivity, coder.apps.space.library.R.font.bold)
        speedometer.textTypeface = ResourcesCompat.getFont(this@SpeedViewActivity, coder.apps.space.library.R.font.medium)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this@SpeedViewActivity)
        createLocationCallback()
        if (hasPermissions(LOCATION_PERMISSION)) {
            startLocationUpdates()
        } else {
            locationPermissionLauncher.launch(LOCATION_PERMISSION)
        }
    }

    private fun ActivitySpeedViewBinding.updateSpeedUI(location: Location?) {
        location?.let {
            if (lastLocation != null && it.distanceTo(lastLocation!!) < 2.5f) {
                return
            }
            lastLocation = it

            if (it.accuracy > 10) return
            val newSpeed = it.speed * 3.6f // Convert m/s to km/h

            if (newSpeed < 1.0f) {
                if (System.currentTimeMillis() - lastMovingTime > 3000) {
                    speedometer.setSpeedAt(0f)
                    speedWindow.clear()
                    speedSum = 0f
                    animateBackgroundColor(ContextCompat.getColor(this@SpeedViewActivity, R.color.colorAccent))
                    return
                }
            } else {
                lastMovingTime = System.currentTimeMillis()
            }

            if (speedWindow.size >= 5) {
                speedSum -= speedWindow.removeAt(0)
            }
            speedWindow.add(newSpeed)
            speedSum += newSpeed
            val averageSpeed = speedSum / speedWindow.size

            if (Math.abs(averageSpeed - speedometer.speed) > 0.3f) {
                speedometer.setSpeedAt(averageSpeed)
                animateBackgroundColor(getSpeedRiskColor(averageSpeed))
            }
        }

        if (!isPremium && !hasPermissions(arrayOf(Manifest.permission.READ_PHONE_STATE))) viewNativeBanner(adNative) else adNative.beGone()
    }

    private fun animateBackgroundColor(newColor: Int) {
        val colorFrom = (binding?.root?.background as? ColorDrawable)?.color ?: Color.WHITE
        ValueAnimator.ofObject(ArgbEvaluator(), colorFrom, newColor).apply {
            duration = 600 // Animation duration in milliseconds
            addUpdateListener { animator ->
                binding?.root?.setBackgroundColor(animator.animatedValue as Int)
            }
            start()
        }
    }

    private fun getSpeedRiskColor(speed: Float): Int {
        return when {
            speed > 60 -> Color.RED
            speed > 30 -> Color.YELLOW
            else -> Color.GREEN
        }
    }

    private fun createLocationCallback() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.locations.forEach { location ->
                    binding?.updateSpeedUI(location)
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY, 1000
        )
            .setMinUpdateIntervalMillis(1000)
            .setDurationMillis(Long.MAX_VALUE)
            .setWaitForAccurateLocation(true)
            .build()

        if (hasPermissions(LOCATION_PERMISSION)) {
            locationCallback?.let {
                fusedLocationClient?.requestLocationUpdates(
                    locationRequest, it, Looper.getMainLooper()
                )
            }
        }
    }

    override fun ActivitySpeedViewBinding.initListeners() {

    }

    override fun ActivitySpeedViewBinding.initView() {
        toolbar.title = getString(R.string.title_speedometer)
        toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        onBackPressedDispatcher.addCallback {
            viewInterAdWithLogic {
                finish()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        fusedLocationClient?.removeLocationUpdates(locationCallback!!)
    }
}