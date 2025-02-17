package aanibrothers.tracker.io.ui

import aanibrothers.tracker.io.R
import aanibrothers.tracker.io.adapter.*
import aanibrothers.tracker.io.databinding.*
import aanibrothers.tracker.io.extension.*
import android.annotation.*
import android.content.*
import android.content.res.*
import android.graphics.*
import android.text.*
import android.view.*
import android.widget.*
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

class RouteActivity : BaseActivity<ActivityRoutesBinding>(ActivityRoutesBinding::inflate, isFullScreen = true, isFullScreenIncludeNav = true), OnMapReadyCallback, GoogleMap.OnPoiClickListener {
    private var isSource: Boolean = false
    private var suggestionLocationAdapter: SuggestionLocationAdapter? = null
    private val TAG = "VoiceNavigationActivity"
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
    private var currentMarkerSource: Marker? = null
    private var currentMarkerDestination: Marker? = null
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

    override fun ActivityRoutesBinding.initExtra() {
        val googleMapOptions = GoogleMapOptions().compassEnabled(true).rotateGesturesEnabled(true).zoomControlsEnabled(false).tiltGesturesEnabled(true).mapToolbarEnabled(false).scrollGesturesEnabled(true)
        val fragment = SupportMapFragment.newInstance(googleMapOptions)

        supportFragmentManager.beginTransaction().replace(R.id.map_fragment, fragment).commit()
        fragment.getMapAsync(this@RouteActivity)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this@RouteActivity)
        setupSuggestionAdapter()

