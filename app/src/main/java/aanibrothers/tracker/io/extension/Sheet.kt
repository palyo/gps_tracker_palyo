package aanibrothers.tracker.io.extension

import aanibrothers.tracker.io.R
import aanibrothers.tracker.io.databinding.LayoutSheetDeleteBinding
import aanibrothers.tracker.io.databinding.LayoutSheetPermissionBinding
import aanibrothers.tracker.io.databinding.LayoutSheetPermissionNotificationBinding
import aanibrothers.tracker.io.databinding.LayoutSheetPinGuideBinding
import aanibrothers.tracker.io.databinding.SheetMapMarkerDetailsBinding
import aanibrothers.tracker.io.databinding.SheetMapStyleBinding
import aanibrothers.tracker.io.databinding.SheetMapVisibilityStyleBinding
import android.app.Activity
import android.content.res.ColorStateList
import android.graphics.Color
import androidx.core.content.ContextCompat
import coder.apps.space.library.extension.applyDialogConfig
import coder.apps.space.library.extension.beGone
import coder.apps.space.library.extension.beVisibleIf
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

fun Activity.viewMapStylingSheet(
    isTrafficEnabled: Boolean,
    is3D: Boolean,
    selected: Int,
    viewMapType: String,
    listener: ((Int, String) -> Unit)?,
    detailListener: ((Boolean, String) -> Unit)?
) {
    val dialog =
        BottomSheetDialog(this, coder.apps.space.library.R.style.Theme_Space_BottomSheetDialogTheme)
    val binding = SheetMapStyleBinding.inflate(layoutInflater)
    dialog.setContentView(binding.root)
    dialog.window?.apply { applyDialogConfig() }

    binding.apply {
        sheetRoot.backgroundTintList = ColorStateList.valueOf(
            Color.parseColor(
                tinyDb.getString("backgroundColor", "#FFFFFF") ?: "#FFFFFF"
            )
        )
        textMapTypeTitle.setTextColor(
            Color.parseColor(
                tinyDb.getString("textColor", "#000000") ?: "#000000"
            )
        )
        textMapDetailsTitle.setTextColor(
            Color.parseColor(
                tinyDb.getString("textColor", "#000000") ?: "#000000"
            )
        )

        fun updateTint(bg: ColorStateList, content: ColorStateList) {
            listOf(
                mapNormal, mapSatellite, mapTerrain, mapSilver,
                mapRetro, mapDark, mapNight, mapAubergine,
                mapDetailTraffic, mapDetail3d
            ).forEach {
                it.backgroundTintList = bg
                it.setTextColor(content)
            }
        }

        fun updateSelection() {
            when (viewMapType) {
                "silver" -> {
                    val bg = ContextCompat.getColorStateList(
                        this@viewMapStylingSheet,
                        R.color.ic_action_bg_silver_selector
                    )
                    val content = ContextCompat.getColorStateList(
                        this@viewMapStylingSheet,
                        R.color.ic_action_content_silver_selector
                    )
                    if (bg != null && content != null) updateTint(bg, content)
                    mapSilver.isSelected = true
                }

                "retro" -> {
                    val bg = ContextCompat.getColorStateList(
                        this@viewMapStylingSheet,
                        R.color.ic_action_bg_retro_selector
                    )
                    val content = ContextCompat.getColorStateList(
                        this@viewMapStylingSheet,
                        R.color.ic_action_content_retro_selector
                    )
                    if (bg != null && content != null) updateTint(bg, content)
                    mapRetro.isSelected = true
                }

                "dark" -> {
                    val bg = ContextCompat.getColorStateList(
                        this@viewMapStylingSheet,
                        R.color.ic_action_bg_dark_selector
                    )
                    val content = ContextCompat.getColorStateList(
                        this@viewMapStylingSheet,
                        R.color.ic_action_content_dark_selector
                    )
                    if (bg != null && content != null) updateTint(bg, content)
                    mapDark.isSelected = true
                }

                "night" -> {
                    val bg = ContextCompat.getColorStateList(
                        this@viewMapStylingSheet,
                        R.color.ic_action_bg_night_selector
                    )
                    val content = ContextCompat.getColorStateList(
                        this@viewMapStylingSheet,
                        R.color.ic_action_content_night_selector
                    )
                    if (bg != null && content != null) updateTint(bg, content)
                    mapNight.isSelected = true
                }

                "aubergine" -> {
                    val bg = ContextCompat.getColorStateList(
                        this@viewMapStylingSheet,
                        R.color.ic_action_bg_aubergine_selector
                    )
                    val content = ContextCompat.getColorStateList(
                        this@viewMapStylingSheet,
                        R.color.ic_action_content_aubergine_selector
                    )
                    if (bg != null && content != null) updateTint(bg, content)
                    mapAubergine.isSelected = true
                }

                else -> {
                    if (selected == GoogleMap.MAP_TYPE_SATELLITE) {
                        val bg = ContextCompat.getColorStateList(
                            this@viewMapStylingSheet,
                            R.color.ic_action_bg_satellite_selector
                        )
                        val content = ContextCompat.getColorStateList(
                            this@viewMapStylingSheet,
                            R.color.ic_action_content_satellite_selector
                        )
                        if (bg != null && content != null) updateTint(bg, content)
                    } else {
                        val bg = ContextCompat.getColorStateList(
                            this@viewMapStylingSheet,
                            R.color.ic_action_bg_standard_selector
                        )
                        val content = ContextCompat.getColorStateList(
                            this@viewMapStylingSheet,
                            R.color.ic_action_content_standard_selector
                        )
                        if (bg != null && content != null) updateTint(bg, content)
                    }
                    mapNormal.isSelected = selected == GoogleMap.MAP_TYPE_NORMAL
                    mapSatellite.isSelected = selected == GoogleMap.MAP_TYPE_SATELLITE
                    mapTerrain.isSelected = selected == GoogleMap.MAP_TYPE_TERRAIN
                }
            }
            mapDetailTraffic.isSelected = isTrafficEnabled
            mapDetail3d.isSelected = is3D
        }

        updateSelection()

        mapNormal.setOnClickListener {
            listener?.invoke(GoogleMap.MAP_TYPE_NORMAL, "standard")
            dialog.dismiss()
        }
        mapSatellite.setOnClickListener {
            listener?.invoke(GoogleMap.MAP_TYPE_SATELLITE, "standard")
            dialog.dismiss()
        }
        mapTerrain.setOnClickListener {
            listener?.invoke(GoogleMap.MAP_TYPE_TERRAIN, "standard")
            dialog.dismiss()
        }
        mapSilver.setOnClickListener {
            listener?.invoke(GoogleMap.MAP_TYPE_NORMAL, "silver")
            dialog.dismiss()
        }
        mapRetro.setOnClickListener {
            listener?.invoke(GoogleMap.MAP_TYPE_NORMAL, "retro")
            dialog.dismiss()
        }
        mapDark.setOnClickListener {
            listener?.invoke(GoogleMap.MAP_TYPE_NORMAL, "dark")
            dialog.dismiss()
        }
        mapNight.setOnClickListener {
            listener?.invoke(GoogleMap.MAP_TYPE_NORMAL, "night")
            dialog.dismiss()
        }
        mapAubergine.setOnClickListener {
            listener?.invoke(GoogleMap.MAP_TYPE_NORMAL, "aubergine")
            dialog.dismiss()
        }
        mapDetailTraffic.setOnClickListener {
            detailListener?.invoke(!isTrafficEnabled, "traffic")
            dialog.dismiss()
        }
        mapDetail3d.setOnClickListener {
            detailListener?.invoke(!is3D, "3d")
            dialog.dismiss()
        }
    }
    if (!isFinishing) dialog.show()
}

