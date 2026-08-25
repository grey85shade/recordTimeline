package com.bcaste.lifetimeline

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import net.sqlcipher.database.SQLiteDatabase

@HiltAndroidApp
class LifeTimelineApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        SQLiteDatabase.loadLibs(this)
    }
}
