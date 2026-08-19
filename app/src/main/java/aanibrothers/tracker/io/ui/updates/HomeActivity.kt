package aanibrothers.tracker.io.ui.updates

import aanibrothers.tracker.io.App.Companion.appOpenManager
import aanibrothers.tracker.io.R
import aanibrothers.tracker.io.databinding.ActivityHomeBinding
import aanibrothers.tracker.io.databinding.LayoutSheetExitBinding
import aanibrothers.tracker.io.extension.lastRatePromptDay
import aanibrothers.tracker.io.extension.viewPermission
import aanibrothers.tracker.io.helper.CapturedPhotos
import aanibrothers.tracker.io.helper.EdgeToEdgeHandled
import aanibrothers.tracker.io.helper.applySystemBarPadding
import aanibrothers.tracker.io.helper.InAppReviewListener
import aanibrothers.tracker.io.helper.PaintOverlayRenderer
import aanibrothers.tracker.io.helper.launchInAppReviewFlow
import aanibrothers.tracker.io.helper.viewRateDialog
import aanibrothers.tracker.io.locations.LocationPreference
import aanibrothers.tracker.io.model.CaptureMode
import aanibrothers.tracker.io.model.LocationMode
import aanibrothers.tracker.io.model.OverlayState
import aanibrothers.tracker.io.model.OverlayTemplate
import aanibrothers.tracker.io.module.AppOpenManager
import aanibrothers.tracker.io.module.viewNativeSmall
import aanibrothers.tracker.io.ui.AppSettingsActivity
import aanibrothers.tracker.io.ui.ToolsActivity
import aanibrothers.tracker.io.ui.ReportActivity
import aanibrothers.tracker.io.ui.ViewCollectionActivity
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
import android.view.KeyEvent
import android.util.Size
import android.view.Gravity
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.ViewConfiguration
import android.view.OrientationEventListener
import android.view.Surface
import android.view.View
import android.view.ViewTreeObserver
import android.view.WindowInsets
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.annotation.StringRes
import androidx.camera.core.AspectRatio
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.core.TorchState
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FallbackStrategy
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.content.edit
import androidx.core.graphics.createBitmap
import androidx.core.graphics.scale
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.core.view.marginTop
import androidx.exifinterface.media.ExifInterface
import androidx.lifecycle.Lifecycle
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
import coder.apps.space.library.extension.applyDialogConfig
import coder.apps.space.library.extension.beInvisible
import coder.apps.space.library.extension.beGone
import coder.apps.space.library.extension.beVisible
import coder.apps.space.library.extension.color
import coder.apps.space.library.extension.disable
import coder.apps.space.library.extension.drawable
import coder.apps.space.library.extension.enable
import coder.apps.space.library.extension.go
import coder.apps.space.library.extension.goResult
import coder.apps.space.library.extension.hasPermission
import coder.apps.space.library.extension.statusBarHeight
import coder.apps.space.library.helper.TinyDB
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
import com.google.android.gms.maps.GoogleMapOptions
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.tasks.Task
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.math.roundToInt

