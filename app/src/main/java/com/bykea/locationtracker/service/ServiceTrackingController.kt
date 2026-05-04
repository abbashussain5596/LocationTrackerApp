package com.bykea.locationtracker.service

import android.content.Context
import com.bykea.locationtracker.domain.TrackingController
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ServiceTrackingController @Inject constructor(
    @ApplicationContext private val context: Context,
) : TrackingController {
    override fun start() = LocationTrackingService.start(context)
    override fun stop() = LocationTrackingService.stop(context)
}
