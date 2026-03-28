package aanibrothers.tracker.io.caller.alert

import aanibrothers.tracker.io.locations.SavedLocationDao
import aanibrothers.tracker.io.locations.SavedLocationEntity
import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [SavedLocationEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun savedLocationDao(): SavedLocationDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "gpsTracker_db"
                ).build().also { instance = it }
            }
        }
    }
}

