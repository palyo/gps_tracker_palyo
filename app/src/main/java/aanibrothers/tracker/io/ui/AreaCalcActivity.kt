package aanibrothers.tracker.io.ui

import aanibrothers.tracker.io.R
import aanibrothers.tracker.io.adapter.*
import aanibrothers.tracker.io.databinding.*
import aanibrothers.tracker.io.extension.*
import aanibrothers.tracker.io.module.*
import android.Manifest
import android.annotation.*
import android.content.*
import android.content.res.*
import android.graphics.*
import android.text.*
import android.view.*
import android.widget.*
import androidx.activity.*
import androidx.activity.result.contract.*
import androidx.core.content.*
import androidx.core.widget.*
import androidx.lifecycle.*
import androidx.recyclerview.widget.*
import coder.apps.space.library.base.*
import coder.apps.space.library.extension.*
import com.bumptech.glide.load.*
import com.google.android.gms.location.*
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.*
import com.google.maps.android.*
import com.google.maps.android.collections.*
import com.google.maps.android.ktx.utils.collection.*
import com.google.maps.android.ui.*
import kotlinx.coroutines.*
import org.json.*

class AreaCalcActivity : BaseActivity<ActivityAreaCalcBinding>(ActivityAreaCalcBinding::inflate, isFullScreen = true, isFullScreenIncludeNav = false), OnMapReadyCallback, GoogleMap.OnPoiClickListener {
    private var suggestionLocationAdapter: SuggestionLocationAdapter? = null
    private val TAG = "AreaCalcActivity"
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
    private var polygon: Polygon? = null
    private val areaBounds = ArrayList<LatLng>()
    private val boundMarkers = ArrayList<Marker>()
    private var resultAvailable = false
    private val locationPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        if (permissions.containsValue(false)) {
            incrementPermissionsDeniedCount("PERMISSION_LOCATION")
            Toast.makeText(this, "Location permission required", Toast.LENGTH_SHORT).show()
        } else {
            enableMyLocation()
        }
    }

    override fun ActivityAreaCalcBinding.initExtra() {
        val googleMapOptions = GoogleMapOptions()
            .compassEnabled(false)
            .rotateGesturesEnabled(true)
            .zoomControlsEnabled(false)
            .tiltGesturesEnabled(true).mapToolbarEnabled(false)
            .scrollGesturesEnabled(true)
        val fragment = SupportMapFragment.newInstance(googleMapOptions)

        supportFragmentManager.beginTransaction().replace(R.id.map_fragment, fragment).commit()
        fragment.getMapAsync(this@AreaCalcActivity)

        if (!isPremium && !hasPermissions(arrayOf(Manifest.permission.READ_PHONE_STATE))) viewNativeBanner(adNative) else adNative.beGone()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this@AreaCalcActivity)
        setupSuggestionAdapter()

        backgroundColor = tinyDb.getString("backgroundColor", backgroundColor) ?: backgroundColor
        cardColor = tinyDb.getString("cardColor", cardColor) ?: cardColor
        textColor = tinyDb.getString("textColor", textColor) ?: textColor
        mapFragment.alpha = 0f
    }

    private fun ActivityAreaCalcBinding.setupSuggestionAdapter() {
        recyclerView.apply {
            layoutManager = LinearLayoutManager(this@AreaCalcActivity, RecyclerView.VERTICAL, false)
            suggestionLocationAdapter = SuggestionLocationAdapter(this@AreaCalcActivity) {
                hideKeyboard(editSearch)
                cardSuggestedResult.beGone()
                addMarker(LatLng(it.latitude, it.longitude), "")
                moveToLocation(LatLng(it.latitude, it.longitude))
            }
            adapter = suggestionLocationAdapter
        }
    }

    override fun ActivityAreaCalcBinding.initListeners() {
        mapCurrentLocation.setOnClickListener {
            moveToCurrentLocation()
        }

        editSearch.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        editSearch.doAfterTextChanged {
            val query = editSearch.text.toString()
            filterQuery(query)
        }

        mapZoomIn.setOnClickListener { zoomMap(true) }
        mapZoomOut.setOnClickListener {
            zoomMap(false)
        }

        mapClear.setOnClickListener {
            if (googleMap != null) {
                mapClear.isSelected = false
                mapClear.disable()
                googleMap?.clear()
                areaBounds.clear()
                boundMarkers.clear()
                textArea.beGone()
                return@setOnClickListener
            } else {
                Toast.makeText(applicationContext, getString(R.string.message_please_add_mark_on_map), Toast.LENGTH_LONG).show()
            }
            Toast.makeText(applicationContext, getString(R.string.message_map_not_initialized_yet), Toast.LENGTH_SHORT).show()
        }
    }

    private fun ActivityAreaCalcBinding.filterQuery(query: String) {
        if (TextUtils.isEmpty(query)) {
            cardSuggestedResult.post {
                cardSuggestedResult.beGone()
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
                        }
                    }
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun ActivityAreaCalcBinding.moveToCurrentLocation() {
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

    private fun ActivityAreaCalcBinding.moveToLocation(latLong: LatLng) {
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

    private fun ActivityAreaCalcBinding.checkLocationDeviation() {
        currentLocation?.let { current ->
            val currentCameraPosition = googleMap?.cameraPosition?.target
            val distance = SphericalUtil.computeDistanceBetween(current, currentCameraPosition)

            if (distance > LOCATION_THRESHOLD) {
                isTracking = false
                mapCurrentLocation.isSelected = false
            }
        }
    }

    override fun ActivityAreaCalcBinding.initView() {
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
        viewMapType = "standard"
        backgroundColor = tinyDb.getString("backgroundColor", backgroundColor) ?: backgroundColor
        cardColor = tinyDb.getString("cardColor", cardColor) ?: cardColor
        textColor = tinyDb.getString("textColor", textColor) ?: textColor
        updateUIColors()
        googleMap?.setOnMapLoadedCallback {
            googleMap?.mapType = GoogleMap.MAP_TYPE_NORMAL
            binding?.apply {
                updateMapStyle()
                mapFragment.animate().alpha(1f).setDuration(500).start()
                progressBar.beGone()
            }
        }
        markerManager = MarkerManager(map)
        googleMap?.setOnMapClickListener { latLng: LatLng? ->
            try {
                if (latLng != null) {
                    areaBounds.add(latLng)
                    binding?.mapClear?.isSelected = true
                    binding?.mapClear?.enable()
                    val title = String(areaBounds.size.toString().toByteArray(), charset(Key.STRING_CHARSET_NAME))
                    googleMap?.addMarker(MarkerOptions().position(latLng).title(title))?.let { boundMarkers.add(it) }
                    val iconGenerator = IconGenerator(applicationContext)
                    iconGenerator.setBackground(ContextCompat.getDrawable(this@AreaCalcActivity, coder.apps.space.library.R.drawable.ic_bg_rounded))
                    val markerIcon: Bitmap = iconGenerator.makeIcon(" $title")
                    boundMarkers[boundMarkers.size - 1].isDraggable = true
                    boundMarkers[boundMarkers.size - 1].setIcon(BitmapDescriptorFactory.fromBitmap(markerIcon))
                    val polygonOptions = PolygonOptions()

                    for (l in areaBounds) {
                        polygonOptions.add(l)
                    }

                    if (areaBounds.size > 2) {
                        if (polygon != null) {
                            polygon?.remove()
                        }
                        polygon = null
                        polygon = googleMap?.addPolygon(polygonOptions.strokeColor(Color.argb(255, 49, 101, 187)).fillColor(Color.argb(100, 49, 101, 187)))
                        resultAvailable = true
                        val computeArea2: Double = SphericalUtil.computeArea(areaBounds)
                        val d2 = if (computeArea2 > 1000.0) computeArea2 / 1000.0 else 0.0
                        if (d2 != 0.0) {
                            binding?.textArea?.beVisible()
                            binding?.textArea?.text = TextUtils.concat(getString(R.string.measurement_area_in_km), String.format("%.4f", d2), " ", Html.fromHtml(" km<sup>2</sup>"))
                            return@setOnMapClickListener
                        }
                        binding?.textArea?.beVisible()
                        binding?.textArea?.text = TextUtils.concat(getString(R.string.measurement_area_in_meters), String.format("%.4f", computeArea2), " ", Html.fromHtml(" m<sup>2</sup>"))
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
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
            val bgColorParsed = Color.parseColor(backgroundColor)
            val textColorParsed = Color.parseColor(textColor)
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

            listOf(mapCurrentLocation, mapClear, mapZoomIn, mapZoomOut).forEach {
                it.backgroundTintList = bgTint
            }
            listOf(mapZoomIn, mapZoomOut).forEach {
                it.iconTint = textTint
            }
            listOf(mapCurrentLocation, mapClear, mapZoomIn, mapZoomOut).forEach {
                it.strokeColor = textOpacityTint
            }

            googleMap?.apply {
                mapCurrentLocation.iconTint = ContextCompat.getColorStateList(this@AreaCalcActivity, R.color.ic_action_accent_standard_selector)
                mapClear.iconTint = ContextCompat.getColorStateList(this@AreaCalcActivity, R.color.ic_action_accent_standard_selector)
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

    override fun onPoiClick(poi: PointOfInterest) {}
}