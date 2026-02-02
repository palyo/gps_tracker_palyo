package aanibrothers.tracker.io.locations

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_locations")
data class SavedLocationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val title: String,
    val latitude: Double,
    val longitude: Double,
    val address: String,

    val isDefault: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
