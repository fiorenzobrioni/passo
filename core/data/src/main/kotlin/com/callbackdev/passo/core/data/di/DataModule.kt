package com.callbackdev.passo.core.data.di

import android.content.Context
import androidx.room.Room
import com.callbackdev.passo.core.data.db.PassoDatabase
import com.callbackdev.passo.core.data.db.TrackingDao
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
}
