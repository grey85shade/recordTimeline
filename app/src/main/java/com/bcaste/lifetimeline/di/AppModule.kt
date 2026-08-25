package com.bcaste.lifetimeline.di

import android.content.Context
import androidx.room.Room
import com.bcaste.lifetimeline.data.local.AppDatabase
import com.bcaste.lifetimeline.data.local.dao.CategoryDao
import com.bcaste.lifetimeline.data.local.dao.ProfileDao
import com.bcaste.lifetimeline.data.local.dao.TimelineDao
import com.bcaste.lifetimeline.BuildConfig
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import net.sqlcipher.database.SupportFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideFirebaseAnalytics(@ApplicationContext context: Context): FirebaseAnalytics {
        return Firebase.analytics
    }

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        val passphrase = BuildConfig.DB_PASSPHRASE.toByteArray(Charsets.UTF_8)
        val supportFactory = SupportFactory(passphrase)

        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "lifetimeline.db"
        )
        .openHelperFactory(supportFactory)
        .addMigrations(AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3)
        .build()
    }

    @Provides
    fun provideTimelineDao(database: AppDatabase): TimelineDao {
        return database.timelineDao()
    }

    @Provides
    fun provideCategoryDao(database: AppDatabase): CategoryDao {
        return database.categoryDao()
    }

    @Provides
    fun provideProfileDao(database: AppDatabase): ProfileDao {
        return database.profileDao()
    }

    @Provides
    fun provideTimelineRepository(
        timelineDao: TimelineDao,
        categoryDao: CategoryDao,
        profileDao: ProfileDao,
        imageStorageManager: com.bcaste.lifetimeline.data.ImageStorageManager
    ): com.bcaste.lifetimeline.data.TimelineRepository {
        return com.bcaste.lifetimeline.data.TimelineRepositoryImpl(timelineDao, categoryDao, profileDao, imageStorageManager)
    }
}
