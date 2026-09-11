package org.tinkerhub.scrollmeter.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [ScrollSession::class, DailyStat::class],
    version = 1,
    exportSchema = false
)
abstract class ScrollMeterDatabase : RoomDatabase() {

    abstract fun scrollDao(): ScrollDao

    companion object {
        @Volatile
        private var INSTANCE: ScrollMeterDatabase? = null

        fun getDatabase(context: Context): ScrollMeterDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ScrollMeterDatabase::class.java,
                    "scroll_meter_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
