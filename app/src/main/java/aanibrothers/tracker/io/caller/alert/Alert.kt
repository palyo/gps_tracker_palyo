package aanibrothers.tracker.io.caller.alert

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "alerts")
data class Alert(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val timeInMillis: Long,
    val requestCode: Int
)