fun Activity.viewMapVisibilitySheet(
    selected: Int,
    viewMapType: String,
    listener: (() -> Unit)?
) {
    val dialog =
        BottomSheetDialog(this, coder.apps.space.library.R.style.Theme_Space_BottomSheetDialogTheme)
    val binding = SheetMapVisibilityStyleBinding.inflate(layoutInflater)
    dialog.setContentView(binding.root)
    dialog.window?.apply { applyDialogConfig() }
    if (!isFinishing) dialog.show()
    val toggles = listOf(
        binding.mapAdministrativeCountry to ADMINISTRATIVE_COUNTRY,
        binding.mapAdministrativeProvince to ADMINISTRATIVE_PROVINCE,
        binding.mapAdministrativeLocality to ADMINISTRATIVE_LOCALITY,
        binding.mapAdministrativeNeighborhood to ADMINISTRATIVE_NEIGHBORHOOD,
        binding.mapAdministrativeLandParcel to ADMINISTRATIVE_LAND_PARCEL,
        binding.mapLandscapeManMade to LANDSCAPE_MAN_MADE,
        binding.mapLandscapeNatural to LANDSCAPE_NATURAL,
        binding.mapLandscapeNaturalLandcover to LANDSCAPE_NATURAL_LANDCOVER,
        binding.mapLandscapeNaturalTerrain to LANDSCAPE_NATURAL_TERRAIN,
        binding.mapPoiAttraction to POI_ATTRACTION,
        binding.mapPoiBusiness to POI_BUSINESS,
        binding.mapPoiGovernment to POI_GOVERNMENT,
        binding.mapPoiMedical to POI_MEDICAL,
        binding.mapPoiPark to POI_PARK,
        binding.mapPoiPlaceOfWorship to POI_PLACE_OF_WORSHIP,
        binding.mapPoiSchool to POI_SCHOOL,
        binding.mapPoiSportsComplex to POI_SPORTS_COMPLEX,
        binding.mapRoadHighway to ROAD_HIGHWAY,
        binding.mapRoadHighwayControlledAccess to ROAD_HIGHWAY_CONTROLLED_ACCESS,
        binding.mapRoadArterial to ROAD_ARTERIAL,
        binding.mapRoadLocal to ROAD_LOCAL,
        binding.mapTransitLine to TRANSIT_LINE,
        binding.mapTransitStation to TRANSIT_STATION,
        binding.mapTransitStationAirport to TRANSIT_STATION_AIRPORT,
        binding.mapTransitStationBus to TRANSIT_STATION_BUS,
        binding.mapTransitStationRail to TRANSIT_STATION_RAIL,
        binding.mapWater to WATER
    )

    tinyDb.apply {
        toggles.forEach { (button, key) ->
            button.isSelected = getBoolean(key, true)
        }
    }

    toggles.forEach { (button, key) ->
        button.setOnClickListener {
            tinyDb.putBoolean(key, !button.isSelected)
            button.isSelected = tinyDb.getBoolean(key, true)
        }
    }
    val bgColor = Color.parseColor(tinyDb.getString("backgroundColor", "#FFFFFF") ?: "#FFFFFF")
    val textColor = Color.parseColor(tinyDb.getString("textColor", "#000000") ?: "#000000")
    binding.sheetRoot.backgroundTintList = ColorStateList.valueOf(bgColor)
    listOf(
        binding.textMapAdministrative,
        binding.textMapLandscape,
        binding.textMapPoi,
        binding.textMapTransit,
        binding.textMapRoad,
        binding.textMapWater
    ).forEach { it.setTextColor(textColor) }

    fun updateTint(bg: ColorStateList, content: ColorStateList) {
        toggles.forEach { (button, _) ->
            button.backgroundTintList = bg
            button.setTextColor(content)
        }
    }

    fun updateSelection() {
        val (bgRes, contentRes) = when (viewMapType) {
            "silver" -> R.color.ic_action_bg_silver_selector to R.color.ic_action_content_silver_selector
            "retro" -> R.color.ic_action_bg_retro_selector to R.color.ic_action_content_retro_selector
            "dark" -> R.color.ic_action_bg_dark_selector to R.color.ic_action_content_dark_selector
            "night" -> R.color.ic_action_bg_night_selector to R.color.ic_action_content_night_selector
            "aubergine" -> R.color.ic_action_bg_aubergine_selector to R.color.ic_action_content_aubergine_selector
            else -> if (selected == GoogleMap.MAP_TYPE_SATELLITE)
                R.color.ic_action_bg_satellite_selector to R.color.ic_action_content_satellite_selector
            else
                R.color.ic_action_bg_standard_selector to R.color.ic_action_content_standard_selector
        }
        val bg = ContextCompat.getColorStateList(this, bgRes)
        val content = ContextCompat.getColorStateList(this, contentRes)
        if (bg != null && content != null) updateTint(bg, content)
    }

    updateSelection()

    binding.root.post { }

    dialog.setOnDismissListener {
        listener?.invoke()
    }
}

