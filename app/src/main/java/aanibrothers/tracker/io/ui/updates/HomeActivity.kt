package aanibrothers.tracker.io.ui.updates

import aanibrothers.tracker.io.App
import aanibrothers.tracker.io.App.Companion.appOpenManager
import aanibrothers.tracker.io.R
import aanibrothers.tracker.io.databinding.ActivityHomeBinding
import aanibrothers.tracker.io.extension.AFTER_CALL_PERMISSION
import aanibrothers.tracker.io.extension.isGrantedOverlay
import aanibrothers.tracker.io.extension.viewPermission
import aanibrothers.tracker.io.helper.HandleSettingPreview
import aanibrothers.tracker.io.helper.PaintOverlayRenderer
import aanibrothers.tracker.io.locations.LocationPreference
import aanibrothers.tracker.io.model.CaptureMode
import aanibrothers.tracker.io.model.LocationMode
import aanibrothers.tracker.io.model.OverlayState
import aanibrothers.tracker.io.model.OverlayTemplate
import aanibrothers.tracker.io.module.AppOpenManager
import aanibrothers.tracker.io.ui.AppSettingsActivity
import aanibrothers.tracker.io.ui.ToolsActivity
import aanibrothers.tracker.io.widgets.FocusView
import android.Manifest
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.RectF
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.util.TypedValue
import android.view.MotionEvent
import android.view.OrientationEventListener
import android.view.Surface
import android.view.View
import android.view.ViewTreeObserver
import android.view.WindowInsets
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.core.TorchState
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FallbackStrategy
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.content.edit
import androidx.core.graphics.createBitmap
import androidx.core.graphics.scale
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.effect.OverlayEffect
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import coder.apps.space.library.base.BaseActivity
import coder.apps.space.library.extension.beInvisible
import coder.apps.space.library.extension.beVisible
import coder.apps.space.library.extension.color
import coder.apps.space.library.extension.disable
import coder.apps.space.library.extension.drawable
import coder.apps.space.library.extension.enable
import coder.apps.space.library.extension.go
import coder.apps.space.library.extension.goResult
import coder.apps.space.library.extension.hasPermission
import coder.apps.space.library.extension.hasPermissions
import coder.apps.space.library.extension.statusBarHeight
import com.bumptech.glide.Glide
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.LocationSettingsResponse
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.tasks.Task
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class HomeActivity : BaseActivity<ActivityHomeBinding>(
    ActivityHomeBinding::inflate, isFullScreen = true, isFullScreenIncludeNav = true
) {

    private var doubleBackToExitPressedOnce = false
    private val PERMISSION_REQUEST_CODE = 100
    private var handlerSettingOverLay: HandleSettingPreview? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private var captureMode = CaptureMode.PHOTO
    private lateinit var videoCapture: VideoCapture<Recorder>
    private var activeRecording: Recording? = null
    private var isRecording = false

    private var isFocusManual = true
    private var isFirstClick = true
    private var isSoundOn = true
    private var isBackCameraSelected = true

    private var lastCapturedFile: File? = null
    private var imageCapture: ImageCapture? = null
    private var currentOrientation = 0
    private var currentScreenRotation = Surface.ROTATION_0
    private var isPortraitMode = true
    private val LOCATION_SETTINGS_REQUEST = 4634

    private val timeHandler = Handler(Looper.getMainLooper())
    private lateinit var timeRunnable: Runnable

    private var orientationEventListener: OrientationEventListener? = null
    private lateinit var locationCallback: LocationCallback

    private val locationUpdateResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode != Activity.RESULT_OK) return@registerForActivityResult
            restoreLocationModeFromPref()
        }

    /*private val permissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val allPermissionsGranted = permissions.all { entry -> entry.value }
            if (!allPermissionsGranted) {
                if (shouldShowRequestPermissionRationale(Manifest.permission.READ_PHONE_STATE)) {
                    showPermissionExplanationDialog()
                } else {
                    showFallbackDialog()
                }
            } else if (!isGrantedOverlay()) {
                sendToSettings()
            }
        }*/

    private val permissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            if (result[Manifest.permission.READ_PHONE_STATE] == false) {
                incrementPermissionsDeniedCount("phone_state")
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                result[Manifest.permission.POST_NOTIFICATIONS] == false
            ) {
                incrementPermissionsDeniedCount("post_notifications")
            }

            if (result.values.any { !it }) {
                showPermissionExplanationDialog()
                return@registerForActivityResult
            }

            if (!isGrantedOverlay()) {
                sendToSettings()
            }
        }

    private var checkOverlay: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {}

    private var locationMode = LocationMode.CURRENT
    private var customLocation: Location? = null

    private var overlayState = OverlayState()
    private val REQ_AUDIO = 999
    private var allowAudio = false

    override fun ActivityHomeBinding.initExtra() {
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        isPortraitMode = true

        initializeComponents()
        setupOrientationListener()
        setupFocusView()
        setupTab()
        setupMapSnapshot()
        displayCurrentLocation()
        setupTimeDisplay()
        handlerSettingOverLay = HandleSettingPreview(this@HomeActivity)
        appOpenManager = AppOpenManager()
        requestPermissions()
    }

    private var googleMap: GoogleMap? = null
    private val lastMapSnapshot = java.util.concurrent.atomic.AtomicReference<Bitmap?>()

    private fun getActiveMapFragment(): SupportMapFragment? {
        return when (tinyDB?.getString("template", "default")) {
            "classic" -> supportFragmentManager.findFragmentById(R.id.map_fragment_classic) as? SupportMapFragment

            "squarise" -> supportFragmentManager.findFragmentById(R.id.map_fragment_squarise) as? SupportMapFragment

            else -> supportFragmentManager.findFragmentById(R.id.map_fragment) as? SupportMapFragment
        }
    }

    private fun setupMapSnapshot() {
        val fragment = getActiveMapFragment() ?: return
        fragment.getMapAsync { map ->
            googleMap = map
            map.setOnMapLoadedCallback {
                captureMapSnapshotSafe()
            }
        }
    }

    private fun captureMapSnapshotSafe() {
        val fragment = getActiveMapFragment() ?: return
        fragment.getMapAsync { map ->
            val mapView = fragment.view ?: return@getMapAsync

            if (mapView.width == 0 || mapView.height == 0) {
                mapView.viewTreeObserver.addOnGlobalLayoutListener(object :
                    ViewTreeObserver.OnGlobalLayoutListener {
                    override fun onGlobalLayout() {
                        mapView.viewTreeObserver.removeOnGlobalLayoutListener(this)
                        captureMapSnapshotSafe()
                    }
                })
                return@getMapAsync
            }

            map.snapshot { bmp ->
                if (bmp != null && bmp.width > 0 && bmp.height > 0) {
                    lastMapSnapshot.getAndSet(bmp)?.recycle()
                }
            }
        }
    }


    fun ActivityHomeBinding.setupTab() {
        tabCaptureMode.apply {
            addTab(newTab().setText("PHOTO"))
            addTab(newTab().setText("VIDEO"))

            addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
                override fun onTabSelected(tab: TabLayout.Tab) {
                    captureMode = if (tab.position == 0) CaptureMode.PHOTO else CaptureMode.VIDEO
                    updateCaptureUI()
                    if (captureMode == CaptureMode.VIDEO && !hasPermission(Manifest.permission.RECORD_AUDIO)) {
                        showAudioPermissionDialog()
                    } else {
                        allowAudio = hasPermission(Manifest.permission.RECORD_AUDIO)
                    }
                }

                override fun onTabUnselected(tab: TabLayout.Tab) {}
                override fun onTabReselected(tab: TabLayout.Tab) {}
            })
        }
    }

    private fun updateCaptureUI() {
        binding?.apply {
            val params = layoutRootContainerPreview.layoutParams as ConstraintLayout.LayoutParams
            if (captureMode == CaptureMode.VIDEO) {
                params.dimensionRatio = "9:16"
                actionCapture.icon = drawable(R.drawable.ic_action_record_button)
                isPortraitMode = true
                actionSound.disable()
                actionTimer.disable()
            } else {
                params.dimensionRatio = "3:4"
                actionCapture.icon = drawable(R.drawable.ic_action_capture_button)
                isPortraitMode = true
                actionSound.enable()
                actionTimer.enable()
            }

            layoutRootContainerPreview.layoutParams = params

            cameraProvider?.let {
                restartCameraWithCorrectOrientation(it)
            }
        }
    }

    private fun showAudioPermissionDialog() {
        viewPermission(
            title = "Microphone Permission",
            body = "Allow microphone to record video with sound?",
            positiveButton = "Okay", isNegativeButton = false
        ) {
            if (it) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.RECORD_AUDIO),
                    REQ_AUDIO
                )
            } else {
                allowAudio = false
            }
        }
    }

    private fun showPermissionBlockedDialog(message: String) {
        viewPermission(
            title = "Permission Required",
            body = message,
            positiveButton = "Open Settings", isNegativeButton = false
        ) {
            if (it) {
                openAppSettings()
            }
        }
    }

    private fun restoreLocationModeFromPref() {
        val savedLocation = LocationPreference.getSelectedLocation(this)
        if (savedLocation == null) {
            locationMode = LocationMode.CURRENT
            customLocation = null
            startLocationUpdates()
        } else {
            locationMode = LocationMode.CUSTOM
            stopLocationUpdates()

            customLocation = Location("custom").apply {
                latitude = savedLocation.latitude
                longitude = savedLocation.longitude
            }

            updateLocationUI(customLocation!!)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQ_AUDIO) {
            val granted = grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED
            allowAudio = granted
            if (!granted) {
                showPermissionBlockedDialog("Microphone permission is required for audio. You can enable it in Settings.")
            }
            return
        }
        handlePermissionResults(permissions, grantResults, requestCode)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == LOCATION_SETTINGS_REQUEST) {
            if (isLocationEnabled()) {
                startLocationUpdates()
                displayCurrentLocation()
                val mapFragment = getActiveMapFragment()
                mapFragment?.getMapAsync { googleMap ->
                    updateMapMarker(googleMap)
                    captureMapSnapshotSafe()
                }
            } else {
                Toast.makeText(this, "Location still disabled", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        orientationEventListener?.enable()
        if (hasPermission(Manifest.permission.CAMERA)) {
            checkAndPromptLocationSettings()
            binding?.startCamera()
        }
        restoreLocationModeFromPref()
    }

    override fun onStop() {
        super.onStop()
        orientationEventListener?.disable()
        stopLocationUpdates()
    }

    override fun onResume() {
        super.onResume()
        restoreUIState()
        checkGooglePlayServices()
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        binding?.apply {
            layoutMapData.isVisible = tinyDB?.getString("template", "default") == "default"
            layoutMapDataClassic.isVisible = tinyDB?.getString("template", "default") == "classic"
            layoutMapDataSquarise.isVisible = tinyDB?.getString("template", "default") == "squarise"
            getLatestCapturedFile()?.let { latestFile ->
                lastCapturedFile = latestFile
                Glide.with(this@HomeActivity).load(latestFile).into(imageCaptured)
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        currentScreenRotation = windowManager.defaultDisplay.rotation
        cameraProvider?.let { provider ->
            restartCameraWithCorrectOrientation(provider)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        orientationEventListener?.disable()
        stopLocationUpdates()
        timeHandler.removeCallbacks(timeRunnable)
    }

    private fun setupOrientationListener() {
        orientationEventListener = object : OrientationEventListener(this) {
            override fun onOrientationChanged(orientation: Int) {
                if (orientation == ORIENTATION_UNKNOWN) return
                currentScreenRotation = windowManager.defaultDisplay.rotation
                currentOrientation = when (currentScreenRotation) {
                    Surface.ROTATION_0 -> 0
                    Surface.ROTATION_90 -> 90
                    Surface.ROTATION_180 -> 180
                    Surface.ROTATION_270 -> 270
                    else -> 0
                }
            }
        }
    }

    private fun getTargetAspectRatio(): Int {
        return if (captureMode == CaptureMode.VIDEO) {
            AspectRatio.RATIO_16_9
        } else {
            AspectRatio.RATIO_4_3
        }
    }

    private fun restartCameraWithCorrectOrientation(cameraProvider: ProcessCameraProvider) {
        binding?.let { binding ->
            val targetRotation = currentScreenRotation
            val targetAspectRatio = getTargetAspectRatio()

            try {
                cameraProvider.unbindAll()

                val preview = Preview.Builder().setTargetAspectRatio(targetAspectRatio)
                    .setTargetRotation(targetRotation).build().also {
                        it.surfaceProvider = binding.previewView.surfaceProvider
                    }

                imageCapture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                    .setTargetAspectRatio(targetAspectRatio).setTargetRotation(targetRotation)
                    .build()

                val cameraSelector = if (isBackCameraSelected) {
                    CameraSelector.DEFAULT_BACK_CAMERA
                } else {
                    CameraSelector.DEFAULT_FRONT_CAMERA
                }
                val recorder = Recorder.Builder().setQualitySelector(
                    QualitySelector.from(
                        Quality.FHD, FallbackStrategy.higherQualityOrLowerThan(Quality.HD)
                    )
                ).build()

                videoCapture = VideoCapture.withOutput(recorder)
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture, videoCapture
                )
            } catch (exc: Exception) {
                Log.e(HomeActivity::class.java.simpleName, "Camera restart failed", exc)
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun initializeComponents() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this@HomeActivity)
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
        }, ContextCompat.getMainExecutor(this))

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        binding?.apply {
            val capturedBy = "Captured by: ${getString(R.string.app_name)}"
            textCapturedBy.text = capturedBy
            textCapturedByClassic.text = capturedBy
            textCapturedBySquarise.text = capturedBy
        }
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    runOnUiThread {
                        getSelectedLocation(location)?.let {
                            updateLocationUI(it)
                        }
                    }
                }
            }
        }
    }

    private fun updateOverlayState(location: Location, addressText: String) {
        val localTime = SimpleDateFormat("HH:mm:ss a", Locale.getDefault()).format(Date())
        val gmtFormat = SimpleDateFormat("HH:mm:ss a", Locale.getDefault()).apply {
            timeZone = TimeZone.getTimeZone("GMT")
        }
        val gmtTime = gmtFormat.format(Date())
        val date = SimpleDateFormat("EEEE, dd/MM/yyyy", Locale.getDefault()).format(Date())

        overlayState = OverlayState(
            address = addressText,
            lat = location.latitude,
            lng = location.longitude,
            altitudeMeters = if (location.hasAltitude()) location.altitude else null,
            localTime = localTime,
            gmtTime = gmtTime,
            date = date
        )
    }

    private fun getTypeFragment(): OverlayTemplate {
        return when (tinyDB?.getString("template", "default")) {
            "classic" -> OverlayTemplate.CLASSIC
            "squarise" -> OverlayTemplate.SQUARISE
            else -> OverlayTemplate.DEFAULT
        }
    }

    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    private fun processVideoWithOverlay(
        inputFile: File, outputFile: File, onSuccess: () -> Unit, onError: (Exception) -> Unit
    ) {
        val bmp = lastMapSnapshot.get()
        val overlay = PaintOverlayRenderer(
            this@HomeActivity,
            stateProvider = { overlayState },
            mapProvider = { bmp },
            mapViewType = getTypeFragment()
        )

        val overlayEffect = OverlayEffect(listOf(overlay))
        val effects = Effects(listOf(), listOf(overlayEffect))

        val mediaItem = MediaItem.fromUri(Uri.fromFile(inputFile))
        val edited = EditedMediaItem.Builder(mediaItem).setEffects(effects).build()

        val transformer = Transformer.Builder(this).build()
        transformer.addListener(object : Transformer.Listener {
            override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                super.onCompleted(composition, exportResult)
                onSuccess()
            }

            override fun onError(
                composition: Composition,
                exportResult: ExportResult,
                exportException: ExportException
            ) {
                super.onError(composition, exportResult, exportException)
                onError(exportException)
            }
        })

        transformer.start(edited, outputFile.absolutePath)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun ActivityHomeBinding.setupFocusView() {
        focusView.visibility = View.INVISIBLE
        previewView.setOnTouchListener { _, event ->
            handleTouchEvent(event)
            return@setOnTouchListener true
        }
        startFocusTimer()
    }

    private fun ActivityHomeBinding.setupTimeDisplay() {
        timeRunnable = object : Runnable {
            override fun run() {
                updateTimeDisplay()
                timeHandler.postDelayed(this, 1000)
            }
        }
        timeHandler.post(timeRunnable)
    }

    private fun requestPermissions() {
        displayCurrentLocation()
        if(!hasPermissions(AFTER_CALL_PERMISSION) || !isGrantedOverlay()) {
            showPermissionExplanationDialog()
        }
    }

    private fun showPermissionExplanationDialog() {
        val phoneDenied = fetchPermissionsDeniedCount("phone_state")
        val notifDenied = fetchPermissionsDeniedCount("post_notifications")

        val phoneBlocked =
            !shouldShowRequestPermissionRationale(Manifest.permission.READ_PHONE_STATE) && phoneDenied > 0

        val notifBlocked =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    !shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) &&
                    notifDenied > 0
        if (phoneBlocked || notifBlocked) {
            showFallbackDialog()
        } else {
            viewPermission(
                title = "Phone and Notification Permission Required",
                body = "We need the phone state and notification permissions to detect call state and show alerts.",
                positiveButton = "Okay",
                isNegativeButton = false
            ) { accepted ->
                if (!accepted) return@viewPermission

                val permissionsToAsk = mutableListOf(Manifest.permission.READ_PHONE_STATE)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    permissionsToAsk.add(Manifest.permission.POST_NOTIFICATIONS)
                }

                if (permissionsToAsk.all { hasPermission(it) }) {
                    if (!isGrantedOverlay()) sendToSettings()
                    return@viewPermission
                }


                if (phoneDenied >= 1 || notifDenied >= 1) {
                    showPermissionBlockedDialog(
                        "Phone or notification permission is required. Please enable it in Settings."
                    )
                } else {
                    permissions.launch(permissionsToAsk.toTypedArray())
                }
            }
        }
    }

    private fun showFallbackDialog() {
        viewPermission(
            title = "Phone and Notification Permission Denied",
            body = "The app cannot function properly without that permission. Please enable the permission in the app settings.",
            positiveButton = "Open Settings", isNegativeButton = false
        ) {
            if (it) {
                openAppSettings()
            }
        }
    }

    private fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        intent.setData("package:$packageName".toUri())
        startActivity(intent)
    }

    private fun sendToSettings() {
        val intent =
            Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, "package:$packageName".toUri())
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
        handlerSettingOverLay?.startPollingImeSettings()
        App.isOpenInter = true
        checkOverlay.launch(intent)
    }

    private fun ActivityHomeBinding.startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this@HomeActivity)

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            val targetRotation = currentScreenRotation
            val targetAspectRatio = getTargetAspectRatio()
            val preview = Preview.Builder().setTargetAspectRatio(targetAspectRatio)
                .setTargetRotation(targetRotation).build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }

            imageCapture =
                ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                    .setTargetAspectRatio(targetAspectRatio).setTargetRotation(targetRotation)
                    .build()

            val cameraSelector = if (isBackCameraSelected) {
                CameraSelector.DEFAULT_BACK_CAMERA
            } else {
                CameraSelector.DEFAULT_FRONT_CAMERA
            }

            try {
                cameraProvider.unbindAll()
                val recorder = Recorder.Builder().setQualitySelector(
                    QualitySelector.from(
                        Quality.FHD, FallbackStrategy.higherQualityOrLowerThan(Quality.HD)
                    )
                ).build()

                videoCapture = VideoCapture.withOutput(recorder)

                cameraProvider.bindToLifecycle(
                    this@HomeActivity, cameraSelector, preview, imageCapture, videoCapture
                )
                setupCameraControls(cameraProvider, cameraSelector)

            } catch (exc: Exception) {
                Toast.makeText(this@HomeActivity, "Failed to start camera", Toast.LENGTH_SHORT)
                    .show()
            }
        }, ContextCompat.getMainExecutor(this@HomeActivity))
    }

    private fun ActivityHomeBinding.setupCameraControls(
        cameraProvider: ProcessCameraProvider, cameraSelector: CameraSelector
    ) {
        actionFlashMode.setOnClickListener {
            if (isBackCameraSelected) {
                handleFlashToggle(cameraProvider, cameraSelector)
            }
        }

        actionChangeCamera.setOnClickListener {
            toggleCamera(cameraProvider)
        }
    }

    private fun ActivityHomeBinding.toggleCamera(cameraProvider: ProcessCameraProvider) {
        val newCameraSelector = if (isBackCameraSelected) {
            CameraSelector.DEFAULT_FRONT_CAMERA
        } else {
            CameraSelector.DEFAULT_BACK_CAMERA
        }

        val targetRotation = currentScreenRotation
        val targetAspectRatio = getTargetAspectRatio()

        try {
            cameraProvider.unbindAll()

            val preview = Preview.Builder().setTargetAspectRatio(targetAspectRatio)
                .setTargetRotation(targetRotation).build()
                .also { it.surfaceProvider = previewView.surfaceProvider }

            imageCapture =
                ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                    .setTargetAspectRatio(targetAspectRatio).setTargetRotation(targetRotation)
                    .build()
            val recorder = Recorder.Builder().setQualitySelector(
                QualitySelector.from(
                    Quality.FHD, FallbackStrategy.higherQualityOrLowerThan(Quality.HD)
                )
            ).build()

            videoCapture = VideoCapture.withOutput(recorder)
            cameraProvider.bindToLifecycle(
                this@HomeActivity, newCameraSelector, preview, imageCapture, videoCapture
            )

            updateFlashIcon()
            isBackCameraSelected = !isBackCameraSelected
        } catch (exc: Exception) {
            Toast.makeText(this@HomeActivity, "Failed to switch camera", Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleFlashToggle(
        cameraProvider: ProcessCameraProvider, cameraSelector: CameraSelector
    ) {
        try {
            val camera = cameraProvider.bindToLifecycle(this@HomeActivity, cameraSelector)

            if (camera.cameraInfo.hasFlashUnit()) {
                val cameraControl = camera.cameraControl
                val torchState = camera.cameraInfo.torchState.value
                val isTorchOn = torchState == TorchState.ON

                updateFlashIcon(isTorchOn)
                cameraControl.enableTorch(!isTorchOn)
            } else {
                Toast.makeText(this@HomeActivity, "Flash not available", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
        }
    }

    private fun ActivityHomeBinding.handleTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                if (isFocusManual) {
                    val adjustedX = event.x - focusView.width / 2f
                    val adjustedY = event.y - focusView.height / 2f
                    focusView.x =
                        adjustedX.coerceIn(0f, previewView.width - focusView.width.toFloat())
                    focusView.y =
                        adjustedY.coerceIn(0f, previewView.height - focusView.height.toFloat())
                    showFocusIconFor3Seconds()
                }
            }

            MotionEvent.ACTION_UP -> {
                focusView.beInvisible()
            }
        }
        return true
    }

    private fun ActivityHomeBinding.startFocusTimer() {
        lifecycleScope.launch(Dispatchers.Main) {
            while (true) {
                if (!isFocusManual) {
                    focusView.beVisible()
                    focusView.x = (previewView.width / 2f) - (focusView.width / 2f)
                    focusView.y = (previewView.height / 2f) - (focusView.height / 2f)
                    delay(3000)
                    focusView.beInvisible()
                }
                delay(8000)
            }
        }
    }

    private fun ActivityHomeBinding.toggleFocusMode() {
        isFocusManual = !isFocusManual
        if (isFocusManual) {
            isFirstClick = true
            actionFocusMode.iconTint = ColorStateList.valueOf(color(R.color.colorWhite))
            focusView.setFocusShape(FocusView.FOCUS_SHAPE_SQUARE)
            actionFocusMode.text = getString(R.string.action_manual_focus)
            focusView.setManualFocus(true)
            showFocusIconFor3Seconds()
        } else {
            focusView.visibility = View.VISIBLE
            isFirstClick = true
            actionFocusMode.iconTint = ColorStateList.valueOf(color(R.color.colorAccent))
            focusView.setFocusShape(FocusView.FOCUS_SHAPE_CIRCLE)
            actionFocusMode.text = getString(R.string.action_auto_focus)
            focusView.setManualFocus(false)
        }
    }

    private fun ActivityHomeBinding.showFocusIconFor3Seconds() {
        focusView.beVisible()
        focusView.animate().scaleX(1.2f).scaleY(1.2f).setDuration(150).withEndAction {
            focusView.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
        }.start()

        lifecycleScope.launch(Dispatchers.Main) {
            delay(3000)
            focusView.animate().alpha(0f).setDuration(300).withEndAction {
                focusView.beInvisible()
                focusView.alpha = 1f
            }.start()
        }
    }

    private fun ActivityHomeBinding.handleCaptureClick() {
        when (actionTimer.text) {
            getString(R.string.action_timer_3sec) -> handleTimer3SecondCapture()
            getString(R.string.action_timer_5sec) -> handleTimer5SecondCapture()
            else -> handleImmediateCapture()
        }
    }

    private fun ActivityHomeBinding.handleTimer3SecondCapture() {
        threeToOneAnimationView.beVisible()
        threeToOneAnimationView.playAnimation()
        threeToOneAnimationView.addAnimatorListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                threeToOneAnimationView.clearAnimation()
                if (isSoundOn) playCameraSound()
                captureImageWithOverlays()
            }
        })
    }

    private fun ActivityHomeBinding.handleTimer5SecondCapture() {
        fiveToOneAnimationView.beVisible()
        fiveToOneAnimationView.playAnimation()
        fiveToOneAnimationView.addAnimatorListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                fiveToOneAnimationView.clearAnimation()
                if (isSoundOn) playCameraSound()
                captureImageWithOverlays()
            }
        })
    }

    private fun ActivityHomeBinding.handleImmediateCapture() {
        if (isSoundOn) playCameraSound()
        captureImageWithOverlays()
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun ActivityHomeBinding.captureImageWithOverlays() {
        val imageCapture = imageCapture ?: run {
            Toast.makeText(this@HomeActivity, "Camera not ready", Toast.LENGTH_SHORT).show()
            return
        }

        val photoFile = createPhotoFile()
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        if (actionTimer.text != getString(R.string.action_timer_3sec) && actionTimer.text != getString(
                R.string.action_timer_5sec
            )
        ) {
            progressBarAnimation.beVisible()
            progressBarAnimation.playAnimation()
        }

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this@HomeActivity),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    val options = BitmapFactory.Options()
                    options.inJustDecodeBounds = true
                    BitmapFactory.decodeFile(photoFile.absolutePath, options)
                    processAndSaveImageWithOverlays(photoFile)
                }

                override fun onError(exception: ImageCaptureException) {
                    runOnUiThread {
                        hideProgressAnimations()
                        Toast.makeText(
                            this@HomeActivity,
                            "Capture failed: ${exception.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            })
    }

    private fun createPhotoFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val fileName = if (isPortraitMode) {
            "GPSMapCamera_Portrait_$timeStamp.jpg"
        } else {
            "GPSMapCamera_Landscape_$timeStamp.jpg"
        }
        val path = when (tinyDB?.getString("directory", "default")) {
            "default" -> "Camera"
            "site_1" -> "${getString(R.string.location_storage_directory)}${File.separator}Site 1"
            "site_2" -> "${getString(R.string.location_storage_directory)}${File.separator}Site 2"
            "site_3" -> "${getString(R.string.location_storage_directory)}${File.separator}Site 3"
            "site_4" -> "${getString(R.string.location_storage_directory)}${File.separator}Site 4"
            else -> "Camera"
        }
        return File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),
            "${path}${File.separator}$fileName"
        )
    }

    private fun getLatestCapturedFile(): File? {
        val directory = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),
            getString(R.string.location_storage_directory)
        )
        if (!directory.exists() || !directory.isDirectory) return null

        val files = directory.listFiles { file ->
            file.isFile && file.name.endsWith(".jpg", ignoreCase = true)
        } ?: return null

        return files.maxByOrNull { it.lastModified() }
    }

    @SuppressLint("MissingPermission")
    private fun ActivityHomeBinding.processAndSaveImageWithOverlays(photoFile: File) {
        try {
            val capturedBitmapOptions = BitmapFactory.Options().apply {
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val capturedBitmap =
                BitmapFactory.decodeFile(photoFile.absolutePath, capturedBitmapOptions)

            if (capturedBitmap == null) {
                handleCaptureError()
                return
            }
            val finalBitmap = if (isPortraitMode && capturedBitmap.width > capturedBitmap.height) {
                rotateBitmap(capturedBitmap)
            } else {
                capturedBitmap
            }

            val mapFragment = getActiveMapFragment()
            mapFragment?.getMapAsync { googleMap ->
                googleMap.snapshot { mapBitmap ->
                    try {
                        val parentOverlayBitmap = createParentOverlayBitmap()

                        if (mapBitmap != null) {
                            val mapCardLeft = mapCardView.left.toFloat()
                            val mapCardTop = mapCardView.top.toFloat()
                            val mapCardWidth = mapCardView.width
                            val mapCardHeight = mapCardView.height

                            val scaledMap = if (mapCardWidth > 0 && mapCardHeight > 0) {
                                mapBitmap.scale(mapCardWidth, mapCardHeight)
                            } else {
                                mapBitmap
                            }

                            val radiusPx = resources.getDimension(com.intuit.sdp.R.dimen._8sdp)

                            val roundedMap = createRoundedMapBitmap(scaledMap, radiusPx)
                            if (scaledMap != mapBitmap) scaledMap.recycle()

                            val overlayCanvas = Canvas(parentOverlayBitmap)
                            overlayCanvas.drawBitmap(
                                roundedMap, mapCardLeft, mapCardTop, Paint(Paint.ANTI_ALIAS_FLAG)
                            )
                            roundedMap.recycle()
                        }
                        mapBitmap?.recycle()
                        val marginDp = 20f
                        val marginPx = TypedValue.applyDimension(
                            TypedValue.COMPLEX_UNIT_DIP, marginDp, resources.displayMetrics
                        )
                        val targetWidth = (finalBitmap.width - (marginPx * 2)).toInt()
                        val targetHeight = (finalBitmap.height * 0.20f).toInt().coerceAtLeast(120)

                        val scaledOverlay =
                            if (parentOverlayBitmap.width == targetWidth && parentOverlayBitmap.height == targetHeight) {
                                parentOverlayBitmap
                            } else {
                                parentOverlayBitmap.scale(targetWidth, targetHeight)
                            }

                        if (scaledOverlay != parentOverlayBitmap) parentOverlayBitmap.recycle()

                        val combinedBitmap = createBitmap(finalBitmap.width, finalBitmap.height)
                        val combinedCanvas = Canvas(combinedBitmap)
                        combinedCanvas.drawBitmap(finalBitmap, 0f, 0f, null)

                        val overlayX = marginPx
                        val overlayY = finalBitmap.height - targetHeight - marginPx

                        combinedCanvas.drawBitmap(
                            scaledOverlay, overlayX, overlayY, null
                        )
                        scaledOverlay.recycle()

                        val saveSuccess = saveCombinedBitmap(combinedBitmap, photoFile)
                        combinedBitmap.recycle()
                        if (finalBitmap != capturedBitmap) finalBitmap.recycle()
                        capturedBitmap.recycle()

                        runOnUiThread {
                            hideProgressAnimations()
                            val message = if (saveSuccess) {
                                "Portrait image saved with GPS data"
                            } else {
                                "Image saved without GPS data"
                            }

                            Toast.makeText(this@HomeActivity, message, Toast.LENGTH_SHORT).show()
                            if (saveSuccess) {
                                broadcastMediaScan(photoFile)
                                Glide.with(this@HomeActivity).load(photoFile).into(imageCaptured)
                                lastCapturedFile = photoFile
                            }
                        }
                    } catch (e: Exception) {
                        val fallbackSuccess = saveCombinedBitmap(finalBitmap, photoFile)
                        if (finalBitmap != capturedBitmap) finalBitmap.recycle()
                        capturedBitmap.recycle()

                        runOnUiThread {
                            hideProgressAnimations()
                            Toast.makeText(
                                this@HomeActivity,
                                if (fallbackSuccess) "Image saved (map failed)" else "Save failed",
                                Toast.LENGTH_SHORT
                            ).show()
                            if (fallbackSuccess) broadcastMediaScan(photoFile)
                        }
                    }
                }
            }

        } catch (e: Exception) {
            handleCaptureError()
        }
    }

    private fun createRoundedMapBitmap(sourceBitmap: Bitmap, radius: Float): Bitmap {
        val width = sourceBitmap.width
        val height = sourceBitmap.height
        val finalBitmap = createBitmap(width, height)
        val canvas = Canvas(finalBitmap)

        val contentWidth = (width - 0F * 2).toInt().coerceAtLeast(1)
        val contentHeight = (height - 0F * 2).toInt().coerceAtLeast(1)

        val scaledContent = sourceBitmap.scale(contentWidth, contentHeight)
        val contentLeft = 0F
        val rect = RectF(0f, 0f, width.toFloat(), height.toFloat())
        val roundedRect = android.graphics.Path().apply {
            addRoundRect(rect, radius, radius, android.graphics.Path.Direction.CW)
        }
        canvas.clipPath(roundedRect)
        canvas.drawColor(Color.WHITE)
        canvas.drawBitmap(scaledContent, contentLeft, 0F, Paint(Paint.ANTI_ALIAS_FLAG))

        scaledContent.recycle()
        return finalBitmap
    }

    private fun rotateBitmap(source: Bitmap): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(90F)

        val rotatedBitmap = Bitmap.createBitmap(
            source, 0, 0, source.width, source.height, matrix, true
        )
        return rotatedBitmap
    }

    private fun ActivityHomeBinding.createParentOverlayBitmap(): Bitmap {
        updateOverlayTextValues()
        layoutMapData.invalidate()
        layoutMapData.requestLayout()
        val width = layoutMapData.width
        val height = layoutMapData.height
        if (width <= 0 || height <= 0) {
            return createTransparentBitmap()
        }
        val bitmap = createBitmap(width, height)
        val canvas = Canvas(bitmap)
        val originalLayerType = layoutMapData.layerType
        layoutMapData.setLayerType(View.LAYER_TYPE_SOFTWARE, null)
        layoutMapData.draw(canvas)
        layoutMapData.setLayerType(originalLayerType, null)
        return bitmap
    }

    private fun createTransparentBitmap(): Bitmap {
        return createBitmap(1, 1).apply {
            val canvas = Canvas(this)
            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
        }
    }

    private fun startVideoRecording() {
        if (!::videoCapture.isInitialized) {
            Toast.makeText(this, "Video not ready", Toast.LENGTH_SHORT).show()
            return
        }

        val videoFile = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),
            "VID_${System.currentTimeMillis()}.mp4"
        )

        val outputOptions = FileOutputOptions.Builder(videoFile).build()

        activeRecording = videoCapture.output.prepareRecording(this, outputOptions).apply {
            if (allowAudio && hasPermission(Manifest.permission.RECORD_AUDIO)) {
                withAudioEnabled()
            }
        }.start(ContextCompat.getMainExecutor(this)) { event ->
            when (event) {
                is VideoRecordEvent.Start -> {
                    isRecording = true
                    runOnUiThread { updateVideoUI(true) }
                }

                is VideoRecordEvent.Finalize -> {
                    isRecording = false
                    runOnUiThread { updateVideoUI(false) }

                    if (!event.hasError()) {
                        isRecording = false

                        if (!event.hasError()) {
                            val inputFile = videoFile
                            val outputFile = File(inputFile.parent, "FINAL_${inputFile.name}")

                            processVideoWithOverlay(
                                inputFile = inputFile,
                                outputFile = outputFile,
                                onSuccess = {
                                    runOnUiThread {
                                        Toast.makeText(
                                            this, "Overlay video saved", Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                },
                                onError = { e ->
                                    Log.e(
                                        "startVideoRecording:", "Overlay failed: ${e.message}"
                                    )
                                    runOnUiThread {
                                        Toast.makeText(
                                            this, "Overlay failed: ${e.message}", Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                })
                        }
                    }
                }
            }
        }
    }

    private fun updateVideoUI(recording: Boolean) {
        binding?.apply {
            if (recording) {
                actionCapture.icon = drawable(R.drawable.ic_action_record_stop_button)
                tabCaptureMode.isEnabled = false
            } else {
                actionCapture.icon = drawable(R.drawable.ic_action_record_button)
                tabCaptureMode.isEnabled = true
            }
        }
    }

    private fun stopVideoRecording() {
        activeRecording?.stop()
        activeRecording = null
    }

    @SuppressLint("MissingPermission")
    private fun updateOverlayTextValues() {
        try {
            if (hasLocationPermission()) {
                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    location?.let { loc ->
                        getSelectedLocation(loc)?.let {
                            updateLocationUI(it)
                        }
                    }
                }
            }
        } catch (e: Exception) {
        }
    }

    private fun saveCombinedBitmap(bitmap: Bitmap, file: File): Boolean {
        return try {
            file.parentFile?.mkdirs() ?: run {
                return false
            }

            val fos = FileOutputStream(file)
            val success = bitmap.compress(Bitmap.CompressFormat.JPEG, 95, fos)
            fos.flush()
            fos.close()
            success
        } catch (e: Exception) {
            false
        }
    }

    private fun handleCaptureError() {
        runOnUiThread {
            hideProgressAnimations()
            Toast.makeText(this@HomeActivity, "Failed to capture image", Toast.LENGTH_SHORT).show()
        }
    }

    private fun hideProgressAnimations() {
        binding?.apply {
            progressBarAnimation.visibility = View.INVISIBLE
            threeToOneAnimationView.visibility = View.INVISIBLE
            fiveToOneAnimationView.visibility = View.INVISIBLE
        }
    }

    private fun broadcastMediaScan(photoFile: File) {
        try {
            val intent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
            intent.data = Uri.fromFile(photoFile)
            sendBroadcast(intent)

        } catch (e: Exception) {
        }
    }

    @SuppressLint("MissingPermission")
    private fun displayCurrentLocation() {
        if (!hasLocationPermission()) {
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                updateLocationUI(location)
            }
        }
        val mapFragment = getActiveMapFragment()
        mapFragment?.getMapAsync { googleMap ->
            updateMapMarker(googleMap)
            captureMapSnapshotSafe()
        }
    }

    @SuppressLint("MissingPermission", "DefaultLocale")
    private fun updateMapMarker(googleMap: GoogleMap) {
        if (!hasLocationPermission()) return

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            location?.let { loc ->
                val selectedLoc = getSelectedLocation(loc) ?: return@let
                val latLng = LatLng(selectedLoc.latitude, selectedLoc.longitude)
                googleMap.clear()

                val markerOptions =
                    MarkerOptions().position(latLng).title("Current Location").snippet(
                        "Lat: ${String.format("%.6f", loc.latitude)}, Lng: ${
                            String.format(
                                "%.6f", loc.longitude
                            )
                        }"
                    ).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))

                val marker = googleMap.addMarker(markerOptions)
                marker?.showInfoWindow()
                val cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, 16f)
                googleMap.animateCamera(cameraUpdate)
            }
        }
    }

    fun Context.isLocationEnabled(): Boolean {
        try {
            return Settings.Secure.getInt(contentResolver, "location_mode") != 0
        } catch (e: Settings.SettingNotFoundException) {
            e.printStackTrace()
            return false
        }
    }

    private fun checkAndPromptLocationSettings() {
        if (!isLocationEnabled()) {
            val locationRequest = LocationRequest.create().apply {
                interval = 10000
                fastestInterval = 5000
                priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            }
            val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
            val client = LocationServices.getSettingsClient(this@HomeActivity)
            val task: Task<LocationSettingsResponse> = client.checkLocationSettings(builder.build())
            task.addOnFailureListener { exception ->
                if (exception is ResolvableApiException) {
                    try {
                        exception.startResolutionForResult(
                            this@HomeActivity, LOCATION_SETTINGS_REQUEST
                        )
                    } catch (_: IntentSender.SendIntentException) {
                    }
                }
            }
        }
    }

    private fun getSelectedLocation(fallback: Location? = null): Location? {
        return when (locationMode) {
            LocationMode.CUSTOM -> customLocation
            LocationMode.CURRENT -> fallback
        }
    }

    private fun updateMapWithSelectedLocation() {
        val mapFragment = getActiveMapFragment()

        mapFragment?.getMapAsync { googleMap ->
            val loc = getSelectedLocation() ?: return@getMapAsync

            val latLng = LatLng(loc.latitude, loc.longitude)
            googleMap.clear()
            googleMap.addMarker(
                MarkerOptions().position(latLng).title(
                    if (locationMode == LocationMode.CUSTOM) "Custom Location"
                    else "Current Location"
                )
            )
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16f))
        }
    }

    @SuppressLint("DefaultLocale")
    private fun updateLocationUI(location: Location) {
        try {
            binding?.apply {
                updateMapWithSelectedLocation()
                getAddress(location.latitude, location.longitude) { address ->
                    val parts = mutableListOf<String>()
                    address.subThoroughfare?.takeIf { it.isNotBlank() }?.let { parts.add(it) }
                    address.thoroughfare?.takeIf { it.isNotBlank() }?.let { parts.add(it) }
                    address.subLocality?.takeIf { it.isNotBlank() }?.let { parts.add(it) }
                    address.locality?.takeIf { it.isNotBlank() }?.let { parts.add(it) }
                    address.adminArea?.takeIf { it.isNotBlank() }?.let { parts.add(it) }
                    address.postalCode?.takeIf { it.isNotBlank() }?.let { parts.add(it) }
                    address.countryName?.takeIf { it.isNotBlank() }?.let { parts.add(it) }

                    val fullAddress = parts.joinToString(", ")
                    textAddress.text = if (address.getAddressLine(0).isEmpty()) {
                        fullAddress
                    } else {
                        address.getAddressLine(0)?.takeIf { it.isNotBlank() } ?: "Unknown location"
                    }
                    updateOverlayState(location, fullAddress)
                    textAddressClassic.text = if (address.getAddressLine(0).isEmpty()) {
                        fullAddress
                    } else {
                        address.getAddressLine(0)?.takeIf { it.isNotBlank() } ?: "Unknown location"
                    }
                    textAddressSquarise.text = if (address.getAddressLine(0).isEmpty()) {
                        fullAddress
                    } else {
                        address.getAddressLine(0)?.takeIf { it.isNotBlank() } ?: "Unknown location"
                    }
                }

                textLatitudeValue.text = String.format("%.6f", location.latitude)
                textLatitudeValueSquarise.text = String.format("%.6f", location.latitude)
                textLatitudeValueClassic.text = String.format("%.6f", location.latitude)
                textLongitudeValue.text = String.format("%.6f", location.longitude)
                textLongitudeValueSquarise.text = String.format("%.6f", location.longitude)
                textLongitudeValueClassic.text = String.format("%.6f", location.longitude)

                textAltitude.text = "Altitude: ${
                    if (location.hasAltitude()) String.format(
                        "%.1f m", location.altitude
                    ) else "N/A"
                }"
                textAltitudeSquarise.text = "Altitude: ${
                    if (location.hasAltitude()) String.format(
                        "%.1f m", location.altitude
                    ) else "N/A"
                }"
                textAltitudeClassic.text = "Altitude: ${
                    if (location.hasAltitude()) String.format(
                        "%.1f m", location.altitude
                    ) else "N/A"
                }"

                val currentDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
                val currentDay = SimpleDateFormat("EEEE", Locale.getDefault()).format(Date())
                textDate.text = "$currentDay, $currentDate"
                textDateSquarise.text = "$currentDay, $currentDate"
                textDateClassic.text = "$currentDay, $currentDate"
            }
        } catch (e: Exception) {
        }
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        if (!hasLocationPermission()) return
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 15000)
            .setWaitForAccurateLocation(false).setMinUpdateIntervalMillis(10000).build()
        fusedLocationClient.requestLocationUpdates(
            locationRequest, locationCallback, Looper.getMainLooper()
        ).addOnCompleteListener { _ -> }
    }

    private fun stopLocationUpdates() {
        try {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        } catch (e: Exception) {
        }
    }

    private fun hasLocationPermission(): Boolean {
        val fineGranted = hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)
        val coarseGranted = hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
        return fineGranted && coarseGranted
    }

    private fun ActivityHomeBinding.handleTimerClick(clickCount: Int) = when (clickCount % 3) {
        1 -> {
            actionTimer.iconTint = ColorStateList.valueOf(color(R.color.colorAccent))
            actionTimer.text = getString(R.string.action_timer_3sec)
            saveTimerState(R.string.action_timer_3sec, R.color.colorAccent)
        }

        2 -> {
            actionTimer.iconTint = ColorStateList.valueOf(color(R.color.colorAccent))
            actionTimer.text = getString(R.string.action_timer_5sec)
            saveTimerState(R.string.action_timer_5sec, R.color.colorAccent)
        }

        else -> {
            actionTimer.iconTint = ColorStateList.valueOf(color(R.color.colorWhite))
            actionTimer.text = getString(R.string.action_timer_off)
            saveTimerState(R.string.action_timer_off, R.color.colorWhite)
        }
    }

    private fun ActivityHomeBinding.toggleSound() {
        isSoundOn = !isSoundOn
        if (isSoundOn) {
            actionSound.iconTint = ColorStateList.valueOf(color(R.color.colorAccent))
            actionSound.icon = drawable(R.drawable.ic_action_sound_on)
            actionSound.text = getString(R.string.action_sound_on)
        } else {
            actionSound.iconTint = ColorStateList.valueOf(color(R.color.colorWhite))
            actionSound.icon = drawable(R.drawable.ic_action_sound_off)
            actionSound.text = getString(R.string.action_sound_off)
        }
        saveSoundState(isSoundOn)
    }

    private fun updateFlashIcon(isTorchOn: Boolean = false) {
        binding?.apply {
            if (!isBackCameraSelected) {
                actionChangeCamera.icon = drawable(R.drawable.ic_action_camera_change)
                return
            }

            if (isTorchOn) {
                actionFlashMode.icon = drawable(R.drawable.ic_action_flash_off)
            } else {
                actionFlashMode.icon = drawable(R.drawable.ic_action_flash_on)
            }
        }
    }

    private fun ActivityHomeBinding.restoreTimerState() {
        val sharedPref = getSharedPreferences("timer_state", MODE_PRIVATE)
        val timerState = sharedPref.getInt("timer_state_key", R.string.action_timer_off)
        val iconResId = sharedPref.getInt("timer_icon_key", R.color.colorWhite)
        actionTimer.iconTint = ColorStateList.valueOf(color(iconResId))
        actionTimer.text = getString(timerState)
    }

    private fun ActivityHomeBinding.restoreSoundState() {
        val sharedPref = getSharedPreferences("sound_state", MODE_PRIVATE)
        isSoundOn = sharedPref.getBoolean("is_sound_on", true)
        if (isSoundOn) {
            actionSound.iconTint = ColorStateList.valueOf(color(R.color.colorAccent))
            actionSound.icon = drawable(R.drawable.ic_action_sound_on)
            actionSound.text = getString(R.string.action_sound_on)
        } else {
            actionSound.iconTint = ColorStateList.valueOf(color(R.color.colorWhite))
            actionSound.icon = drawable(R.drawable.ic_action_sound_off)
            actionSound.text = getString(R.string.action_sound_off)
        }
    }

    private fun restoreUIState() {
        binding?.apply {
            actionChangeCamera.icon = drawable(R.drawable.ic_action_camera_change)
            if (isBackCameraSelected) {
                actionFlashMode.icon = drawable(R.drawable.ic_action_flash_off)
            } else {
                actionFlashMode.icon = drawable(R.drawable.ic_action_flash_on)
            }
            restoreTimerState()
            restoreSoundState()
        }
        updateMapTypeFromTemplate(getSelectedMapType())
    }

    private fun getSelectedMapType(): Int {
        val sharedPreferences = getSharedPreferences("MapPreferences", MODE_PRIVATE)
        return sharedPreferences.getInt("SelectedMapType", GoogleMap.MAP_TYPE_NORMAL)
    }

    private fun updateMapTypeFromTemplate(mapType: Int) {
        val mapFragment = getActiveMapFragment()
        mapFragment?.getMapAsync { googleMap ->
            googleMap.mapType = mapType
        }
    }

    private fun checkGooglePlayServices() {
        val availability = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this)
        if (availability != ConnectionResult.SUCCESS) {
            GoogleApiAvailability.getInstance().getErrorDialog(this, availability, 0)?.show()
        }
    }

    private fun handlePermissionResults(
        permissionList: Array<out String>, grantResults: IntArray, requestCode: Int
    ) {
        if (requestCode != PERMISSION_REQUEST_CODE) return

        var cameraGranted = false
        var locationGranted = false

        permissionList.forEachIndexed { index, permission ->
            if (grantResults.getOrNull(index) != PackageManager.PERMISSION_GRANTED) return@forEachIndexed

            when (permission) {
                Manifest.permission.CAMERA -> cameraGranted = true
                Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION -> locationGranted =
                    true
            }
        }

        if (cameraGranted && locationGranted) {

            checkAndPromptLocationSettings()
            binding?.startCamera()
            displayCurrentLocation()

            when {
                !hasPermissions(AFTER_CALL_PERMISSION) -> permissions.launch(AFTER_CALL_PERMISSION)
                !isGrantedOverlay() -> sendToSettings()
            }

            Toast.makeText(this, "Camera & GPS ready", Toast.LENGTH_SHORT).show()

        } else {
            Toast.makeText(
                this, "Camera and GPS permissions required", Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun ActivityHomeBinding.updateTimeDisplay() {
        val localTime = SimpleDateFormat("HH:mm:ss a", Locale.getDefault()).format(Date())
        textLocalTime.text = "Local: $localTime"
        textLocalTimeSquarise.text = "Local: $localTime"
        textLocalTimeClassic.text = "Local: $localTime"

        val gmtFormat = SimpleDateFormat("HH:mm:ss a", Locale.getDefault())
        gmtFormat.timeZone = TimeZone.getTimeZone("GMT")
        val gmtTime = gmtFormat.format(Date())
        textGmtTime.text = "GMT: $gmtTime"
        textGmtTimeSquarise.text = "GMT: $gmtTime"
        textGmtTimeClassic.text = "GMT: $gmtTime"
    }

    private fun saveTimerState(timerState: Int, iconResId: Int) {
        val sharedPref = getSharedPreferences("timer_state", MODE_PRIVATE)
        sharedPref.edit {
            putInt("timer_state_key", timerState)
            putInt("timer_icon_key", iconResId)
        }
    }

    private fun saveSoundState(isSoundOn: Boolean) {
        val sharedPref = getSharedPreferences("sound_state", MODE_PRIVATE)
        sharedPref.edit {
            putBoolean("is_sound_on", isSoundOn)
        }
    }

    @Suppress("DEPRECATION")
    private fun getAddress(latitude: Double, longitude: Double, callback: (Address) -> Unit) {
        val geocoder = Geocoder(this, Locale.getDefault())

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            geocoder.getFromLocation(latitude, longitude, 1, object : Geocoder.GeocodeListener {
                override fun onGeocode(addresses: MutableList<Address>) {
                    val address = addresses.firstOrNull() ?: Address(Locale.getDefault()).apply {
                        locality = "Unknown"
                        adminArea = "Unknown"
                        countryName = "Unknown"
                    }
                    callback(address)
                }

                override fun onError(errorMessage: String?) {
                    callback(Address(Locale.getDefault()).apply {
                        locality = "Unknown"
                        adminArea = "Unknown"
                        countryName = "Unknown"
                    })
                }
            })
        } else {
            try {
                val addresses = geocoder.getFromLocation(latitude, longitude, 1)
                val address = addresses?.firstOrNull() ?: Address(Locale.getDefault()).apply {
                    locality = "Unknown"
                    adminArea = "Unknown"
                    countryName = "Unknown"
                }
                callback(address)
            } catch (e: Exception) {
                Log.e("Geocoder", "Geocoding failed: ${e.message}")
                callback(Address(Locale.getDefault()).apply {
                    locality = "Unknown"
                    adminArea = "Unknown"
                    countryName = "Unknown"
                })
            }
        }
    }

    private fun playCameraSound() {
        try {
            MediaPlayer.create(this, R.raw.camera_shutter_6305)?.apply {
                start()
                setOnCompletionListener { mp ->
                    mp?.release()
                }
            }
        } catch (e: Exception) {
        }
    }

    override fun ActivityHomeBinding.initListeners() {
        var clickCount = 0
        restoreSoundState()
        actionFocusMode.setOnClickListener {
            toggleFocusMode()
        }
        actionSound.setOnClickListener {
            toggleSound()
        }
        actionTimer.setOnClickListener {
            clickCount++
            handleTimerClick(clickCount)
        }
        actionCapture.setOnClickListener {
            when (captureMode) {
                CaptureMode.PHOTO -> handleCaptureClick()
                CaptureMode.VIDEO -> toggleVideoRecording()
            }
        }
        imageCaptured.setOnClickListener {
            lastCapturedFile?.let { file ->
                if (file.exists()) {
                    val uri = FileProvider.getUriForFile(
                        this@HomeActivity, "${applicationContext.packageName}.provider", file
                    )
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(uri, "image/*")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    startActivity(intent)
                } else {
                    Toast.makeText(this@HomeActivity, "No image available", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }
        actionTemplates.setOnClickListener {
            stopLocationUpdates()
            timeHandler.removeCallbacks(timeRunnable)
            go(TemplatesActivity::class.java)
        }
        actionTools.setOnClickListener {
            stopLocationUpdates()
            timeHandler.removeCallbacks(timeRunnable)
            go(ToolsActivity::class.java)
        }
        actionMapData.setOnClickListener {
            stopLocationUpdates()
            timeHandler.removeCallbacks(timeRunnable)
            locationUpdateResult.launch(goResult(LocationsActivity::class.java))
        }
        actionSettings.setOnClickListener {
            stopLocationUpdates()
            timeHandler.removeCallbacks(timeRunnable)
            go(AppSettingsActivity::class.java)
        }
    }

    private fun toggleVideoRecording() {
        if (isRecording) {
            binding?.actionChangeCamera?.disable()
            stopVideoRecording()
        } else {
            binding?.actionChangeCamera?.enable()
            startVideoRecording()
        }
    }

    override fun ActivityHomeBinding.initView() {
        updateStatusBarColor(R.color.colorTransparent)
        updateNavigationBarColor(R.color.colorTransparent)
        layoutTopController.setOnApplyWindowInsetsListener { v: View, insets: WindowInsets ->
            v.setPadding(0, statusBarHeight, 0, 0)
            insets
        }
        onBackPressedDispatcher.addCallback {
            if (doubleBackToExitPressedOnce) {
                finishAffinity()
                return@addCallback
            }

            doubleBackToExitPressedOnce = true
            Toast.makeText(this@HomeActivity, "Please click BACK again to exit", Toast.LENGTH_SHORT)
                .show()
            Handler(Looper.getMainLooper()).postDelayed(Runnable {
                doubleBackToExitPressedOnce = false
            }, 2000)
        }
    }

    fun invokeSetupWizardOfThisIme() {
        handlerSettingOverLay?.cancelPollingImeSettings()
        val intent = Intent()
        intent.setClass(this@HomeActivity, HomeActivity::class.java)
        intent.flags = 606076928
        startActivity(intent)
    }
}