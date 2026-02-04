package aanibrothers.tracker.io.model

import android.content.Context
import coder.apps.space.library.helper.TinyDB

enum class OverlayTemplate {
    DEFAULT, CLASSIC, SQUARISE
}

fun getSelectedTemplate(context: Context): OverlayTemplate {
    return when (TinyDB(context).getString("template", "default")) {
        "classic" -> OverlayTemplate.CLASSIC
        "squarise" -> OverlayTemplate.SQUARISE
        else -> OverlayTemplate.DEFAULT
    }
}
data class OverlayState(
    val address: String = "Unknown",
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val altitudeMeters: Double? = null,
    val localTime: String = "",
    val gmtTime: String = "",
    val date: String = "",
    val startTimeMillis: Long = System.currentTimeMillis()
)

