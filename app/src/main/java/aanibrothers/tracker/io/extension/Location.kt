package aanibrothers.tracker.io.extension

import android.content.*
import android.location.*
import android.net.*
import android.os.*
import android.widget.*
import coder.apps.space.library.extension.*
import com.google.android.gms.maps.model.*
import com.simplemobiletools.commons.extensions.isPackageInstalled
import kotlinx.coroutines.*
import java.io.*
import java.util.*
import kotlin.math.*

fun Context.findAddressFromLatLng(latLng: LatLng, callback: (Address?) -> Unit) {
    val geocoder = Geocoder(this, Locale.getDefault())
    CoroutineScope(Dispatchers.IO).launch {
        try {
            val addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 5)?.toMutableList()
            val bestMatch = findBestMatch(addresses, latLng.latitude, latLng.longitude)
            if (bestMatch != null) {
                withContext(Dispatchers.Main) {
                    callback(bestMatch)
                }
            } else {
                withContext(Dispatchers.Main) {
                    callback(addresses?.firstOrNull())
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                callback(null)
            }
        }
    }
}

private fun haversineDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val R = 6371e3 // Earth radius in meters
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    return R * c // Distance in meters
}

fun findBestMatch(addresses: MutableList<Address>?, targetLat: Double, targetLon: Double): Address? {
    return addresses?.minByOrNull { haversineDistance(it.latitude, it.longitude, targetLat, targetLon) }
}

fun calculateDistance(currentLatLng: LatLng, markerLatLng: LatLng): String {
    val currentLat = currentLatLng.latitude
    val currentLng = currentLatLng.longitude
    val results = FloatArray(1)
    Location.distanceBetween(
        currentLat, currentLng,
        markerLatLng.latitude, markerLatLng.longitude,
        results
    )
    val distanceInMeters = results[0]
    "Distance".log("Distance: ${distanceInMeters} meters")
    val distanceInKm = distanceInMeters / 1000
    return "$distanceInKm km"
}

fun Context.shareLocation(latLong: LatLng) {
    val locationLink = "https://www.google.com/maps?q=${latLong.latitude},${latLong.longitude}"
    Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, "Check out this location: $locationLink")
        startActivity(Intent.createChooser(this, "Share location via"))
    }
}

fun Context.navigateLocation(currentLatLng: LatLng?, markerLatLng: LatLng?) {
    val uri = Uri.parse("https://www.google.com/maps/dir/?api=1&origin=${currentLatLng?.latitude},${currentLatLng?.longitude}&destination=${markerLatLng?.latitude},${markerLatLng?.longitude}")
    val intent = Intent(Intent.ACTION_VIEW, uri)

    // Check if Google Maps is installed
    val packageName = "com.google.android.apps.maps"
    if (isPackageInstalled(packageName)) {
        intent.setPackage(packageName)
    }

    try {
        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
        } else {
            Toast.makeText(this, "Google Maps is not installed", Toast.LENGTH_SHORT).show()
        }
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(this, "No app found to handle maps", Toast.LENGTH_SHORT).show()
    }
}

fun Context.navigateLocationByPlace(origin: String?, destination: String?) {
    val uri = Uri.parse("https://www.google.com/maps/dir/?api=1&origin=${Uri.encode(origin)}&destination=${Uri.encode(destination)}")
    val intent = Intent(Intent.ACTION_VIEW, uri)
    val packageName = "com.google.android.apps.maps"
    if (isPackageInstalled(packageName)) {
        intent.setPackage(packageName)
    }

    try {
        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
        } else {
            Toast.makeText(this, "Google Maps is not installed", Toast.LENGTH_SHORT).show()
        }
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(this, "No app found to handle maps", Toast.LENGTH_SHORT).show()
    }
}

fun Context.searchPlaceInGoogleMaps(query: String) {
    val uri = Uri.parse("geo:0,0?q=$query")
    val intent = Intent(Intent.ACTION_VIEW, uri)
    intent.setPackage("com.google.android.apps.maps")
    if (intent.resolveActivity(packageManager) != null) {
        startActivity(intent)
    } else {
        Toast.makeText(this, "Google Maps is not installed", Toast.LENGTH_SHORT).show()
    }
}

suspend fun Context.getSuggestedLocations(
    query: String,
    maxResults: Int = 5,
    callback: (MutableList<Address>) -> Unit
) {
    withContext(Dispatchers.IO) {
        val geocoder = Geocoder(this@getSuggestedLocations, Locale.getDefault())
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            geocoder.getFromLocationName(query, maxResults, object : Geocoder.GeocodeListener {
                override fun onGeocode(addresses: MutableList<Address>) {
                    callback(addresses)
                }

                override fun onError(errorMessage: String?) {
                    callback(mutableListOf())
                }
            })
        } else {
            try {
                @Suppress("DEPRECATION")
                callback(geocoder.getFromLocationName(query, maxResults)?.toMutableList() ?: mutableListOf())
            } catch (e: IOException) {
                e.printStackTrace()
                callback(mutableListOf())
            }
        }
    }
}
