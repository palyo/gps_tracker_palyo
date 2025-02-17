package aanibrothers.tracker.io.model

import android.os.*
import com.google.gson.annotations.*
import kotlinx.parcelize.*

@Parcelize
data class NominatimResult(
    @SerializedName("place_id")
    val placeId: String,
    @SerializedName("licence")
    val licence: String,
    @SerializedName("osm_type")
    val osmType: String,
    @SerializedName("osm_id")
    val osmId: String,
    @SerializedName("boundingbox")
    val boundingbox: List<String>,
    @SerializedName("lat")
    val lat: String,
    @SerializedName("lon")
    val lon: String,
    @SerializedName("display_name")
    val displayName: String,
    @SerializedName("class")
    val classType: String,
    @SerializedName("type")
    val type: String,
    @SerializedName("importance")
    val importance: Double,
    @SerializedName("address")
    val address: Address
) : Parcelable

@Parcelize
data class Address(
    @SerializedName("house_number")
    val houseNumber: String?,
    @SerializedName("road")
    val road: String?,
    @SerializedName("suburb")
    val suburb: String?,
    @SerializedName("city")
    val city: String?,
    @SerializedName("state")
    val state: String?,
    @SerializedName("postcode")
    val postcode: String?,
    @SerializedName("country")
    val country: String?,
    @SerializedName("country_code")
    val countryCode: String?
) : Parcelable