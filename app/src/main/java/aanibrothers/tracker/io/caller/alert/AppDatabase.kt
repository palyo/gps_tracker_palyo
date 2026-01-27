package aanibrothers.tracker.io.caller.alert

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import aanibrothers.tracker.io.caller.alert.Alert

@Database(entities = [Alert::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun alertDao(): AlertDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "alert_db"
                ).build().also { instance = it }
            }
        }
    }
}