fun Activity.viewMapMarkerDetailsSheet(currentLocation: LatLng, marker: LatLng, name: String) {
    val dialog =
        BottomSheetDialog(this, coder.apps.space.library.R.style.Theme_Space_BottomSheetDialogTheme)
    val binding = SheetMapMarkerDetailsBinding.inflate(layoutInflater)
    dialog.setContentView(binding.root)
    dialog.window?.apply {
        applyDialogConfig()
    }

    if (!isFinishing) dialog.show()

    binding.apply {
        textMapLocationArea.text = "${marker.latitude}, ${marker.longitude}"
        actionClose.setOnClickListener {
            dialog.dismiss()
        }
        actionShare.setOnClickListener {
            shareLocation(latLong = marker)
            dialog.dismiss()
        }
        actionNavigation.setOnClickListener {
            navigateLocation(currentLatLng = currentLocation, markerLatLng = marker)
            dialog.dismiss()
        }
        CoroutineScope(Dispatchers.IO).launch {
            findAddressFromLatLng(marker) {
                it?.let {
                    runOnUiThread {
                        if (name.isEmpty()) {
                            val addressParts = it.getAddressLine(0)
                                .split(",")
                                .map { part ->
                                    part.trim().replace(Regex("^\\d+-\\s*"), "")
                                } // Remove only leading "digits-"
                            val firstPositionName = addressParts.getOrNull(0)?.trim().orEmpty()
                            val formattedPositionName =
                                if (firstPositionName.matches(Regex("^[0-9A-Z]+\\+[0-9A-Z]+$")) || firstPositionName.matches(
                                        Regex("^\\d+$")
                                    )
                                ) {
                                    ""
                                } else {
                                    firstPositionName
                                }
                            val secondPositionName = addressParts.getOrNull(1)?.trim().orEmpty()
                            val subLocality = it.subLocality.orEmpty()
                            val mergedLocation = listOfNotNull(
                                formattedPositionName.takeIf { it.isNotEmpty() },
                                secondPositionName.takeIf { it.isNotEmpty() },
                                subLocality.takeIf { it.isNotEmpty() }
                            ).joinToString(", ")

                            textMapLocationArea.text = mergedLocation
                            textMapLocationDetails.text = "${it.locality} ${it.adminArea}"
                            textMapDirectionTime.beGone()
                        } else {
                            val addressParts = it.getAddressLine(0)
                                .split(",")
                                .map { part ->
                                    part.trim().replace(Regex("^\\d+-\\s*"), "")
                                } // Remove only leading "digits-"
                            val firstPositionName = addressParts.getOrNull(0)?.trim().orEmpty()
                            val formattedPositionName =
                                if (firstPositionName.matches(Regex("^[0-9A-Z]+\\+[0-9A-Z]+$")) || firstPositionName.matches(
                                        Regex("^\\d+$")
                                    )
                                ) {
                                    ""
                                } else {
                                    firstPositionName
                                }
                            val secondPositionName = addressParts.getOrNull(1)?.trim().orEmpty()
                            val subLocality = it.subLocality.orEmpty()
                            val mergedLocation = listOfNotNull(
                                formattedPositionName.takeIf { it.isNotEmpty() },
                                secondPositionName.takeIf { it.isNotEmpty() },
                                subLocality.takeIf { it.isNotEmpty() }
                            ).joinToString(", ")
                            textMapLocationArea.text = "${name.replace("\n", " ")}\n$mergedLocation"
                            textMapLocationDetails.text = "${it.locality} ${it.adminArea}"
                            textMapDirectionTime.beGone()
                        }
                    }
                }
            }
        }
    }
}

