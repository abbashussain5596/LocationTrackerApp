package com.bykea.locationtracker.di

import android.content.Context
import androidx.room.Room
import com.bykea.locationtracker.data.local.cache.LastLocationCache
import com.bykea.locationtracker.data.local.db.AppDatabase
import com.bykea.locationtracker.data.local.db.LocationDao
import com.bykea.locationtracker.data.location.FusedLocationDataSource
import com.bykea.locationtracker.data.location.LocationDataSource
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
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
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.NAME).build()

    @Provides
    fun provideLocationDao(db: AppDatabase): LocationDao = db.locationDao()

    @Provides
    @Singleton
    fun provideLastLocationCache(@ApplicationContext context: Context): LastLocationCache =
        LastLocationCache(context)

    @Provides
    @Singleton
    fun provideFusedLocationClient(
        @ApplicationContext context: Context,
    ): FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)

    @Provides
    @Singleton
    fun provideLocationDataSource(
        @ApplicationContext context: Context,
        client: FusedLocationProviderClient,
    ): LocationDataSource = FusedLocationDataSource(context, client)
}
