package aanibrothers.tracker.io.locations

import android.content.Context
import androidx.core.content.edit

object LocationPreference {
    private const val PREFS_NAME = "selected_location"
    private const val KEY_TITLE = "title"
    private const val KEY_LAT = "latitude"
    private const val KEY_LNG = "longitude"
    private const val KEY_ADDRESS = "address"
    private const val KEY_CUSTOM_ID = "custom_id"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun markCurrentLocation(context: Context) {
        prefs(context).edit {
            putString(KEY_TITLE, null)
                .putString(KEY_LAT, null)
                .putString(KEY_LNG, null)
                .putString(KEY_ADDRESS, null)
                .putLong(KEY_CUSTOM_ID, -1L)
        }
    }

    fun saveCustomLocation(context: Context, entity: SavedLocationEntity) {
        prefs(context).edit {
            putString(KEY_TITLE, entity.title)
                .putString(KEY_LAT, entity.latitude.toString())
                .putString(KEY_LNG, entity.longitude.toString())
                .putString(KEY_ADDRESS, entity.address)
                .putLong(KEY_CUSTOM_ID, entity.id)
        }
    }

    fun getSelectedLocation(context: Context): SelectedLocation? {
        val customId = prefs(context).getLong(KEY_CUSTOM_ID, -1L)
        return if (customId == -1L) null else {
            val title = prefs(context).getString(KEY_TITLE, "") ?: ""
            val lat = prefs(context).getString(KEY_LAT, "0")?.toDoubleOrNull() ?: 0.0
            val lng = prefs(context).getString(KEY_LNG, "0")?.toDoubleOrNull() ?: 0.0
            val address = prefs(context).getString(KEY_ADDRESS, "") ?: ""
            SelectedLocation(title, lat, lng, address, customId)
        }
    }
}

data class SelectedLocation(
    val title: String,
    val latitude: Double,
    val longitude: Double,
    val address: String,
    val customId: Long
)
