package com.bykea.locationtracker.di

import com.bykea.locationtracker.domain.TrackingController
import com.bykea.locationtracker.service.ServiceTrackingController
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class TrackingModule {

    @Binds
    @Singleton
    abstract fun bindTrackingController(
        impl: ServiceTrackingController,
    ): TrackingController
}