        backgroundColor = tinyDb.getString("backgroundColor", backgroundColor) ?: backgroundColor
        cardColor = tinyDb.getString("cardColor", cardColor) ?: cardColor
        textColor = tinyDb.getString("textColor", textColor) ?: textColor
        mapFragment.alpha = 0f
    }

    private fun ActivityRoutesBinding.setupSuggestionAdapter() {
        recyclerView.apply {
            layoutManager = LinearLayoutManager(this@RouteActivity, RecyclerView.VERTICAL, false)
            suggestionLocationAdapter = SuggestionLocationAdapter(this@RouteActivity) {
                if (isSource) {
                    hideKeyboard(editSearchSource)
                    cardSuggestedResult.beGone()
                    addMarker(LatLng(it.latitude, it.longitude), isSource)
                    moveToLocation(LatLng(it.latitude, it.longitude))
                } else {
                    hideKeyboard(editSearchDestination)
                    cardSuggestedResult.beGone()
                    addMarker(LatLng(it.latitude, it.longitude), isSource)
                    moveToLocation(LatLng(it.latitude, it.longitude))
                }
            }
            adapter = suggestionLocationAdapter
        }
    }

    override fun ActivityRoutesBinding.initListeners() {
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
            if (currentMarkerSource != null && currentMarkerDestination != null) {
                navigateLocationByPlace(origin = editSearchSource.text.toString(), destination = editSearchDestination.text.toString())
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

        editSearchSource.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        editSearchSource.doAfterTextChanged {
            val query = editSearchSource.text.toString()
            cardProgress.beVisibleIf(!cardProgress.isVisible)
            filterQuery(query)
        }
        editSearchDestination.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        editSearchDestination.doAfterTextChanged {
            val query = editSearchDestination.text.toString()
            cardProgress.beVisibleIf(!cardProgress.isVisible)
            filterQuery(query)
        }

        editSearchSource.setOnFocusChangeListener { v, hasFocus ->
            isSource = hasFocus
        }

        mapZoomIn.setOnClickListener { zoomMap(true) }
        mapZoomOut.setOnClickListener {
            zoomMap(false)
        }
    }

    private fun ActivityRoutesBinding.filterQuery(query: String) {
        if (TextUtils.isEmpty(query)) {
            cardSuggestedResult.post {
                cardSuggestedResult.beGone()
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
    private fun ActivityRoutesBinding.moveToCurrentLocation() {
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

    private fun ActivityRoutesBinding.moveToLocation(latLong: LatLng) {
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

    private fun ActivityRoutesBinding.checkLocationDeviation() {
        currentLocation?.let { current ->
            val currentCameraPosition = googleMap?.cameraPosition?.target
            val distance = SphericalUtil.computeDistanceBetween(current, currentCameraPosition)

            if (distance > LOCATION_THRESHOLD) {
                isTracking = false
                mapCurrentLocation.isSelected = false
            }
        }
    }

    override fun ActivityRoutesBinding.initView() {
        updateStatusBarColor(R.color.colorTransparent)
        updateNavigationBarColor(R.color.colorTransparent)
        layoutController.setOnApplyWindowInsetsListener { v: View, insets: WindowInsets ->
            v.setPadding(0, statusBarHeight, 0, navigationBarHeight)
            insets
        }
    }

    @SuppressLint("MissingPermission")
    private fun enableMyLocation() {
        if (hasPermissions(LOCATION_PERMISSION)) {
            googleMap?.isMyLocationEnabled = true
            binding?.moveToCurrentLocation()
        } else {
            locationPermissionLauncher.launch(LOCATION_PERMISSION)
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
            addMarker(latLng, isSource)
        }

        googleMap?.setOnPoiClickListener { poi ->
            addMarker(poi.latLng, isSource,poi.name)
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
            val bgColor = Color.parseColor(backgroundColor)
            val txtColor = Color.parseColor(textColor)
            val txtColorOpacity = textColor.applyOpacity(0.5F)

            // Apply background colors
            mapFragment.setBackgroundColor(bgColor)

            listOf(cardSearchSource, cardSearchDestination, cardSuggestedResult).forEach {
                it.setCardBackgroundColor(bgColor)
                it.strokeColor = txtColorOpacity
            }

            listOf(editSearchSource, editSearchDestination).forEach {
                it.setHintTextColor(txtColorOpacity)
                it.setTextColor(txtColor)
                it.compoundDrawableTintList = ColorStateList.valueOf(txtColor)
            }

            listOf(mapNavigate, mapStyle, mapVisibility, mapCurrentLocation, mapZoomIn, mapZoomOut).forEach {
                it.backgroundTintList = ColorStateList.valueOf(bgColor)
                it.strokeColor = ColorStateList.valueOf(txtColorOpacity)
            }

            listOf(mapNavigate, mapStyle, mapVisibility, mapZoomIn, mapZoomOut).forEach {
                it.iconTint = ColorStateList.valueOf(txtColor)
            }

            googleMap?.apply {
                mapCurrentLocation.iconTint = ContextCompat.getColorStateList(
                    this@RouteActivity,
                    when (viewMapType) {
                        "silver" -> R.color.ic_action_accent_silver_selector
                        "retro" -> R.color.ic_action_accent_retro_selector
                        "dark" -> R.color.ic_action_accent_dark_selector
                        "night" -> R.color.ic_action_accent_night_selector
                        "aubergine" -> R.color.ic_action_accent_aubergine_selector
                        else -> if (mapType == GoogleMap.MAP_TYPE_SATELLITE) {
                            R.color.ic_action_accent_satellite_selector
                        } else {
                            R.color.ic_action_accent_standard_selector
                        }
                    }
                )
            }
        }
    }

    private fun addMarker(latLng: LatLng, isSource: Boolean,name:String? = "") {
        if (markerCollection == null) {
            markerCollection = markerManager?.newCollection()
        }

        googleMap?.setOnMarkerClickListener { marker ->
            currentLocation?.let { viewMapMarkerDetailsSheet(it, latLng, name.toString()) }
            true
        }
        if (isSource) {
            currentMarkerSource?.remove()
            currentMarkerSource = markerCollection?.addMarker {
                position(latLng)
                icon(vectorToBitmap(R.drawable.ic_icon_marker_pin))
            }
        } else {
            currentMarkerDestination?.remove()
            currentMarkerDestination = markerCollection?.addMarker {
                position(latLng)
                icon(vectorToBitmap(R.drawable.ic_icon_marker_pin))
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
            val doubleSetup: Boolean = false
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
        Toast.makeText(
            this, """Clicked: ${poi.name}
            Place ID:${poi.placeId}
            Latitude:${poi.latLng.latitude} Longitude:${poi.latLng.longitude}""", Toast.LENGTH_SHORT
        ).show()
    }

    private fun drawRouteOnMap(encodedPolyline: String) {
        val decodedPath = PolyUtil.decode(encodedPolyline)
        googleMap?.addPolyline(
            PolylineOptions().addAll(decodedPath).width(10f).color(Color.BLUE)
        )
    }
}