package aanibrothers.tracker.io.caller.alert

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import aanibrothers.tracker.io.caller.alert.Alert

@Dao
interface AlertDao {
    @Insert
    suspend fun insert(alert: Alert)
    @Query("SELECT * FROM alerts") suspend fun getAll(): List<Alert>

    @Query("SELECT * FROM alerts WHERE timeInMillis >= :currentTime ORDER BY timeInMillis ASC")
    suspend fun getUpcoming(currentTime: Long): List<Alert>

    @Query("DELETE FROM alerts WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Query("DELETE FROM alerts")
    suspend fun deleteAll()

    @Query("DELETE FROM alerts WHERE timeInMillis < :currentTime")
    suspend fun deletePastReminders(currentTime: Long = System.currentTimeMillis())
}
