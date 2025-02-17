package aanibrothers.tracker.io.ui

import aanibrothers.tracker.io.R
import aanibrothers.tracker.io.databinding.*
import aanibrothers.tracker.io.extension.*
import android.animation.*
import android.content.*
import android.hardware.*
import android.os.*
import android.view.animation.*
import android.widget.*
import androidx.activity.*
import androidx.activity.result.contract.*
import coder.apps.space.library.base.*

class CompassActivity : BaseActivity<ActivityCompassBinding>(
    ActivityCompassBinding::inflate
), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var rotationVectorSensor: Sensor? = null
    // Fallback sensors if rotation vector is not available
    private var accelerometer: Sensor? = null
    private var magnetometer: Sensor? = null

    // For fallback sensor fusion
    private var gravity: FloatArray? = null
    private var geomagnetic: FloatArray? = null

    private var currentDegree = 0f
    private val animatorDuration = 200L // ms
    // A threshold to filter out noise in degree changes (in degrees)
    private val degreeThreshold = 1f

    // For permission results
    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.containsValue(false)) {
            incrementPermissionsDeniedCount("PERMISSION_LOCATION")
            Toast.makeText(this, "Location permission required", Toast.LENGTH_SHORT).show()
        } else {
            startUpdates()
        }
    }

    private fun startUpdates() {
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager

        // Try to use the rotation vector sensor for better stability
        rotationVectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        if (rotationVectorSensor != null) {
            sensorManager.registerListener(
                this@CompassActivity,
                rotationVectorSensor,
                SensorManager.SENSOR_DELAY_UI
            )
        } else {
            // If not available, fallback to accelerometer and magnetometer
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
            accelerometer?.also {
                sensorManager.registerListener(
                    this@CompassActivity,
                    it,
                    SensorManager.SENSOR_DELAY_UI
                )
            }
            magnetometer?.also {
                sensorManager.registerListener(
                    this@CompassActivity,
                    it,
                    SensorManager.SENSOR_DELAY_UI
                )
            }
        }
    }

    override fun ActivityCompassBinding.initExtra() {
        if (hasPermissions(LOCATION_PERMISSION)) {
            startUpdates()
        } else {
            locationPermissionLauncher.launch(LOCATION_PERMISSION)
        }
    }

    override fun ActivityCompassBinding.initListeners() {
        // Your listeners here if any.
    }

    override fun ActivityCompassBinding.initView() {
        toolbar.title = getString(R.string.title_compass)
        toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
        onBackPressedDispatcher.addCallback {
            finish()
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        var azimuth = 0f

        if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
            // Use the rotation vector sensor for orientation
            val rotationMatrix = FloatArray(9)
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
            val orientation = FloatArray(3)
            SensorManager.getOrientation(rotationMatrix, orientation)
            // Convert radians to degrees and normalize to [0, 360)
            azimuth = ((Math.toDegrees(orientation[0].toDouble()) + 360) % 360).toFloat()
        } else {
            // Fallback: Use accelerometer and magnetometer
            when (event.sensor.type) {
                Sensor.TYPE_ACCELEROMETER -> gravity = event.values.clone()
                Sensor.TYPE_MAGNETIC_FIELD -> geomagnetic = event.values.clone()
            }
            if (gravity != null && geomagnetic != null) {
                val rotationMatrix = FloatArray(9)
                if (SensorManager.getRotationMatrix(rotationMatrix, null, gravity, geomagnetic)) {
                    val orientation = FloatArray(3)
                    SensorManager.getOrientation(rotationMatrix, orientation)
                    azimuth = ((Math.toDegrees(orientation[0].toDouble()) + 360) % 360).toFloat()
                }
            }
        }

        // Only animate if change is significant
        if (Math.abs(azimuth - currentDegree) >= degreeThreshold) {
            binding?.rotateNeedleSmoothly(azimuth)
        }
    }

    private fun ActivityCompassBinding.rotateNeedleSmoothly(newDegree: Float) {
        // Compute the smallest difference between angles
        var start = currentDegree
        var end = newDegree

        // Adjust values so that we rotate in the shortest direction
        val diff = ((end - start + 540) % 360) - 180
        end = start + diff

        // Create and start a new animator immediately
        val animator = ValueAnimator.ofFloat(start, end).apply {
            duration = animatorDuration
            interpolator = DecelerateInterpolator()
            addUpdateListener { animation ->
                val animatedValue = animation.animatedValue as Float
                imageNeedle.rotation = animatedValue
            }
        }
        animator.start()

        // Update currentDegree when the animation ends
        currentDegree = (newDegree + 360) % 360
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not used
    }
}
