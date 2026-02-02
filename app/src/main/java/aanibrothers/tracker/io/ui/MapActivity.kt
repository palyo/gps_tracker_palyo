package aanibrothers.tracker.io.ui

import aanibrothers.tracker.io.R
import aanibrothers.tracker.io.adapter.*
import aanibrothers.tracker.io.databinding.*
import aanibrothers.tracker.io.extension.*
import aanibrothers.tracker.io.module.*
import android.*
import android.annotation.*
import android.content.*
import android.content.res.*
import android.graphics.*
import android.net.*
import android.provider.*
import android.speech.*
import android.text.*
import android.view.*
import android.widget.*
import androidx.activity.*
import androidx.activity.result.contract.*
import androidx.core.content.*
import androidx.core.view.*
import androidx.core.widget.*
import androidx.lifecycle.*
import androidx.recyclerview.widget.*
import coder.apps.space.library.base.*
import coder.apps.space.library.extension.*
import com.google.android.gms.location.*
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.*
import com.google.maps.android.*
import com.google.maps.android.collections.*
import com.google.maps.android.ktx.utils.collection.*
import kotlinx.coroutines.*
import org.json.*
import androidx.core.graphics.toColorInt

class MapActivity : BaseActivity<ActivityMapBinding>(ActivityMapBinding::inflate, isFullScreen = true, isFullScreenIncludeNav = false), OnMapReadyCallback, GoogleMap.OnPoiClickListener {