fun Activity.viewNotificationPermission(listener: () -> Unit) {
    val dialog =
        BottomSheetDialog(this, coder.apps.space.library.R.style.Theme_Space_BottomSheetDialogTheme)
    val bindDialog: LayoutSheetPermissionNotificationBinding =
        LayoutSheetPermissionNotificationBinding.inflate(layoutInflater)
    dialog.setContentView(bindDialog.root)
    dialog.window?.apply {
        applyDialogConfig()
        setDimAmount(.24f)
    }

    with(bindDialog) {
        buttonContinue.setOnClickListener {
            listener.invoke()
            dialog.dismiss()
        }
    }

    if (!isFinishing || !isDestroyed) {
        dialog.show()
    }
}

fun Activity.viewPermission(
    title: String,
    body: String,
    positiveButton: String,
    isNegativeButton: Boolean,
    listener: (Boolean) -> Unit
) {
    val dialog =
        BottomSheetDialog(this, coder.apps.space.library.R.style.Theme_Space_BottomSheetDialogTheme)
    val bindDialog: LayoutSheetPermissionBinding =
        LayoutSheetPermissionBinding.inflate(layoutInflater)
    dialog.setContentView(bindDialog.root)
    dialog.window?.apply {
        applyDialogConfig()
        setDimAmount(.24f)
    }

    with(bindDialog) {
        textTitle.text = title
        textDescription.text = body
        actionPositive.text = positiveButton
        actionPositive.setOnClickListener {
            listener.invoke(true)
            dialog.dismiss()
        }
        actionNegative.beVisibleIf(isNegativeButton)
        actionNegative.setOnClickListener {
            listener.invoke(false)
            dialog.dismiss()
        }
    }

    if (!isFinishing || !isDestroyed) {
        dialog.show()
    }
}

fun Activity.viewTrashOrDeleteSheet(listener: () -> Unit) {
    val dialog =
        BottomSheetDialog(this, coder.apps.space.library.R.style.Theme_Space_BottomSheetDialogTheme)
    val binding = LayoutSheetDeleteBinding.inflate(layoutInflater)
    dialog.setContentView(binding.root)
    dialog.window?.apply {
        applyDialogConfig()
    }
    binding.apply {
        buttonRemove.setOnClickListener {
            dialog.dismiss()
            listener.invoke()
        }
        buttonCancel.setOnClickListener {
            dialog.dismiss()
        }
    }
    if (!isFinishing) dialog.show()
}

fun Activity.viewPinGuideSheet(listener: () -> Unit) {
    val dialog =
        BottomSheetDialog(this, coder.apps.space.library.R.style.Theme_Space_BottomSheetDialogTheme)
    val binding = LayoutSheetPinGuideBinding.inflate(layoutInflater)
    dialog.setContentView(binding.root)
    dialog.window?.apply {
        applyDialogConfig()
    }
    binding.apply {
        buttonRemove.setOnClickListener {
            dialog.dismiss()
            listener.invoke()
        }
    }
    if (!isFinishing) dialog.show()
}