class HomeActivity : BaseActivity<ActivityHomeBinding>(
    ActivityHomeBinding::inflate, isFullScreen = true, isFullScreenIncludeNav = false
), EdgeToEdgeHandled {
    private var exitSheetDialog: BottomSheetDialog? = null
    private val PERMISSION_REQUEST_CODE = 100
    private var cameraProvider: ProcessCameraProvider? = null

    // Bound camera handle — needed for tap-to-focus and torch. Refreshed on
    // every bindToLifecycle() so the control always points at the live camera.
    private var camera: Camera? = null

    // True once the use-cases are bound. CameraX follows this activity's
    // lifecycle and resumes the preview by itself, so onStart/onResume must
    // NOT rebind (that unbinds the surface and flashes the loader again).
    private var isCameraBound = false
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private var captureMode = CaptureMode.PHOTO
    private lateinit var videoCapture: VideoCapture<Recorder>
    private var activeRecording: Recording? = null
    private var isRecording = false

    private var isFocusManual = true
    private var focusHideJob: Job? = null

    // Vertical drag on the preview = exposure compensation, anchored to the
    // value that was live when the finger went down.
    private var exposureDragStartIndex = 0
    private var isExposureDragging = false
    private var touchDownX = 0f
    private var touchDownY = 0f
    private val touchSlop by lazy { ViewConfiguration.get(this).scaledTouchSlop }

    // Zoom chips are built per lens, so they are held here rather than in the
    // layout — the available ratios differ between the front and back camera.
    private val zoomChips = mutableListOf<TextView>()

    private val scaleGestureDetector by lazy {
        ScaleGestureDetector(this, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                val zoomState = camera?.cameraInfo?.zoomState?.value ?: return false
                val target = (zoomState.zoomRatio * detector.scaleFactor)
                    .coerceIn(zoomState.minZoomRatio, zoomState.maxZoomRatio)
                camera?.cameraControl?.setZoomRatio(target)
                updateZoomChips(target)
                return true
            }
        })
    }

    private var isFirstClick = true
    private var isSoundOn = true
    private var isBackCameraSelected = true

    private var lastCapturedFile: File? = null
    private var imageCapture: ImageCapture? = null
    private var currentOrientation = 0
    private var currentScreenRotation = Surface.ROTATION_0
    private var isPortraitMode = true
    private val LOCATION_SETTINGS_REQUEST = 4634

    @Volatile
    private var allowMapSnapshot = false

    private val timeHandler = Handler(Looper.getMainLooper())
    private lateinit var timeRunnable: Runnable

    private var orientationEventListener: OrientationEventListener? = null
    private lateinit var locationCallback: LocationCallback

    private val locationUpdateResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode != Activity.RESULT_OK) return@registerForActivityResult
            restoreLocationModeFromPref()
        }

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
        appOpenManager = AppOpenManager()
        requestPermissions()
        updateCameraPermissionUi()
        handleRate()
    }

    private var googleMap: GoogleMap? = null
    private val lastMapSnapshot = java.util.concurrent.atomic.AtomicReference<Bitmap?>()

    /** Container the active template draws its map tile into. */
    private fun activeMapContainerId(): Int {
        return when (tinyDB?.getString("template", "default")) {
            "classic" -> R.id.map_fragment_classic
            "squarise" -> R.id.map_fragment_squarise
            else -> R.id.map_fragment
        }
    }

    private fun getActiveMapFragment(): SupportMapFragment? {
        return supportFragmentManager.findFragmentById(activeMapContainerId()) as? SupportMapFragment
    }

    /**
     * Puts a map into the active template's container.
     *
     * The containers in the layout are plain FrameLayouts, and nothing ever
     * added a fragment to them — so [getActiveMapFragment] returned null on
     * every call and the map tile, the snapshot and the map type were all
     * dead. MapActivity has always done this transaction; this screen never
     * did. Same pattern as MapActivity, minus the gestures, since this map is
     * only ever a thumbnail that gets snapshotted into the stamp.
     */
    private fun ensureMapFragment(): SupportMapFragment? {
        getActiveMapFragment()?.let { return it }
        if (isFinishing || isDestroyed) return null

        val options = GoogleMapOptions()
            .compassEnabled(false)
            .zoomControlsEnabled(false)
            .mapToolbarEnabled(false)
            .scrollGesturesEnabled(false)
            .zoomGesturesEnabled(false)
            .rotateGesturesEnabled(false)
            .tiltGesturesEnabled(false)

        return try {
            val containerId = activeMapContainerId()
            val transaction = supportFragmentManager.beginTransaction()

            // Drop a map left behind in another template's container, so
            // switching templates doesn't accumulate live GoogleMap instances.
            listOf(R.id.map_fragment, R.id.map_fragment_classic, R.id.map_fragment_squarise)
                .filter { it != containerId }
                .mapNotNull { supportFragmentManager.findFragmentById(it) }
                .forEach { transaction.remove(it) }

            val fragment = SupportMapFragment.newInstance(options)
            transaction.replace(containerId, fragment)
                // Committed synchronously so the very next findFragmentById
                // (and the callers below) see it rather than racing the queue.
                .commitNowAllowingStateLoss()
            fragment
        } catch (exc: Exception) {
            Log.e(HomeActivity::class.java.simpleName, "Map fragment attach failed", exc)
            null
        }
    }

    private fun setupMapSnapshot() {
        val fragment = ensureMapFragment() ?: return
        fragment.getMapAsync { map ->
            googleMap = map
            map.mapType = getSelectedMapType()
            map.setOnMapLoadedCallback {
                if (canSnapshotMap()) {
                    captureMapSnapshotSafe()
                }
            }
        }
    }

    private fun canSnapshotMap(): Boolean {
        return allowMapSnapshot &&
                lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) &&
                !isFinishing && !isDestroyed &&
                window?.decorView?.isShown == true
    }

    private fun captureMapSnapshotSafe() {
        if (!canSnapshotMap()) return
        val fragment = getActiveMapFragment() ?: return
        fragment.getMapAsync { map ->
            val mapView = fragment.view ?: return@getMapAsync

            if (mapView.width == 0 || mapView.height == 0) {
                mapView.viewTreeObserver.addOnGlobalLayoutListener(object :
                    ViewTreeObserver.OnGlobalLayoutListener {
                    override fun onGlobalLayout() {
                        mapView.viewTreeObserver.removeOnGlobalLayoutListener(this)
                        if (canSnapshotMap()) {
                            captureMapSnapshotSafe()
                        }
                    }
                })
                return@getMapAsync
            }

            if (!canSnapshotMap()) return@getMapAsync
            try {
                map.snapshot { bmp ->
                    if (bmp != null && bmp.width > 0 && bmp.height > 0) {
                        lastMapSnapshot.getAndSet(bmp)?.recycle()
                    }
                }
            } catch (_: IllegalStateException) {
                // Ignore snapshots while backgrounded
            }
        }
    }


    fun ActivityHomeBinding.setupTab() {
        tabCaptureMode.apply {
            addTab(newTab().setText(getString(R.string.tab_photo)))
            addTab(newTab().setText(getString(R.string.tab_video)))
            addTab(newTab().setText(getString(R.string.action_report).uppercase()))

            addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
                override fun onTabSelected(tab: TabLayout.Tab) {
                    if (tab.position == 2) {
                        // REPORT is an action, not a capture mode: open the
                        // screen and snap the selection back to the active
                        // mode's tab so the camera state is untouched.
                        post { getTabAt(if (captureMode == CaptureMode.VIDEO) 1 else 0)?.select() }
                        go(ReportActivity::class.java)
                        return
                    }
                    val newMode = if (tab.position == 0) CaptureMode.PHOTO else CaptureMode.VIDEO
                    // The snap-back from REPORT re-selects the current mode's
                    // tab; skip the camera restart in that case.
                    if (newMode == captureMode) return
                    captureMode = newMode
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
            title = getString(R.string.title_microphone_permission),
            body = getString(R.string.body_microphone_permission),
            positiveButton = getString(R.string.button_allow),
            isNegativeButton = false,
            isIconShown = true
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
            title = getString(R.string.title_permission_required),
            body = message,
            positiveButton = getString(R.string.button_open_settings),
            isNegativeButton = false
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
                showPermissionBlockedDialog(getString(R.string.message_microphone_permission_required_for_audio))
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
                Toast.makeText(
                    this,
                    getString(R.string.toast_location_still_disabled),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        orientationEventListener?.enable()
        binding?.updateCameraPermissionUi()
        restoreLocationModeFromPref()
    }

    override fun onStop() {
        super.onStop()
        allowMapSnapshot = false
        orientationEventListener?.disable()
        stopLocationUpdates()
    }

    override fun onResume() {
        super.onResume()
        allowMapSnapshot = true
        restoreUIState()
        checkGooglePlayServices()
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        binding?.refreshLastCapturedThumbnail()
        binding?.updateCameraPermissionUi()
    }

    override fun onPause() {
        super.onPause()
        allowMapSnapshot = false
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
        binding?.bindCameraUseCases(cameraProvider)
    }

    /**
     * The single place use-cases are bound. Cold start, camera flip, the
     * photo/video switch and rotation changes all funnel through here, so
     * resolution and rotation settings can never drift apart between the
     * paths the way they used to.
     */
    private fun ActivityHomeBinding.bindCameraUseCases(provider: ProcessCameraProvider): Boolean {
        val targetRotation = currentScreenRotation
        val targetAspectRatio = getTargetAspectRatio()

        val preview = Preview.Builder()
            .setResolutionSelector(
                ResolutionSelector.Builder()
                    .setAspectRatioStrategy(aspectRatioStrategy(targetAspectRatio))
                    .build()
            )
            .setTargetRotation(targetRotation)
            .build()
            .also { it.surfaceProvider = previewView.surfaceProvider }

        imageCapture = buildImageCapture(targetAspectRatio, targetRotation)

        val recorder = Recorder.Builder().setQualitySelector(
            QualitySelector.from(
                Quality.FHD, FallbackStrategy.higherQualityOrLowerThan(Quality.HD)
            )
        ).build()
        videoCapture = VideoCapture.withOutput(recorder)

        val cameraSelector = if (isBackCameraSelected) {
            CameraSelector.DEFAULT_BACK_CAMERA
        } else {
            CameraSelector.DEFAULT_FRONT_CAMERA
        }

        return try {
            provider.unbindAll()
            camera = provider.bindToLifecycle(
                this@HomeActivity, cameraSelector, preview, imageCapture, videoCapture
            )
            cameraProvider = provider
            isCameraBound = true
            resetExposure()
            // Deferred a frame: CameraX fills in the zoom range as the camera
            // finishes opening, so reading it inline can come back empty.
            previewView.post { setupZoomControls() }
            true
        } catch (exc: Exception) {
            Log.e(HomeActivity::class.java.simpleName, "Camera bind failed", exc)
            isCameraBound = false
            false
        }
    }

    private fun aspectRatioStrategy(targetAspectRatio: Int): AspectRatioStrategy {
        return if (targetAspectRatio == AspectRatio.RATIO_16_9) {
            AspectRatioStrategy.RATIO_16_9_FALLBACK_AUTO_STRATEGY
        } else {
            AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY
        }
    }

    @SuppressLint("SetTextI18n")
    private fun initializeComponents() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this@HomeActivity)
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
        }, ContextCompat.getMainExecutor(this))

        // Preview frames arrive asynchronously after startCamera() binds. Drop
        // the loading overlay the moment the first frame is on screen —
        // without this the spinner sits on top of a live preview forever.
        binding?.previewView?.previewStreamState?.observe(this) { state ->
            if (state == PreviewView.StreamState.STREAMING) {
                binding?.previewLoadingOverlay?.visibility = View.GONE
            }
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        binding?.apply {
            val capturedBy = getString(R.string.format_captured_by, getString(R.string.app_name))
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
        val safeAddress = addressText.ifBlank { getString(R.string.label_unknown_location) }

        overlayState = OverlayState(
            address = safeAddress,
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
            // Pinch owns the gesture while two fingers are down; the focus and
            // brightness handling only ever sees single-finger events.
            scaleGestureDetector.onTouchEvent(event)
            if (!scaleGestureDetector.isInProgress && event.pointerCount == 1) {
                handleTouchEvent(event)
            }
            return@setOnTouchListener true
        }
        // The widget draws a fixed 150px ring by default, which clips on
        // low-density screens. Scale it to whatever the view actually got.
        focusView.post {
            if (focusView.width > 0) {
                focusView.setFocusSize((focusView.width * 0.72f).toInt())
            }
        }
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
    }

    private fun requiredCameraPermissions(): Array<String> {
        return arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    }

    private fun hasCameraLocationPermissions(): Boolean {
        return requiredCameraPermissions().all { hasPermission(it) }
    }

    private fun requestCameraLocationPermissions() {
        val deniedCount = fetchPermissionsDeniedCount("PERMISSION_CAMERA_LOCATION")
        if (deniedCount >= 2) {
            openAppSettings()
            return
        }
        ActivityCompat.requestPermissions(
            this,
            requiredCameraPermissions(),
            PERMISSION_REQUEST_CODE
        )
    }

    private fun ActivityHomeBinding.updateCameraPermissionUi() {
        val hasAllPermissions = hasCameraLocationPermissions()
        permissionLayout.isVisible = !hasAllPermissions
        previewView.isVisible = hasAllPermissions

        val template = tinyDB?.getString("template", "default")
        layoutMapData.isVisible = hasAllPermissions && template == "default"
        layoutMapDataClassic.isVisible = hasAllPermissions && template == "classic"
        layoutMapDataSquarise.isVisible = hasAllPermissions && template == "squarise"

        if (!hasAllPermissions) {
            permissionTitle.setText(R.string.title_camera_location)
            permissionBody.setText(R.string.body_camera_location)
            cameraProvider?.unbindAll()
            camera = null
            isCameraBound = false
            stopLocationUpdates()
            return
        }

        checkAndPromptLocationSettings()
        startCamera()
        displayCurrentLocation()
    }

    private fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        intent.setData("package:$packageName".toUri())
        startActivity(intent)
    }

    private fun ActivityHomeBinding.startCamera() {
        // Already bound — CameraX resumes the preview on its own, so a rebind
        // here would only tear the surface down and re-show the loader.
        if (isCameraBound) return

        // Covered until the first frame streams; the previewStreamState
        // observer in initializeComponents() takes it away again.
        previewLoadingOverlay.visibility = View.VISIBLE

        val cameraProviderFuture = ProcessCameraProvider.getInstance(this@HomeActivity)

        cameraProviderFuture.addListener({
            val provider: ProcessCameraProvider = cameraProviderFuture.get()

            if (bindCameraUseCases(provider)) {
                setupCameraControls(provider)
            } else {
                previewLoadingOverlay.visibility = View.GONE
                Toast.makeText(
                    this@HomeActivity,
                    getString(R.string.toast_failed_start_camera),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }, ContextCompat.getMainExecutor(this@HomeActivity))
    }

    /**
     * Single place where photo quality is configured. MAXIMIZE_QUALITY runs the
     * full 3A convergence before the shot; the resolution is deliberately
     * capped — see [captureResolutionBound].
     */
    private fun buildImageCapture(targetAspectRatio: Int, targetRotation: Int): ImageCapture {
        val resolutionSelector = ResolutionSelector.Builder()
            .setAspectRatioStrategy(aspectRatioStrategy(targetAspectRatio))
            .setResolutionStrategy(
                ResolutionStrategy(
                    captureResolutionBound(targetAspectRatio),
                    ResolutionStrategy.FALLBACK_RULE_CLOSEST_LOWER_THEN_HIGHER
                )
            )
            .build()

        return ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
            .setJpegQuality(JPEG_QUALITY)
            .setResolutionSelector(resolutionSelector)
            .setTargetRotation(targetRotation)
            .build()
    }

    /**
     * Ceiling on the captured frame.
     *
     * The save path decodes the JPEG to an ARGB_8888 bitmap and then makes
     * several more full-size copies of it (EXIF rotate, front-camera flip,
     * portrait rotate, the composite), so peak memory is roughly three times
     * `pixels * 4` bytes. On a 50MP sensor that is over half a gigabyte and
     * the capture dies outright; asking for the sensor maximum here was what
     * broke it. ~12MP keeps far more detail than the stamped output needs
     * while staying inside a normal heap.
     */
    private fun captureResolutionBound(targetAspectRatio: Int): Size {
        return if (targetAspectRatio == AspectRatio.RATIO_16_9) {
            Size(3840, 2160)
        } else {
            Size(4000, 3000)
        }
    }

    private fun ActivityHomeBinding.setupCameraControls(cameraProvider: ProcessCameraProvider) {
        actionFlashMode.setOnClickListener {
            if (isBackCameraSelected) {
                handleFlashToggle()
            }
        }

        actionChangeCamera.setOnClickListener {
            toggleCamera(cameraProvider)
        }
    }

    private fun ActivityHomeBinding.toggleCamera(cameraProvider: ProcessCameraProvider) {
        isBackCameraSelected = !isBackCameraSelected

        if (bindCameraUseCases(cameraProvider)) {
            updateFlashIcon()
        } else {
            isBackCameraSelected = !isBackCameraSelected
            Toast.makeText(
                this@HomeActivity,
                getString(R.string.toast_failed_switch_camera),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun handleFlashToggle() {
        try {
            // Use the already-bound camera. Re-binding just to reach the
            // control tore the preview down for a frame on every tap.
            val camera = camera ?: return

            if (camera.cameraInfo.hasFlashUnit()) {
                val cameraControl = camera.cameraControl
                val torchState = camera.cameraInfo.torchState.value
                val isTorchOn = torchState == TorchState.ON

                updateFlashIcon(isTorchOn)
                cameraControl.enableTorch(!isTorchOn)
            } else {
                Toast.makeText(
                    this@HomeActivity,
                    getString(R.string.toast_flash_not_available),
                    Toast.LENGTH_SHORT
                ).show()
            }
        } catch (e: Exception) {
        }
    }

    /**
     * Tap to focus, then slide up or down without lifting to trim the
     * brightness — the gesture stock camera apps use. The old handler only
     * looked at ACTION_DOWN and hid the ring again on ACTION_UP, so neither
     * half of this worked.
     */
    private fun ActivityHomeBinding.handleTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                touchDownX = event.x
                touchDownY = event.y
                isExposureDragging = false
                exposureDragStartIndex =
                    camera?.cameraInfo?.exposureState?.exposureCompensationIndex ?: 0

                moveFocusRingTo(event.x, event.y)
                showFocusRing()
                focusAt(event.x, event.y)
            }

            MotionEvent.ACTION_MOVE -> {
                val dy = event.y - touchDownY
                if (!isExposureDragging &&
                    abs(dy) > touchSlop &&
                    abs(dy) > abs(event.x - touchDownX)
                ) {
                    isExposureDragging = true
                    showExposureSlider()
                }
                if (isExposureDragging) {
                    // Hold the overlays open for as long as the finger is down.
                    focusHideJob?.cancel()
                    applyExposureDrag(dy)
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (isExposureDragging) {
                    isExposureDragging = false
                    scheduleFocusOverlayHide()
                }
            }
        }
        return true
    }

    /** Centres the focus ring on the tapped point, clamped to the preview. */
    private fun ActivityHomeBinding.moveFocusRingTo(x: Float, y: Float) {
        val maxX = (previewView.width - focusView.width).toFloat().coerceAtLeast(0f)
        val maxY = (previewView.height - focusView.height).toFloat().coerceAtLeast(0f)
        // Translation, not x/y: the ring is laid out at the preview's top-left
        // corner, so a translation is already in preview coordinates and stays
        // correct wherever the preview sits inside its container.
        focusView.translationX = (x - focusView.width / 2f).coerceIn(0f, maxX)
        focusView.translationY = (y - focusView.height / 2f).coerceIn(0f, maxY)
    }

    /**
     * Maps the vertical drag onto the device's exposure compensation range —
     * up is brighter. A drag across [EXPOSURE_DRAG_TRAVEL] of the preview
     * height covers the whole range, so short sensors still get fine control.
     */
    private fun ActivityHomeBinding.applyExposureDrag(dy: Float) {
        val cameraInfo = camera?.cameraInfo ?: return
        val exposureState = cameraInfo.exposureState
        if (!exposureState.isExposureCompensationSupported) return

        val range = exposureState.exposureCompensationRange
        val span = range.upper - range.lower
        if (span <= 0) return

        val travel = (previewView.height * EXPOSURE_DRAG_TRAVEL).coerceAtLeast(1f)
        val steps = (-dy / travel) * span
        val index = (exposureDragStartIndex + steps).roundToInt()
            .coerceIn(range.lower, range.upper)

        if (index != exposureState.exposureCompensationIndex) {
            camera?.cameraControl?.setExposureCompensationIndex(index)
        }
        exposureSlider.setProgress((index - range.lower).toFloat() / span)
    }

    private fun ActivityHomeBinding.showExposureSlider() {
        val exposureState = camera?.cameraInfo?.exposureState ?: return
        if (!exposureState.isExposureCompensationSupported) return

        val range = exposureState.exposureCompensationRange
        val span = range.upper - range.lower
        if (span > 0) {
            exposureSlider.setProgress(
                (exposureState.exposureCompensationIndex - range.lower).toFloat() / span
            )
        }

        positionExposureSlider()
        exposureSlider.animate().cancel()
        exposureSlider.alpha = 1f
        exposureSlider.beVisible()
    }

    /** Parks the slider beside the ring, flipping sides near the right edge. */
    private fun ActivityHomeBinding.positionExposureSlider() {
        val gap = EXPOSURE_SLIDER_GAP_DP * resources.displayMetrics.density
        val rightOfRing = focusView.translationX + focusView.width + gap
        val translationX = if (rightOfRing + exposureSlider.width <= previewView.width) {
            rightOfRing
        } else {
            (focusView.translationX - gap - exposureSlider.width).coerceAtLeast(0f)
        }
        val maxY = (previewView.height - exposureSlider.height).toFloat().coerceAtLeast(0f)

        exposureSlider.translationX = translationX
        exposureSlider.translationY =
            (focusView.translationY + focusView.height / 2f - exposureSlider.height / 2f)
                .coerceIn(0f, maxY)
    }

    /**
     * Rebuilds the zoom chips for whatever lens is now bound.
     *
     * The candidate ratios are filtered against the camera's real
     * min/max — an ultra-wide 0.5x only appears on hardware that has one, and
     * a 5x only where the range reaches it — so the row never offers a ratio
     * the device would silently clamp.
     */
    private fun ActivityHomeBinding.setupZoomControls() {
        val cameraInfo = camera?.cameraInfo
        val zoomState = cameraInfo?.zoomState?.value

        if (cameraInfo == null || zoomState == null || zoomState.maxZoomRatio <= 1f) {
            layoutZoomLevels.beGone()
            return
        }

        val ratios = zoomRatiosFor(zoomState.minZoomRatio, zoomState.maxZoomRatio)
        layoutZoomLevels.removeAllViews()
        zoomChips.clear()

        val chipWidth = resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._40sdp)
        val chipHeight = resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._20sdp)
        val chipGap = resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._2sdp)

        ratios.forEach { ratio ->
            val chip = TextView(this@HomeActivity).apply {
                text = formatZoomLabel(ratio)
                tag = ratio
                gravity = Gravity.CENTER
                includeFontPadding = false
                setTextSize(TypedValue.COMPLEX_UNIT_SP, ZOOM_CHIP_TEXT_SP)
                setBackgroundResource(R.drawable.bg_zoom_chip)
                layoutParams = LinearLayout.LayoutParams(chipWidth, chipHeight).apply {
                    marginStart = chipGap
                    marginEnd = chipGap
                }
                setOnClickListener { applyZoomRatio(ratio) }
            }
            zoomChips.add(chip)
            layoutZoomLevels.addView(chip)
        }
        layoutZoomLevels.beVisible()

        // The camera object is replaced on every rebind, so drop the previous
        // observer or each flip would stack another one on this activity.
        cameraInfo.zoomState.removeObservers(this@HomeActivity)
        cameraInfo.zoomState.observe(this@HomeActivity) { state ->
            updateZoomChips(state.zoomRatio)
        }
        updateZoomChips(zoomState.zoomRatio)
    }

    private fun zoomRatiosFor(minRatio: Float, maxRatio: Float): List<Float> {
        val ratios = mutableListOf<Float>()
        // Lead with the lens's true wide end rather than a hardcoded 0.5x —
        // ultra-wides report 0.5, 0.6 or 0.7 depending on the device, and a
        // fixed chip would either miss it or ask for a ratio it can't reach.
        if (minRatio < WIDE_ZOOM_THRESHOLD) ratios.add(minRatio)
        ratios.addAll(ZOOM_CANDIDATES.filter { it in minRatio..maxRatio })

        return ratios.distinct().ifEmpty { listOf(1f.coerceIn(minRatio, maxRatio)) }
    }

    /** "0.5x", "1x", "2x" — trailing zeroes dropped, like the stock camera. */
    private fun formatZoomLabel(ratio: Float): String {
        return if (ratio % 1f == 0f) {
            "${ratio.toInt()}x"
        } else {
            "${(ratio * 10).roundToInt() / 10f}x"
        }
    }

    private fun ActivityHomeBinding.applyZoomRatio(ratio: Float) {
        val cameraControl = camera?.cameraControl ?: return
        val zoomState = camera?.cameraInfo?.zoomState?.value ?: return
        val target = ratio.coerceIn(zoomState.minZoomRatio, zoomState.maxZoomRatio)

        try {
            cameraControl.setZoomRatio(target)
        } catch (exc: Exception) {
            Log.e(HomeActivity::class.java.simpleName, "Zoom request failed", exc)
            return
        }
        updateZoomChips(target)
    }

    /**
     * Marks the chip the current ratio belongs to. A pinch lands between the
     * presets, so the nearest chip at or below the ratio stays lit rather than
     * the row going blank mid-gesture.
     */
    private fun updateZoomChips(currentRatio: Float) {
        if (zoomChips.isEmpty()) return

        val activeIndex = zoomChips.indices.lastOrNull { index ->
            (zoomChips[index].tag as? Float ?: 1f) <= currentRatio + ZOOM_MATCH_TOLERANCE
        } ?: 0

        zoomChips.forEachIndexed { index, chip ->
            val isActive = index == activeIndex
            chip.isSelected = isActive
            // White on the accent fill; plain white when unselected.
            chip.setTextColor(color(R.color.colorWhite))
            chip.text = if (isActive) {
                formatZoomLabel(currentRatio)
            } else {
                formatZoomLabel(chip.tag as? Float ?: 1f)
            }
        }
    }

    /** Back to the metered exposure — called after every rebind. */
    private fun resetExposure() {
        val exposureState = camera?.cameraInfo?.exposureState ?: return
        if (!exposureState.isExposureCompensationSupported) return

        if (exposureState.exposureCompensationIndex != 0) {
            camera?.cameraControl?.setExposureCompensationIndex(0)
        }
        val range = exposureState.exposureCompensationRange
        val span = range.upper - range.lower
        if (span > 0) {
            binding?.exposureSlider?.setProgress((-range.lower).toFloat() / span)
        }
    }

    /**
     * Drives the real CameraX 3A engine. Until now the tap only moved a
     * decorative ring, so the sensor never refocused and photos came out soft.
     * Manual mode holds the lock until the next tap; auto mode releases it
     * after [FOCUS_LOCK_SECONDS] and hands control back to continuous AF.
     */
    private fun ActivityHomeBinding.focusAt(x: Float, y: Float) {
        val cameraControl = camera?.cameraControl ?: return
        val point = previewView.meteringPointFactory.createPoint(x, y)
        val action = FocusMeteringAction.Builder(
            point, FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE
        ).apply {
            if (isFocusManual) {
                disableAutoCancel()
            } else {
                setAutoCancelDuration(FOCUS_LOCK_SECONDS, TimeUnit.SECONDS)
            }
        }.build()

        try {
            cameraControl.startFocusAndMetering(action)
        } catch (exc: Exception) {
            Log.e(HomeActivity::class.java.simpleName, "Focus request failed", exc)
        }
    }

    private fun ActivityHomeBinding.toggleFocusMode() {
        isFocusManual = !isFocusManual
        if (isFocusManual) {
            isFirstClick = true
            actionFocusMode.iconTint = ColorStateList.valueOf(color(R.color.colorWhite))
            focusView.setFocusShape(FocusView.FOCUS_SHAPE_SQUARE)
            focusView.setManualFocus(true)
            // Seed the manual lock at the centre so the mode takes effect
            // immediately instead of waiting for the first tap.
            centreFocusRing()
            showFocusRing()
            focusAt(previewView.width / 2f, previewView.height / 2f)
        } else {
            isFirstClick = true
            actionFocusMode.iconTint = ColorStateList.valueOf(color(R.color.colorAccent))
            focusView.setFocusShape(FocusView.FOCUS_SHAPE_CIRCLE)
            focusView.setManualFocus(false)
            // Release the lock — the sensor goes back to tracking the scene.
            camera?.cameraControl?.cancelFocusAndMetering()
            centreFocusRing()
            showFocusRing()
        }
    }

    private fun ActivityHomeBinding.centreFocusRing() {
        moveFocusRingTo(previewView.width / 2f, previewView.height / 2f)
    }

    private fun ActivityHomeBinding.showFocusRing() {
        // Cancel whatever the previous tap left running, otherwise rapid taps
        // fight over alpha/visibility and the ring flickers or sticks.
        focusHideJob?.cancel()
        focusView.animate().cancel()
        focusView.alpha = 1f
        focusView.scaleX = 1f
        focusView.scaleY = 1f
        focusView.beVisible()

        focusView.animate().scaleX(1.15f).scaleY(1.15f).setDuration(120).withEndAction {
            focusView.animate().scaleX(1f).scaleY(1f).setDuration(120).start()
        }.start()

        scheduleFocusOverlayHide()
    }

    /**
     * Fades out the ring and, if it is up, the brightness slider. The slider
     * lingers a little longer so the value the user just dialled in stays
     * readable after they lift their finger.
     */
    private fun ActivityHomeBinding.scheduleFocusOverlayHide() {
        focusHideJob?.cancel()
        focusHideJob = lifecycleScope.launch(Dispatchers.Main) {
            val sliderShowing = exposureSlider.isVisible
            delay(if (sliderShowing) EXPOSURE_VISIBLE_MS else FOCUS_RING_VISIBLE_MS)
            fadeOutOverlay(focusView)
            if (sliderShowing) fadeOutOverlay(exposureSlider)
        }
    }

    /** Shutter recoil — the tactile beat a camera app is expected to have. */
    private fun ActivityHomeBinding.animateShutterPress() {
        actionCapture.animate().cancel()
        actionCapture.scaleX = 1f
        actionCapture.scaleY = 1f
        actionCapture.animate()
            .scaleX(SHUTTER_PRESS_SCALE).scaleY(SHUTTER_PRESS_SCALE)
            .setDuration(80)
            .withEndAction {
                actionCapture.animate().scaleX(1f).scaleY(1f).setDuration(120).start()
            }
            .start()
    }

    private fun fadeOutOverlay(view: View) {
        view.animate().alpha(0f).setDuration(250).withEndAction {
            view.beInvisible()
            view.alpha = 1f
        }.start()
    }

    /**
     * The selected self-timer, read from the same preference
     * [restoreTimerState] restores from.
     *
     * This used to be inferred from the timer button's own label, which meant
     * the capture path broke the moment the button stopped carrying text.
     */
    private fun currentTimerStringRes(): Int {
        return getSharedPreferences("timer_state", MODE_PRIVATE)
            .getInt("timer_state_key", R.string.action_timer_off)
    }

    private fun hasActiveTimer(): Boolean {
        return currentTimerStringRes() != R.string.action_timer_off
    }

    private fun ActivityHomeBinding.handleCaptureClick() {
        when (currentTimerStringRes()) {
            R.string.action_timer_3sec -> handleTimer3SecondCapture()
            R.string.action_timer_5sec -> handleTimer5SecondCapture()
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
            Toast.makeText(
                this@HomeActivity,
                getString(R.string.toast_camera_not_ready),
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val photoFile = createPhotoFile()
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        if (!hasActiveTimer()) {
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
                            getString(
                                R.string.message_capture_failed,
                                exception.message ?: ""
                            ),
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

    /**
     * Refreshes the gallery thumbnail from the most recent capture.
     *
     * The previous version looked only at the top level of the app directory,
     * while captures actually land in DCIM/Camera by default and in Site
     * subfolders otherwise — so it found nothing in every configuration and
     * the thumbnail sat empty until you took a photo in that session. The scan
     * walks several roots, so it runs off the main thread.
     */
    private fun ActivityHomeBinding.refreshLastCapturedThumbnail() {
        lifecycleScope.launch {
            val latest = withContext(Dispatchers.IO) {
                CapturedPhotos.latest(this@HomeActivity)
            }
            latest?.let { file ->
                lastCapturedFile = file
                Glide.with(this@HomeActivity).load(file).into(imageCaptured)
            }
        }
    }

    /**
     * Renders the stamp overlay and hands the heavy pixel work to a background
     * thread.
     *
     * Only the parts that touch a View can stay here: the overlay is drawn
     * from the live stamp views and the map snapshot is delivered on the main
     * looper. Everything after that — decode, rotate, flip, composite, encode
     * — used to run on the main thread too, which is why the system logged
     * pause/stop timeouts for this activity on every shot.
     */
    @SuppressLint("MissingPermission")
    private fun ActivityHomeBinding.processAndSaveImageWithOverlays(photoFile: File) {
        val mapFragment = getActiveMapFragment()
        if (mapFragment == null) {
            composeAndSaveInBackground(photoFile, renderStampOverlay(null))
            return
        }
        mapFragment.getMapAsync { googleMap ->
            googleMap.snapshot { mapBitmap ->
                composeAndSaveInBackground(photoFile, renderStampOverlay(mapBitmap))
            }
        }
    }

    /** Draws the stamp panel, with the map tile composited in. Main thread only. */
    private fun ActivityHomeBinding.renderStampOverlay(mapBitmap: Bitmap?): Bitmap {
        val overlay = createParentOverlayBitmap()
        if (mapBitmap == null) return overlay

        try {
            val cardWidth = mapCardView.width
            val cardHeight = mapCardView.height
            val scaledMap = if (cardWidth > 0 && cardHeight > 0) {
                mapBitmap.scale(cardWidth, cardHeight)
            } else {
                mapBitmap
            }

            val radiusPx = resources.getDimension(com.intuit.sdp.R.dimen._8sdp)
            val roundedMap = createRoundedMapBitmap(scaledMap, radiusPx)
            if (scaledMap != mapBitmap) scaledMap.recycle()

            Canvas(overlay).drawBitmap(
                roundedMap,
                mapCardView.left.toFloat(),
                mapCardView.top.toFloat(),
                Paint(Paint.ANTI_ALIAS_FLAG)
            )
            roundedMap.recycle()
        } catch (exc: Exception) {
            Log.e(HomeActivity::class.java.simpleName, "Map tile render failed", exc)
        } finally {
            mapBitmap.recycle()
        }
        return overlay
    }

    private fun ActivityHomeBinding.composeAndSaveInBackground(photoFile: File, overlay: Bitmap) {
        // Snapshot the view-dependent inputs now; the worker must not touch
        // the activity state while the user keeps using the camera.
        val flipHorizontally = !isBackCameraSelected
        val forcePortrait = isPortraitMode
        val marginPx = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, OVERLAY_MARGIN_DP, resources.displayMetrics
        )

        lifecycleScope.launch {
            val result = withContext(Dispatchers.Default) {
                composePhoto(photoFile, overlay, flipHorizontally, forcePortrait, marginPx)
            }

            hideProgressAnimations()
            Toast.makeText(this@HomeActivity, getString(result.messageRes), Toast.LENGTH_SHORT)
                .show()
            if (result.saved) {
                broadcastMediaScan(photoFile)
                Glide.with(this@HomeActivity).load(photoFile).into(imageCaptured)
                lastCapturedFile = photoFile
            }
        }
    }

    private data class CaptureResult(val saved: Boolean, @StringRes val messageRes: Int)

    /** Pure bitmap work — no views, no activity state. Safe off the main thread. */
    private fun composePhoto(
        photoFile: File,
        overlay: Bitmap,
        flipHorizontally: Boolean,
        forcePortrait: Boolean,
        marginPx: Float
    ): CaptureResult {
        val options = BitmapFactory.Options().apply {
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val captured = BitmapFactory.decodeFile(photoFile.absolutePath, options)
        if (captured == null) {
            overlay.recycle()
            return CaptureResult(false, R.string.toast_failed_capture_image)
        }

        var working = captured
        working = replaceBitmap(working, applyExifOrientation(photoFile, working))
        if (flipHorizontally) {
            working = replaceBitmap(working, flipBitmapHorizontal(working))
        }
        if (forcePortrait && working.width > working.height) {
            working = replaceBitmap(working, rotateBitmap(working))
        }

        return try {
            val targetWidth = (working.width - marginPx * 2).toInt().coerceAtLeast(1)
            val targetHeight = (working.height * OVERLAY_HEIGHT_RATIO).toInt().coerceAtLeast(120)

            val scaledOverlay =
                if (overlay.width == targetWidth && overlay.height == targetHeight) {
                    overlay
                } else {
                    overlay.scale(targetWidth, targetHeight)
                }
            if (scaledOverlay != overlay) overlay.recycle()

            val combined = createBitmap(working.width, working.height)
            val canvas = Canvas(combined)
            canvas.drawBitmap(working, 0f, 0f, null)
            canvas.drawBitmap(
                scaledOverlay, marginPx, working.height - targetHeight - marginPx, null
            )
            scaledOverlay.recycle()

            val saved = saveCombinedBitmap(combined, photoFile)
            combined.recycle()
            working.recycle()

            CaptureResult(
                saved,
                if (saved) {
                    R.string.message_portrait_saved_with_gps
                } else {
                    R.string.message_image_saved_without_gps
                }
            )
        } catch (exc: Exception) {
            Log.e(HomeActivity::class.java.simpleName, "Overlay composite failed", exc)
            // Still give the user their photo, just without the stamp.
            val fallbackSaved = saveCombinedBitmap(working, photoFile)
            working.recycle()
            CaptureResult(
                fallbackSaved,
                if (fallbackSaved) {
                    R.string.message_image_saved_map_failed
                } else {
                    R.string.message_save_failed
                }
            )
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

    private fun applyExifOrientation(photoFile: File, source: Bitmap): Bitmap {
        val exif = ExifInterface(photoFile.absolutePath)
        val orientation = exif.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL
        )
        val matrix = Matrix()
        var hasTransform = false
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> {
                matrix.postRotate(90F)
                hasTransform = true
            }

            ExifInterface.ORIENTATION_ROTATE_180 -> {
                matrix.postRotate(180F)
                hasTransform = true
            }

            ExifInterface.ORIENTATION_ROTATE_270 -> {
                matrix.postRotate(270F)
                hasTransform = true
            }

            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> {
                matrix.postScale(-1F, 1F)
                hasTransform = true
            }

            ExifInterface.ORIENTATION_FLIP_VERTICAL -> {
                matrix.postScale(1F, -1F)
                hasTransform = true
            }

            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.postRotate(90F)
                matrix.postScale(-1F, 1F)
                hasTransform = true
            }

            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.postRotate(270F)
                matrix.postScale(-1F, 1F)
                hasTransform = true
            }

            else -> Unit
        }

        return if (!hasTransform) {
            source
        } else {
            Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
        }
    }

    private fun flipBitmapHorizontal(source: Bitmap): Bitmap {
        val matrix = Matrix().apply { postScale(-1F, 1F) }
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }

    private fun replaceBitmap(current: Bitmap, next: Bitmap): Bitmap {
        if (next !== current) {
            current.recycle()
        }
        return next
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
            Toast.makeText(this, getString(R.string.toast_video_not_ready), Toast.LENGTH_SHORT)
                .show()
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
                                    // The Transformer export outlives the activity: if the user
                                    // left before it finished, the file is saved but no UI work
                                    // is possible (Glide throws on a destroyed activity).
                                    runOnUiThread {
                                        if (isFinishing || isDestroyed) return@runOnUiThread
                                        Toast.makeText(
                                            this,
                                            getString(R.string.message_overlay_video_saved),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        lastCapturedFile = outputFile
                                        binding?.imageCaptured?.let { preview ->
                                            Glide.with(this@HomeActivity).load(outputFile).into(preview)
                                        }
                                    }
                                },
                                onError = { e ->
                                    Log.e(
                                        "startVideoRecording:", "Overlay failed: ${e.message}"
                                    )
                                    runOnUiThread {
                                        if (isFinishing || isDestroyed) return@runOnUiThread
                                        Toast.makeText(
                                            this,
                                            getString(
                                                R.string.message_overlay_failed,
                                                e.message ?: ""
                                            ),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        lastCapturedFile = inputFile
                                        binding?.imageCaptured?.let { preview ->
                                            Glide.with(this@HomeActivity).load(inputFile).into(preview)
                                        }
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

            // The frame has already been through one JPEG round-trip by the
            // time it gets here, so the final encode is kept high to avoid
            // stacking artefacts around the stamp text and map tile.
            FileOutputStream(file).use { fos ->
                val success = bitmap.compress(
                    Bitmap.CompressFormat.JPEG, SAVE_JPEG_QUALITY, fos
                )
                fos.flush()
                success
            }
        } catch (e: Exception) {
            false
        }
    }

    private fun handleCaptureError() {
        runOnUiThread {
            hideProgressAnimations()
            Toast.makeText(
                this@HomeActivity,
                getString(R.string.toast_failed_capture_image),
                Toast.LENGTH_SHORT
            ).show()
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
                    MarkerOptions().position(latLng)
                        .title(getString(R.string.label_current_location))
                        .snippet(
                            getString(
                                R.string.format_lat_lng,
                                loc.latitude,
                                loc.longitude
                            )
                        )
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))

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
                    if (locationMode == LocationMode.CUSTOM) getString(R.string.label_custom_location)
                    else getString(R.string.label_current_location)
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
                    val unknownLocation = getString(R.string.label_unknown_location)
                    val parts = mutableListOf<String>()
                    address.featureName?.takeIf { it.isNotBlank() }?.let { parts.add(it) }
                    address.premises?.takeIf { it.isNotBlank() }?.let { parts.add(it) }
                    address.subThoroughfare?.takeIf { it.isNotBlank() }?.let { parts.add(it) }
                    address.thoroughfare?.takeIf { it.isNotBlank() }?.let { parts.add(it) }
                    address.subLocality?.takeIf { it.isNotBlank() }?.let { parts.add(it) }
                    address.locality?.takeIf { it.isNotBlank() }?.let { parts.add(it) }
                    address.adminArea?.takeIf { it.isNotBlank() }?.let { parts.add(it) }
                    address.postalCode?.takeIf { it.isNotBlank() }?.let { parts.add(it) }
                    address.countryName?.takeIf { it.isNotBlank() }?.let { parts.add(it) }

                    val fullAddress = parts.joinToString(", ")

                    val isShort =
                        address.getAddressLine(0).length < 12

                    if (isShort) {
                        Log.e("isShort", "updateLocationUI: $fullAddress" )
                        displayCurrentLocation()
                        return@getAddress
                    }
                    textAddress.text = if (address.getAddressLine(0).isEmpty()) {
                        fullAddress
                    } else {
                        address.getAddressLine(0)?.takeIf { it.isNotBlank() } ?: unknownLocation
                    }
                    updateOverlayState(location, fullAddress)
                    textAddressClassic.text = if (address.getAddressLine(0).isEmpty()) {
                        fullAddress
                    } else {
                        address.getAddressLine(0)?.takeIf { it.isNotBlank() } ?: unknownLocation
                    }
                    textAddressSquarise.text = if (address.getAddressLine(0).isEmpty()) {
                        fullAddress
                    } else {
                        address.getAddressLine(0)?.takeIf { it.isNotBlank() } ?: unknownLocation
                    }
                }

                textLatitudeValue.text = String.format("%.6f", location.latitude)
                textLatitudeValueSquarise.text = String.format("%.6f", location.latitude)
                textLatitudeValueClassic.text = String.format("%.6f", location.latitude)
                textLongitudeValue.text = String.format("%.6f", location.longitude)
                textLongitudeValueSquarise.text = String.format("%.6f", location.longitude)
                textLongitudeValueClassic.text = String.format("%.6f", location.longitude)

                val altitudeText = if (location.hasAltitude()) {
                    String.format("%.1f m", location.altitude)
                } else {
                    getString(R.string.label_not_available)
                }
                textAltitude.text = getString(R.string.format_altitude, altitudeText)
                textAltitudeSquarise.text = getString(R.string.format_altitude, altitudeText)
                textAltitudeClassic.text = getString(R.string.format_altitude, altitudeText)

                val currentDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
                val currentDay = SimpleDateFormat("EEEE", Locale.getDefault()).format(Date())
                textDate.text = getString(R.string.format_day_date, currentDay, currentDate)
                textDateSquarise.text = getString(R.string.format_day_date, currentDay, currentDate)
                textDateClassic.text = getString(R.string.format_day_date, currentDay, currentDate)
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
            saveTimerState(R.string.action_timer_3sec, R.color.colorAccent)
        }

        2 -> {
            actionTimer.iconTint = ColorStateList.valueOf(color(R.color.colorAccent))
            saveTimerState(R.string.action_timer_5sec, R.color.colorAccent)
        }

        else -> {
            actionTimer.iconTint = ColorStateList.valueOf(color(R.color.colorWhite))
            saveTimerState(R.string.action_timer_off, R.color.colorWhite)
        }
    }

    private fun ActivityHomeBinding.toggleSound() {
        isSoundOn = !isSoundOn
        if (isSoundOn) {
            actionSound.iconTint = ColorStateList.valueOf(color(R.color.colorAccent))
            actionSound.icon = drawable(R.drawable.ic_action_sound_on)
        } else {
            actionSound.iconTint = ColorStateList.valueOf(color(R.color.colorWhite))
            actionSound.icon = drawable(R.drawable.ic_action_sound_off)
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
    }

    private fun ActivityHomeBinding.restoreSoundState() {
        val sharedPref = getSharedPreferences("sound_state", MODE_PRIVATE)
        isSoundOn = sharedPref.getBoolean("is_sound_on", true)
        if (isSoundOn) {
            actionSound.iconTint = ColorStateList.valueOf(color(R.color.colorAccent))
            actionSound.icon = drawable(R.drawable.ic_action_sound_on)
        } else {
            actionSound.iconTint = ColorStateList.valueOf(color(R.color.colorWhite))
            actionSound.icon = drawable(R.drawable.ic_action_sound_off)
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
        // The user may have picked a different template while away, which moves
        // the map to a different container — re-attach before touching it.
        setupMapSnapshot()
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

        var cameraGranted = hasPermission(Manifest.permission.CAMERA)
        var fineGranted = hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)
        var coarseGranted = hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
        permissionList.forEachIndexed { index, permission ->
            if (grantResults.getOrNull(index) != PackageManager.PERMISSION_GRANTED) return@forEachIndexed

            when (permission) {
                Manifest.permission.CAMERA -> cameraGranted = true
                Manifest.permission.ACCESS_FINE_LOCATION -> fineGranted = true
                Manifest.permission.ACCESS_COARSE_LOCATION -> coarseGranted = true
            }
        }

        val locationGranted = fineGranted && coarseGranted

        if (cameraGranted && locationGranted) {
            binding?.updateCameraPermissionUi()
            Toast.makeText(this, getString(R.string.toast_camera_gps_ready), Toast.LENGTH_SHORT)
                .show()
        } else {
            incrementPermissionsDeniedCount("PERMISSION_CAMERA_LOCATION")
            binding?.updateCameraPermissionUi()
            Toast.makeText(
                this,
                getString(R.string.message_camera_gps_permissions_required),
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun ActivityHomeBinding.updateTimeDisplay() {
        val localTime = SimpleDateFormat("HH:mm:ss a", Locale.getDefault()).format(Date())
        textLocalTime.text = getString(R.string.format_local_time, localTime)
        textLocalTimeSquarise.text = getString(R.string.format_local_time, localTime)
        textLocalTimeClassic.text = getString(R.string.format_local_time, localTime)

        val gmtFormat = SimpleDateFormat("HH:mm:ss a", Locale.getDefault())
        gmtFormat.timeZone = TimeZone.getTimeZone("GMT")
        val gmtTime = gmtFormat.format(Date())
        textGmtTime.text = getString(R.string.format_gmt_time, gmtTime)
        textGmtTimeSquarise.text = getString(R.string.format_gmt_time, gmtTime)
        textGmtTimeClassic.text = getString(R.string.format_gmt_time, gmtTime)
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
        val unknownValue = getString(R.string.label_unknown)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            geocoder.getFromLocation(latitude, longitude, 1, object : Geocoder.GeocodeListener {
                override fun onGeocode(addresses: MutableList<Address>) {
                    val address = addresses.firstOrNull() ?: Address(Locale.getDefault()).apply {
                        locality = unknownValue
                        adminArea = unknownValue
                        countryName = unknownValue
                    }
                    callback(address)
                }

                override fun onError(errorMessage: String?) {
                    callback(Address(Locale.getDefault()).apply {
                        locality = unknownValue
                        adminArea = unknownValue
                        countryName = unknownValue
                    })
                }
            })
        } else {
            try {
                val addresses = geocoder.getFromLocation(latitude, longitude, 1)
                val address = addresses?.firstOrNull() ?: Address(Locale.getDefault()).apply {
                    locality = unknownValue
                    adminArea = unknownValue
                    countryName = unknownValue
                }
                callback(address)
            } catch (e: Exception) {
                Log.e("Geocoder", "Geocoding failed: ${e.message}")
                callback(Address(Locale.getDefault()).apply {
                    locality = unknownValue
                    adminArea = unknownValue
                    countryName = unknownValue
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
            animateShutterPress()
            when (captureMode) {
                CaptureMode.PHOTO -> handleCaptureClick()
                CaptureMode.VIDEO -> toggleVideoRecording()
            }
        }
        imageCaptured.setOnClickListener {
            lastCapturedFile?.let { file ->
                if (file.exists()) {
                    val extension = file.extension.lowercase(Locale.getDefault())
                    val isVideo = extension in setOf("mp4", "mkv", "3gp", "webm")
                    if (isVideo) {
                        val uri = FileProvider.getUriForFile(
                            this@HomeActivity, "${applicationContext.packageName}.provider", file
                        )
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            setDataAndType(uri, "video/*")
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        startActivity(
                            Intent.createChooser(
                                intent,
                                getString(R.string.chooser_open_video)
                            )
                        )
                    } else {
                        val intent = Intent(this@HomeActivity, ViewCollectionActivity::class.java).apply {
                            putExtra(ViewCollectionActivity.EXTRA_FILE_PATH, file.absolutePath)
                        }
                        startActivity(intent)
                    }
                } else {
                    Toast.makeText(
                        this@HomeActivity,
                        getString(R.string.toast_no_image_available),
                        Toast.LENGTH_SHORT
                    )
                        .show()
                }
            }
        }
        actionTemplates.setOnClickListener {
            stopLocationUpdates()
            timeHandler.removeCallbacks(timeRunnable)
            allowMapSnapshot = false
            go(TemplatesActivity::class.java)
        }
        actionTools.setOnClickListener {
            stopLocationUpdates()
            timeHandler.removeCallbacks(timeRunnable)
            allowMapSnapshot = false
            go(ToolsActivity::class.java)
        }
        actionMapData.setOnClickListener {
            stopLocationUpdates()
            timeHandler.removeCallbacks(timeRunnable)
            allowMapSnapshot = false
            locationUpdateResult.launch(goResult(LocationsActivity::class.java))
        }
        actionSettings.setOnClickListener {
            stopLocationUpdates()
            timeHandler.removeCallbacks(timeRunnable)
            allowMapSnapshot = false
            go(AppSettingsActivity::class.java)
        }
        buttonAllowAccess.setOnClickListener {
            requestCameraLocationPermissions()
        }

        when (intent?.getIntExtra("action", 0)) {
            1 -> {
                stopLocationUpdates()
                timeHandler.removeCallbacks(timeRunnable)
                allowMapSnapshot = false
                go(TemplatesActivity::class.java)
            }

            2 -> {
                stopLocationUpdates()
                timeHandler.removeCallbacks(timeRunnable)
                allowMapSnapshot = false
                locationUpdateResult.launch(goResult(LocationsActivity::class.java))
            }

            3 -> {
                stopLocationUpdates()
                timeHandler.removeCallbacks(timeRunnable)
                allowMapSnapshot = false
                go(ToolsActivity::class.java)
            }
        }
    }

    private fun handleRate() {
        val today = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
        if (lastRatePromptDay == today) {
            return
        }

        if (!TinyDB(this@HomeActivity).getBoolean("isRated", false)) {
            lastRatePromptDay = today
//            viewRateDialog {
//                if (it) {
//                    launchInAppReviewFlow(object : InAppReviewListener {
//                        override fun onComplete() {
//                            TinyDB(this@HomeActivity).putBoolean("isRated", true)
//                        }
//
//                        override fun onFailed() {
//                            Log.e("TAG", "reviewFailed: ")
//                        }
//                    })
//                } else {
//                    Intent(this@HomeActivity, FeedbackActivity::class.java).apply {
//                        startActivity(this)
//                    }
//                }
//            }
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

    private fun showExitSheet() {
        if (isFinishing || exitSheetDialog?.isShowing == true) return

        val dialog =
            BottomSheetDialog(
                this,
                coder.apps.space.library.R.style.Theme_Space_BottomSheetDialogTheme
            )
        val sheetBinding = LayoutSheetExitBinding.inflate(layoutInflater)
        dialog.setContentView(sheetBinding.root)
        dialog.window?.apply {
            applyDialogConfig()
        }

        sheetBinding.actionPositive.setOnClickListener {
            finishAffinity()
        }

        viewNativeSmall(sheetBinding.adNative)

        dialog.setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
                finishAffinity()
                true
            } else {
                false
            }
        }

        dialog.setOnDismissListener {
            if (exitSheetDialog === dialog) {
                exitSheetDialog = null
            }
        }

        exitSheetDialog = dialog
        if (!isFinishing) dialog.show()
    }

    override fun ActivityHomeBinding.initView() {
        updateStatusBarColor(R.color.colorTransparent)
        updateNavigationBarColor(R.color.colorBlack)
        // The preview stays full-bleed under the bars; only the chrome is
        // inset. The bottom row needs it too now that API 36 always draws
        // behind the navigation bar.
        layoutTopController.applySystemBarPadding(applyBottom = false)
        bottomControls.applySystemBarPadding(applyTop = false)
        onBackPressedDispatcher.addCallback {
            if (exitSheetDialog?.isShowing == true) {
                finishAffinity()
                return@addCallback
            }
            showExitSheet()
        }
    }

    companion object {
        /**
         * Quality of the JPEG CameraX hands back. It is decoded and re-encoded
         * with the overlay straight away, so pushing this to 100 only inflated
         * the file the save path has to read back.
         */
        private const val JPEG_QUALITY = 95

        /** Quality of the finished photo written to storage. */
        private const val SAVE_JPEG_QUALITY = 95

        /** How long a tap-to-focus lock is held before 3A returns to auto. */
        private const val FOCUS_LOCK_SECONDS = 4L

        /** How long the focus ring stays on screen after a tap. */
        private const val FOCUS_RING_VISIBLE_MS = 1200L

        /** How long the brightness slider lingers after the drag ends. */
        private const val EXPOSURE_VISIBLE_MS = 2000L

        /** Fraction of the preview height that spans the full exposure range. */
        private const val EXPOSURE_DRAG_TRAVEL = 0.55f

        /** Space between the focus ring and the brightness slider, in dp. */
        private const val EXPOSURE_SLIDER_GAP_DP = 6f

        /** How far the shutter button dips when pressed. */
        private const val SHUTTER_PRESS_SCALE = 0.88f

        /** Inset of the stamp overlay from the photo edges, in dp. */
        private const val OVERLAY_MARGIN_DP = 20f

        /** Stamp overlay height, as a fraction of the photo height. */
        private const val OVERLAY_HEIGHT_RATIO = 0.20f

        /** Ratios offered as chips, filtered against what the lens supports. */
        private val ZOOM_CANDIDATES = listOf(1f, 2f, 5f, 10f)

        /** Below this, the lens has a real ultra-wide worth its own chip. */
        private const val WIDE_ZOOM_THRESHOLD = 0.99f

        private const val ZOOM_CHIP_TEXT_SP = 12f

        /** Slack when deciding which chip a pinched ratio belongs to. */
        private const val ZOOM_MATCH_TOLERANCE = 0.05f
    }
}