    private var suggestionLocationAdapter: SuggestionLocationAdapter? = null
    private val TAG = "MapActivity"
    private var markerManager: MarkerManager? = null
    private var backgroundColor: String = "#FFFFFF"
    private var cardColor: String = "#EDEDED"
    private var textColor: String = "#000000"
    private var viewMapType: String = "standard"
    private var googleMap: GoogleMap? = null
    private var fusedLocationClient: FusedLocationProviderClient? = null
    private var isTracking = false
    private var currentLocation: LatLng? = null
    private val LOCATION_THRESHOLD = 500.0
    private var currentMarker: Marker? = null
    private var markerCollection: MarkerManager.Collection? = null
    private var maxZoom: Float = 0f
    private var minZoom: Float = 0f
    private val locationPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        if (permissions.containsValue(false)) {
            incrementPermissionsDeniedCount("PERMISSION_LOCATION")
            Toast.makeText(this, "Location permission required", Toast.LENGTH_SHORT).show()
        } else {
            enableMyLocation()
        }
    }
    private val microphonePermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        if (permissions.containsValue(false)) {
            incrementPermissionsDeniedCount("MICROPHONE")
            Toast.makeText(this, "Microphone permission required", Toast.LENGTH_SHORT).show()
        } else {
            startVoiceInput()
        }
    }
    private val appSettingsLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result -> }
    private val speakLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val matches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
        matches?.firstOrNull()?.let { placeName ->
            binding?.editSearch?.setText(placeName)
        }
    }

    override fun ActivityMapBinding.initExtra() {
        val googleMapOptions = GoogleMapOptions()
            .compassEnabled(false)
            .rotateGesturesEnabled(true)
            .zoomControlsEnabled(false)
            .tiltGesturesEnabled(true).mapToolbarEnabled(false)
            .scrollGesturesEnabled(true)
        val fragment = SupportMapFragment.newInstance(googleMapOptions)

        supportFragmentManager.beginTransaction().replace(R.id.map_fragment, fragment).commit()
        fragment.getMapAsync(this@MapActivity)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this@MapActivity)
        setupSuggestionAdapter()

        backgroundColor = tinyDb.getString("backgroundColor", backgroundColor) ?: backgroundColor
        cardColor = tinyDb.getString("cardColor", cardColor) ?: cardColor
        textColor = tinyDb.getString("textColor", textColor) ?: textColor
        mapFragment.alpha = 0f
    }

    private fun ActivityMapBinding.setupSuggestionAdapter() {
        recyclerView.apply {
            layoutManager = LinearLayoutManager(this@MapActivity, RecyclerView.VERTICAL, false)
            suggestionLocationAdapter = SuggestionLocationAdapter(this@MapActivity) {
                hideKeyboard(editSearch)
                cardSuggestedResult.beGone()
                addMarker(LatLng(it.latitude, it.longitude), "")
                moveToLocation(LatLng(it.latitude, it.longitude))
                mapNavigate.beEnableIf(currentMarker != null)
            }
            adapter = suggestionLocationAdapter
        }
    }

    override fun ActivityMapBinding.initListeners() {
        mapStyle.setOnClickListener {
            googleMap?.apply {
                viewMapStylingSheet(
                    isTrafficEnabled = googleMap?.isTrafficEnabled == true,
                    is3D = googleMap?.isBuildingsEnabled == true,
                    selected = mapType,
                    viewMapType = viewMapType,
                    listener = { style, type ->
                        mapType = style
                        viewMapType = type
                        tinyDb.putString("map_style", type)
                        updateMapStyle()
                    },
                ) { enable, action ->
                    when (action) {
                        "traffic" -> toggleTraffic(enable)
                        "3d" -> toggle3d(enable)
                        else -> {}
                    }
                }
            }
        }

        mapNavigate.setOnClickListener {
            searchPlaceInGoogleMaps(editSearch.text.toString())
        }
        mapVoiceNavigation.beVisibleIf(intent?.getBooleanExtra("is_voice_navigation", false) == true)
        mapVoiceNavigation.setOnClickListener {
            if (hasPermissions(arrayOf(Manifest.permission.RECORD_AUDIO))) {
                startVoiceInput()
            } else {
                microphonePermissionLauncher.launch(arrayOf(Manifest.permission.RECORD_AUDIO))
            }
        }

        mapVisibility.setOnClickListener {
            googleMap?.apply {
                viewMapVisibilitySheet(
                    selected = mapType,
                    viewMapType = viewMapType,
                ) {
                    updateMapStyle()
                }
            }
        }

        mapCurrentLocation.setOnClickListener {
            moveToCurrentLocation()
        }

        mapNavigate.disable()
        editSearch.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        editSearch.doAfterTextChanged {
            val query = editSearch.text.toString()
            cardProgress.beVisibleIf(!cardProgress.isVisible)
            filterQuery(query)
        }

        mapZoomIn.setOnClickListener { zoomMap(true) }
        mapZoomOut.setOnClickListener {
            zoomMap(false)
        }
    }

    private fun startVoiceInput() {
        if (hasPermissions(arrayOf(Manifest.permission.RECORD_AUDIO))) {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak the place name...")
            }
            speakLauncher.launch(intent)
        } else {
            if (fetchPermissionsDeniedCount("MICROPHONE") < 2 && !hasPermissions(arrayOf(Manifest.permission.RECORD_AUDIO))) {
                microphonePermissionLauncher.launch(arrayOf(Manifest.permission.RECORD_AUDIO))
                return
            } else {
                Toast.makeText(this@MapActivity, "Need Microphone permission enable manually..", Toast.LENGTH_SHORT).show()
                viewPermissions {
                    val appSettingsIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", packageName, null)
                    }
                    appSettingsLauncher.launch(appSettingsIntent)
                }
            }
        }
    }

    private fun ActivityMapBinding.filterQuery(query: String) {
        if (TextUtils.isEmpty(query)) {
            cardSuggestedResult.post {
                cardSuggestedResult.beGone()
                mapNavigate.disable()
                cardProgress.beGone()
            }
            recyclerView.post {
                suggestionLocationAdapter?.updateData(mutableListOf(), query, textColor)
            }
        } else {
            lifecycleScope.launch {
                getSuggestedLocations(query) {
                    runOnUiThread {
                        cardSuggestedResult.post {
                            cardSuggestedResult.beVisibleIf(it.isNotEmpty())
                            mapNavigate.beEnableIf(it.isEmpty())
                        }
                        recyclerView.post {
                            suggestionLocationAdapter?.updateData(it, query, textColor)
                            cardProgress.beGone()
                        }
                    }
                }
            }
        }
    }

    private fun toggleTraffic(enable: Boolean) {
        googleMap?.isTrafficEnabled = enable
    }

    private fun toggle3d(enable: Boolean) {
        googleMap?.isBuildingsEnabled = enable
        googleMap?.isIndoorEnabled = enable
    }

    @SuppressLint("MissingPermission")
    private fun ActivityMapBinding.moveToCurrentLocation() {
        if (hasPermissions(LOCATION_PERMISSION)) {
            fusedLocationClient?.lastLocation?.addOnSuccessListener { location ->
                location?.let {
                    currentLocation = LatLng(it.latitude, it.longitude)
                    currentLocation?.let {
                        moveToLocation(it)
                    }
                }
            }
        }
    }

    private fun ActivityMapBinding.moveToLocation(latLong: LatLng) {
        val cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLong, 15f)
        googleMap?.animateCamera(cameraUpdate, object : GoogleMap.CancelableCallback {
            override fun onFinish() {
                isTracking = true
                mapCurrentLocation.isSelected = true
            }

            override fun onCancel() {
                isTracking = false
                mapCurrentLocation.isSelected = false
            }
        })
    }

    private fun ActivityMapBinding.checkLocationDeviation() {
        currentLocation?.let { current ->
            val currentCameraPosition = googleMap?.cameraPosition?.target
            val distance = SphericalUtil.computeDistanceBetween(current, currentCameraPosition)

            if (distance > LOCATION_THRESHOLD) {
                isTracking = false
                mapCurrentLocation.isSelected = false
            }
        }
    }

    override fun ActivityMapBinding.initView() {
        updateStatusBarColor(R.color.colorTransparent)
        updateNavigationBarColor(R.color.colorTransparent)
        layoutController.setOnApplyWindowInsetsListener { v: View, insets: WindowInsets ->
            v.setPadding(0, statusBarHeight, 0, navigationBarHeight)
            insets
        }

        onBackPressedDispatcher.addCallback {
            viewInterAdWithLogic {
                finish()
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun enableMyLocation() {
        if (hasPermissions(LOCATION_PERMISSION)) {
            googleMap?.isMyLocationEnabled = true
            binding?.moveToCurrentLocation()
        } else {
            if (fetchPermissionsDeniedCount("PERMISSION_LOCATION") < 2 && !hasPermissions(LOCATION_PERMISSION)) {
                locationPermissionLauncher.launch(LOCATION_PERMISSION)
                return
            } else {
                Toast.makeText(this@MapActivity, "Need location permission enable manually..", Toast.LENGTH_SHORT).show()
                viewPermissions {
                    val appSettingsIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", packageName, null)
                    }
                    appSettingsLauncher.launch(appSettingsIntent)
                }
            }
        }
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        maxZoom = map.maxZoomLevel
        minZoom = map.minZoomLevel
        if (minZoom == 2F) {
            minZoom = 3F
        }
        googleMap?.mapType = GoogleMap.MAP_TYPE_NONE
        binding?.root?.setLayerType(View.LAYER_TYPE_HARDWARE, null)
        googleMap?.uiSettings?.isMyLocationButtonEnabled = false
        enableMyLocation()
        updateZoomButtons()
        googleMap?.setOnCameraMoveListener {
            if (isTracking) {
                binding?.checkLocationDeviation()
            }

            updateZoomButtons()
        }
        viewMapType = tinyDb.getString("map_style", "standard") ?: "standard"
        backgroundColor = tinyDb.getString("backgroundColor", backgroundColor) ?: backgroundColor
        cardColor = tinyDb.getString("cardColor", cardColor) ?: cardColor
        textColor = tinyDb.getString("textColor", textColor) ?: textColor
        updateUIColors()
        googleMap?.setOnMapLoadedCallback {
            googleMap?.mapType = when (viewMapType) {
                "standard" -> GoogleMap.MAP_TYPE_NORMAL
                "silver", "retro", "dark", "night", "aubergine" -> GoogleMap.MAP_TYPE_NORMAL
                else -> GoogleMap.MAP_TYPE_NORMAL
            }
            binding?.apply {
                updateMapStyle()
                mapFragment.animate().alpha(1f).setDuration(500).start()
                progressBar.beGone()
            }
        }
        markerManager = MarkerManager(map)

        googleMap?.setOnMapLongClickListener { latLng ->
            addMarker(latLng, "")
        }

        googleMap?.setOnPoiClickListener { poi ->
            addMarker(poi.latLng, poi.name)
        }
        binding?.moveToCurrentLocation()
    }

    private fun updateZoomButtons() {
        binding?.apply {
            val currentZoom = googleMap?.cameraPosition?.zoom ?: 0F
            mapZoomIn.beEnableIf(currentZoom < maxZoom)
            mapZoomOut.beEnableIf(currentZoom > minZoom)
        }
    }

    private fun zoomMap(zoomIn: Boolean) {
        val currentZoom = googleMap?.cameraPosition?.zoom ?: 0F
        val newZoom = if (zoomIn) currentZoom + 1 else currentZoom - 1
        googleMap?.animateCamera(CameraUpdateFactory.zoomTo(newZoom))
    }

    private fun updateUIColors() {
        binding?.apply {
            val bgColorParsed = backgroundColor.toColorInt()
            val textColorParsed = textColor.toColorInt()
            val textOpacity = textColor.applyOpacity(0.5F)
            val bgTint = ColorStateList.valueOf(bgColorParsed)
            val textTint = ColorStateList.valueOf(textColorParsed)
            val textOpacityTint = ColorStateList.valueOf(textOpacity)

            mapFragment.setBackgroundColor(bgColorParsed)
            cardSearch.setCardBackgroundColor(bgColorParsed)
            cardSearch.strokeColor = textOpacity
            cardSuggestedResult.setCardBackgroundColor(bgColorParsed)
            cardSuggestedResult.strokeColor = textOpacity

            editSearch.apply {
                setHintTextColor(textOpacity)
                setTextColor(textColorParsed)
                compoundDrawableTintList = textTint
            }

            listOf(mapNavigate, mapStyle, mapVoiceNavigation, mapVisibility, mapCurrentLocation, mapZoomIn, mapZoomOut).forEach {
                it.backgroundTintList = bgTint
            }
            listOf(mapNavigate, mapStyle, mapVisibility, mapVoiceNavigation, mapZoomIn, mapZoomOut).forEach {
                it.iconTint = textTint
            }
            listOf(mapNavigate, mapStyle, mapVoiceNavigation, mapVisibility, mapCurrentLocation, mapZoomIn, mapZoomOut).forEach {
                it.strokeColor = textOpacityTint
            }

            googleMap?.apply {
                mapCurrentLocation.iconTint = when (viewMapType) {
                    "silver" -> ContextCompat.getColorStateList(this@MapActivity, R.color.ic_action_accent_silver_selector)
                    "retro" -> ContextCompat.getColorStateList(this@MapActivity, R.color.ic_action_accent_retro_selector)
                    "dark" -> ContextCompat.getColorStateList(this@MapActivity, R.color.ic_action_accent_dark_selector)
                    "night" -> ContextCompat.getColorStateList(this@MapActivity, R.color.ic_action_accent_night_selector)
                    "aubergine" -> ContextCompat.getColorStateList(this@MapActivity, R.color.ic_action_accent_aubergine_selector)
                    else -> if (mapType == GoogleMap.MAP_TYPE_SATELLITE)
                        ContextCompat.getColorStateList(this@MapActivity, R.color.ic_action_accent_satellite_selector)
                    else
                        ContextCompat.getColorStateList(this@MapActivity, R.color.ic_action_accent_standard_selector)
                }
            }
        }
    }

    private fun addMarker(latLng: LatLng, name: String) {
        if (markerCollection == null) {
            markerCollection = markerManager?.newCollection()
        }

        googleMap?.setOnMarkerClickListener { marker ->
            currentLocation?.let { viewMapMarkerDetailsSheet(it, latLng, name) }
            true
        }
        currentMarker?.remove()

        currentMarker = markerCollection?.addMarker {
            position(latLng)
            icon(vectorToBitmap(R.drawable.ic_icon_marker_pin))
        }
        if (tinyDb.getBoolean("pin_guide_enabled", true)) {
            viewPinGuideSheet {
                tinyDb.putBoolean("pin_guide_enabled", false)
            }
        }
    }

    private fun updateMapStyle() {
        data class MapStyleConfig(
            val backgroundColor: String,
            val cardColor: String,
            val textColor: String,
            val styleRes: Int,
            val updateIcons: () -> Unit,
            val delayed: Boolean = true,
            val doubleSetup: Boolean = false,
        )

        val config = if (viewMapType == "standard") {
            val iconsUpdate = {
                when (googleMap?.mapType) {
                    GoogleMap.MAP_TYPE_NORMAL -> window.updateStatusBarIcons(true)
                    GoogleMap.MAP_TYPE_SATELLITE -> window.updateStatusBarIcons(false)
                    else -> Unit
                }
            }
            MapStyleConfig(
                backgroundColor = "#FFFFFF",
                cardColor = "#F8F7F7",
                textColor = "#3C4043",
                styleRes = R.raw.standard_map_style,
                updateIcons = iconsUpdate
            )
        } else {
            when (viewMapType) {
                "silver" -> MapStyleConfig(
                    backgroundColor = "#FFFFFF",
                    cardColor = "#F5F5F5",
                    textColor = "#9D9D9D",
                    styleRes = R.raw.silver_map_style,
                    updateIcons = { window.updateStatusBarIcons(true) }
                )

                "retro" -> MapStyleConfig(
                    backgroundColor = "#FDFCF8",
                    cardColor = "#EBE3CD",
                    textColor = "#7E6B63",
                    styleRes = R.raw.retro_map_style,
                    updateIcons = { window.updateStatusBarIcons(true) },
                    delayed = false,
                    doubleSetup = true
                )

                "dark" -> MapStyleConfig(
                    backgroundColor = "#000000",
                    cardColor = "#212121",
                    textColor = "#FFFFFF",
                    styleRes = R.raw.dark_map_style,
                    updateIcons = { window.updateStatusBarIcons(false) }
                )

                "night" -> MapStyleConfig(
                    backgroundColor = "#38414E",
                    cardColor = "#242F3E",
                    textColor = "#9BA3B3",
                    styleRes = R.raw.night_map_style,
                    updateIcons = { window.updateStatusBarIcons(false) },
                    doubleSetup = true
                )

                "aubergine" -> MapStyleConfig(
                    backgroundColor = "#304A7D",
                    cardColor = "#1D2C4D",
                    textColor = "#99A5BC",
                    styleRes = R.raw.aubergine_map_style,
                    updateIcons = { window.updateStatusBarIcons(false) }
                )

                else -> error("Unsupported map type")
            }
        }

        backgroundColor = config.backgroundColor
        cardColor = config.cardColor
        textColor = config.textColor

        setupMapStyle(config.styleRes)
        if (config.doubleSetup) setupMapStyle(config.styleRes)

        if (config.delayed) {
            delayed(500L) { config.updateIcons() }
        } else {
            config.updateIcons()
        }

        tinyDb.putString("backgroundColor", backgroundColor)
        tinyDb.putString("cardColor", cardColor)
        tinyDb.putString("textColor", textColor)
    }

    private fun Context.getDynamicMapStyle(style: Int): MapStyleOptions {
        val inputStream = resources.openRawResource(style)
        val jsonString = inputStream.bufferedReader().use { it.readText() }
        val jsonArray = JSONArray(jsonString)

        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.optJSONObject(i)
            if (obj != null && VISIBILITY.contains(obj.optString("featureType"))) {
                val featureType = obj.optString("featureType")
                val stylers = obj.optJSONArray("stylers") ?: continue
                for (j in 0 until stylers.length()) {
                    val newStyle = stylers.optJSONObject(j) ?: continue
                    if (newStyle.has("visibility")) {
                        val visibility = if (tinyDb.getBoolean(featureType, value = true)) "on" else "off"
                        newStyle.put("visibility", visibility)
                    }
                }
            }
        }

        return MapStyleOptions(jsonArray.toString())
    }

    private fun setupMapStyle(style: Int) {
        try {
            val success = googleMap?.setMapStyle(getDynamicMapStyle(style))
            if (success == true) {
                updateUIColors()
            }
        } catch (e: Resources.NotFoundException) {
            TAG.log("Can't find style. Error: $e")
        }
    }

    override fun onPoiClick(poi: PointOfInterest) {

    }
}