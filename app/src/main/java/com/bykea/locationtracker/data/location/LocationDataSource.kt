package com.bykea.locationtracker.data.location

import com.bykea.locationtracker.domain.model.TrackedLocation
import com.bykea.locationtracker.domain.model.TrackingConfig
import kotlinx.coroutines.flow.Flow

interface LocationDataSource {
    fun locationUpdates(config: TrackingConfig): Flow<TrackedLocation>
    suspend fun lastKnown(): TrackedLocation?
}
