package com.callbackdev.passo.core.data.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import com.callbackdev.passo.core.data.db.PassoDatabase
import com.callbackdev.passo.core.data.db.SessionDao
import com.callbackdev.passo.core.data.db.TrackingDao
import com.callbackdev.passo.core.data.prefs.UserPreferencesDataSource
import com.callbackdev.passo.core.data.time.TodaySource
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {
    @Provides
    @Singleton
    fun database(@ApplicationContext context: Context): PassoDatabase =
        Room.databaseBuilder(context, PassoDatabase::class.java, PassoDatabase.NAME).build()

    @Provides
    fun trackingDao(database: PassoDatabase): TrackingDao = database.trackingDao()

    @Provides
    fun sessionDao(database: PassoDatabase): SessionDao = database.sessionDao()

    /**
     * One instance per process, as DataStore requires. A corrupt file is replaced by an empty
     * one: the reader loses their settings, not their steps, and the app keeps counting.
     */
    @Provides
    @Singleton
    fun preferencesDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
            produceFile = { context.preferencesDataStoreFile(UserPreferencesDataSource.FILE_NAME) },
        )

    @Provides
    fun todaySource(): TodaySource = TodaySource.System
}
