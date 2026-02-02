package aanibrothers.tracker.io.locations

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface SavedLocationDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(location: SavedLocationEntity)

    @Query("SELECT * FROM saved_locations ORDER BY createdAt DESC")
    suspend fun getAll(): List<SavedLocationEntity>

    @Query("SELECT * FROM saved_locations WHERE isDefault = 1 LIMIT 1")
    suspend fun getDefault(): SavedLocationEntity?

    @Query("UPDATE saved_locations SET isDefault = 0")
    suspend fun clearDefault()

    @Query("UPDATE saved_locations SET isDefault = 1 WHERE id = :id")
    suspend fun setDefault(id: Long)
}
