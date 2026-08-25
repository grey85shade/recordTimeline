package com.bcaste.lifetimeline.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.bcaste.lifetimeline.data.local.dao.CategoryDao
import com.bcaste.lifetimeline.data.local.dao.ProfileDao
import com.bcaste.lifetimeline.data.local.dao.TimelineDao
import com.bcaste.lifetimeline.data.local.entity.*

@Database(
    entities = [
        TimelineEvent::class,
        Category::class,
        EventImage::class,
        EventCategoryCrossRef::class,
        Profile::class
    ],
    version = 3,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun timelineDao(): TimelineDao
    abstract fun categoryDao(): CategoryDao
    abstract fun profileDao(): ProfileDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE timeline_events ADD COLUMN eventType TEXT NOT NULL DEFAULT 'POINT'")
                db.execSQL("ALTER TABLE timeline_events ADD COLUMN endTimestamp INTEGER DEFAULT NULL")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Create profiles table
                db.execSQL("CREATE TABLE IF NOT EXISTS profiles (id TEXT NOT NULL, name TEXT NOT NULL, isVisibleInMain INTEGER NOT NULL, color TEXT NOT NULL, PRIMARY KEY(id))")
                
                // 2. Add profileId column to timeline_events
                db.execSQL("ALTER TABLE timeline_events ADD COLUMN profileId TEXT NOT NULL DEFAULT 'main'")
                
                // 3. Add profileId column to categories
                db.execSQL("ALTER TABLE categories ADD COLUMN profileId TEXT NOT NULL DEFAULT 'main'")
                
                // 4. Insert default main profile
                db.execSQL("INSERT OR IGNORE INTO profiles (id, name, isVisibleInMain, color) VALUES ('main', 'Principal', 1, '#3D82F5')")
            }
        }
    }
}
