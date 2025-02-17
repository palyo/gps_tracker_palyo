package aanibrothers.tracker.io.model

import com.google.gson.annotations.*

data class NearLocation(
    @SerializedName("icon")
    val icon: Int,
    @SerializedName("title")
    val title: String,
)
