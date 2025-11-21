package aanibrothers.tracker.io.ui

import aanibrothers.tracker.io.R
import aanibrothers.tracker.io.databinding.*
import aanibrothers.tracker.io.extension.*
import aanibrothers.tracker.io.module.*
import android.Manifest
import android.annotation.*
import android.content.*
import android.graphics.*
import android.net.*
import android.os.*
import android.provider.*
import android.view.*
import android.widget.*
import androidx.activity.*
import androidx.activity.result.contract.*
import androidx.camera.core.*
import androidx.camera.lifecycle.*
import androidx.core.content.*
import androidx.exifinterface.media.*
import coder.apps.space.library.base.*
import coder.apps.space.library.extension.*
import com.bumptech.glide.*
import com.bumptech.glide.load.resource.drawable.*
import com.bumptech.glide.request.*
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.*
import kotlinx.coroutines.*
import java.io.*
import java.text.*
import java.util.*
import java.util.concurrent.*

class GPSCameraActivity : BaseActivity<ActivityGpsCameraBinding>(ActivityGpsCameraBinding::inflate, isFullScreen = true, isFullScreenIncludeNav = false) {
    private var imageCapture: ImageCapture? = null
    private var fusedLocationClient: FusedLocationProviderClient? = null
    private var aspectRatio = AspectRatio.RATIO_DEFAULT
    private var cameraExecutor: ExecutorService? = null
    private var currentLocation: LatLng? = null
    private var currentCameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
    private var currentFlashMode = ImageCapture.FLASH_MODE_OFF
    private var lastModified: File? = null
    private val locationPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        if (permissions.containsValue(false)) {
            incrementPermissionsDeniedCount("PERMISSION_LOCATION")
            Toast.makeText(this, "Location permission required", Toast.LENGTH_SHORT).show()
        } else {
            binding?.initExtra()
        }
    }
    private val cameraPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        if (permissions.containsValue(false)) {
            incrementPermissionsDeniedCount("CAMERA")
            Toast.makeText(this, "Camera permission required", Toast.LENGTH_SHORT).show()
        } else {
            binding?.initExtra()
        }
    }
    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            binding?.initExtra()
        } else {
            Toast.makeText(this, "Camera & Location permissions required", Toast.LENGTH_SHORT).show()
            incrementPermissionsDeniedCount("PERMISSION_CAMERA_LOCATION")
        }
    }
    private val appSettingsLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        binding?.initExtra()
    }

    override fun ActivityGpsCameraBinding.initExtra() {
        if (hasPermissions(LOCATION_PERMISSION + arrayOf(Manifest.permission.CAMERA))) {
            permissionLayout.beGone()
            textAddress.text = "Fetching.."
            delayed(1000L) {
                setupLocation()
            }
            setupCamera()
            val filesList = (File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), "GPS Camera"
            ).listFiles()?.toMutableList() ?: mutableListOf())
            filesList.sortByDescending { it.lastModified() }
            if (filesList.isNotEmpty()) {
                lastModified = filesList[0]
                Glide.with(applicationContext).load(lastModified?.absolutePath).transition(DrawableTransitionOptions.withCrossFade()).apply(
                    RequestOptions().dontTransform().dontAnimate().skipMemoryCache(false)
                ).into(imageLastCaptured)
            }
            viewNativeBanner(adNative)
        } else {
            permissionLayout.beVisible()
            buttonAllowAccess.setOnClickListener {
                val deniedCount = getPermissionsDeniedCount("PERMISSION_CAMERA_LOCATION")
                if (deniedCount < 2) {
                    permissionLauncher.launch(LOCATION_PERMISSION + arrayOf(Manifest.permission.CAMERA))
                } else {
                    viewPermissions {
                        val appSettingsIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", packageName, null)
                        }
                        appSettingsLauncher.launch(appSettingsIntent)
                    }
                }
            }
        }
    }

    private fun ActivityGpsCameraBinding.setupCamera() {
        cameraExecutor = Executors.newSingleThreadExecutor()
        startCamera()
    }

    @SuppressLint("MissingPermission")
    private fun setupLocation() {
        if (hasPermissions(LOCATION_PERMISSION)) {
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(this@GPSCameraActivity)
            fusedLocationClient?.lastLocation?.addOnSuccessListener { location ->
                location?.let {
                    currentLocation = LatLng(it.latitude, it.longitude)
                    currentLocation?.let { location ->
                        findAddressFromLatLng(location) { address ->
                            address?.apply {
                                val addressParts = address.getAddressLine(0)
                                    .split(",")
                                    .map { part -> part.trim().replace(Regex("^\\d+-\\s*"), "") } // Remove only leading "digits-"
                                val firstPositionName = addressParts.getOrNull(0)?.trim().orEmpty()
                                val formattedPositionName = if (firstPositionName.matches(Regex("^[0-9A-Z]+\\+[0-9A-Z]+$")) || firstPositionName.matches(Regex("^\\d+$"))) {
                                    ""
                                } else {
                                    firstPositionName
                                }
                                val secondFormattedPositionName = addressParts.getOrNull(1)?.trim().orEmpty()
                                val secondPositionName = if (secondFormattedPositionName.matches(Regex("^[0-9A-Z]+\\+[0-9A-Z]+$"))
                                    || secondFormattedPositionName.matches(Regex("^\\d+$"))
                                ) {
                                    ""
                                } else {
                                    secondFormattedPositionName
                                }
                                val subLocality = address.subLocality.orEmpty()
                                val locality = address.locality.orEmpty()
                                val adminArea = address.adminArea.orEmpty()
                                val postalCode = address.postalCode.orEmpty()
                                val mergedLocation = listOfNotNull(
                                    formattedPositionName.takeIf { it.isNotEmpty() },
                                    secondPositionName.takeIf { it.isNotEmpty() },
                                    subLocality.takeIf { it.isNotEmpty() },
                                    locality.takeIf { it.isNotEmpty() },
                                    adminArea.takeIf { it.isNotEmpty() },
                                    postalCode.takeIf { it.isNotEmpty() }
                                ).joinToString(", ")

                                binding?.apply {
                                    layoutMapDetail.beVisible()
                                    textAddress.text = mergedLocation
                                    textLatlong.text = "${location.latitude}, ${location.longitude}"
                                }
                            }
                        }
                    }
                }
            }
        } else {
            locationPermissionLauncher.launch(LOCATION_PERMISSION)
        }
    }

    private fun View.bitmapFromView(): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        draw(canvas)
        return bitmap
    }

    fun overlayBitmapAtBottom(background: Bitmap, overlay: Bitmap, sideSpacing: Int = 40): Bitmap {
        val desiredWidth = background.width - 2 * sideSpacing
        val scaleFactor = desiredWidth.toFloat() / overlay.width
        val desiredHeight = (overlay.height * scaleFactor).toInt()
        val scaledOverlay = Bitmap.createScaledBitmap(overlay, desiredWidth, desiredHeight, true)
        val resultBitmap = Bitmap.createBitmap(
            background.width, background.height,
            background.config ?: Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(resultBitmap)
        canvas.drawBitmap(background, 0f, 0f, null)
        val left = sideSpacing.toFloat()
        val top = (background.height - desiredHeight).toFloat() - sideSpacing
        canvas.drawBitmap(scaledOverlay, left, top, null)
        return resultBitmap
    }

    fun processPhotoToLandscape(photoPath: String): Boolean {
        val originalBitmap = BitmapFactory.decodeFile(photoPath) ?: return false
        val exif = ExifInterface(photoPath)
        val orientation = exif.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL
        )
        var rotationDegrees = when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90
            ExifInterface.ORIENTATION_ROTATE_180 -> 180
            ExifInterface.ORIENTATION_ROTATE_270 -> 270
            else -> 0
        }

        if (originalBitmap.width < originalBitmap.height && rotationDegrees == 0) {
            rotationDegrees = 90
        }
        val processedBitmap: Bitmap = if (rotationDegrees != 0) {
            val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
            Bitmap.createBitmap(
                originalBitmap, 0, 0,
                originalBitmap.width, originalBitmap.height, matrix, true
            )
        } else {
            originalBitmap
        }

        return try {
            val file = File(photoPath)
            FileOutputStream(file).use { outStream ->
                processedBitmap.compress(Bitmap.CompressFormat.JPEG, 90, outStream)
            }
            true
        } catch (e: IOException) {
            e.printStackTrace()
            false
        }
    }

    private fun ActivityGpsCameraBinding.startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this@GPSCameraActivity)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            bindPreview(cameraProvider)
        }, ContextCompat.getMainExecutor(this@GPSCameraActivity))
    }

    private fun ActivityGpsCameraBinding.bindPreview(cameraProvider: ProcessCameraProvider?) {
        val preview = Preview.Builder()
            .setTargetAspectRatio(aspectRatio)
            .build()
            .also {
                it.surfaceProvider = previewView.surfaceProvider
            }
        val flashVector = when (currentFlashMode) {
            ImageCapture.FLASH_MODE_AUTO -> R.drawable.ic_acion_flash_auto
            ImageCapture.FLASH_MODE_ON -> R.drawable.ic_acion_flash_on
            ImageCapture.FLASH_MODE_OFF -> R.drawable.ic_acion_flash_off
            else -> R.drawable.ic_acion_flash_off
        }
        buttonFlash.icon = ContextCompat.getDrawable(this@GPSCameraActivity, flashVector)

        imageCapture = ImageCapture.Builder()
            .setTargetAspectRatio(aspectRatio)
            .setFlashMode(currentFlashMode)
            .build()

        try {
            cameraProvider?.unbindAll()
            cameraProvider?.bindToLifecycle(
                this@GPSCameraActivity, currentCameraSelector, preview, imageCapture
            )
        } catch (exc: Exception) {
            Toast.makeText(
                this@GPSCameraActivity,
                "Camera binding failed",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun ActivityGpsCameraBinding.initListeners() {
        buttonCapture.setOnClickListener { takePhoto() }
        buttonClose.setOnClickListener { onBackPressedDispatcher.onBackPressed() }
        buttonFlash.setOnClickListener { toggleFlashMode() }
        buttonCollection.setOnClickListener {
            go(ViewCollectionActivity::class.java)
        }

        buttonChangeCamera.setOnClickListener { switchCamera() }
    }

    private fun ActivityGpsCameraBinding.switchCamera() {
        currentCameraSelector = if (currentCameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
            CameraSelector.DEFAULT_FRONT_CAMERA
        } else {
            CameraSelector.DEFAULT_BACK_CAMERA
        }
        startCamera()
    }

    private fun ActivityGpsCameraBinding.toggleFlashMode() {
        currentFlashMode = when (currentFlashMode) {
            ImageCapture.FLASH_MODE_AUTO -> ImageCapture.FLASH_MODE_ON
            ImageCapture.FLASH_MODE_ON -> ImageCapture.FLASH_MODE_OFF
            ImageCapture.FLASH_MODE_OFF -> ImageCapture.FLASH_MODE_AUTO
            else -> ImageCapture.FLASH_MODE_AUTO
        }
        imageCapture?.flashMode = currentFlashMode
        val flashVector = when (currentFlashMode) {
            ImageCapture.FLASH_MODE_AUTO -> R.drawable.ic_acion_flash_auto
            ImageCapture.FLASH_MODE_ON -> R.drawable.ic_acion_flash_on
            ImageCapture.FLASH_MODE_OFF -> R.drawable.ic_acion_flash_off
            else -> R.drawable.ic_acion_flash_off
        }
        buttonFlash.icon = ContextCompat.getDrawable(this@GPSCameraActivity, flashVector)
    }

    private fun ActivityGpsCameraBinding.takePhoto() {
        val imageCapture = imageCapture ?: return
        buttonCapture.disable()
        progressBar.beVisible()
        val outputDir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), "GPS Camera"
        ).apply { mkdirs() }
        val photoFile = File(
            outputDir,
            SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US)
                .format(System.currentTimeMillis()) + ".jpg"
        )
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this@GPSCameraActivity),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Toast.makeText(
                        this@GPSCameraActivity,
                        "Error: ${exc.message}",
                        Toast.LENGTH_SHORT
                    ).show()

                    buttonCapture.enable()
                    progressBar.beGone()
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    CoroutineScope(Dispatchers.IO).launch {
                        binding?.apply {
                            lastModified = photoFile
                            processPhotoToLandscape(photoFile.absolutePath)
                            val overlay = layoutMapDetail.bitmapFromView()
                            val bitmap = overlayBitmapAtBottom(BitmapFactory.decodeFile(photoFile.absolutePath), overlay)
                            FileOutputStream(photoFile).use { outStream ->
                                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outStream)
                            }
                            launch(Dispatchers.Main) {
                                Glide.with(applicationContext).load(lastModified?.absolutePath).transition(DrawableTransitionOptions.withCrossFade()).apply(
                                    RequestOptions().dontTransform().dontAnimate().skipMemoryCache(false)
                                ).into(imageLastCaptured)
                                buttonCapture.enable()
                                progressBar.beGone()
                            }
                        }
                    }
                }
            }
        )
    }

    override fun ActivityGpsCameraBinding.initView() {
        updateStatusBarColor(R.color.colorTransparent)
        updateNavigationBarColor(R.color.colorTransparent)

        onBackPressedDispatcher.addCallback {
            viewInterAdWithLogic {
                finish()
            }
        }
        controlBar.setOnApplyWindowInsetsListener { v: View, insets: WindowInsets ->
            v.setPadding(0, statusBarHeight, 0, navigationBarHeight)
            insets
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor?.shutdown()
    }
}