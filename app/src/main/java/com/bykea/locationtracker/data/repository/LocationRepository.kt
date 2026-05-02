package com.bykea.locationtracker.data.repository

import com.bykea.locationtracker.data.local.cache.LastLocationCache
import com.bykea.locationtracker.data.local.db.LocationDao
import com.bykea.locationtracker.data.local.db.LocationEntity
import com.bykea.locationtracker.data.location.LocationDataSource
import com.bykea.locationtracker.di.IoDispatcher
import com.bykea.locationtracker.domain.model.TrackedLocation
import com.bykea.locationtracker.domain.model.TrackingConfig
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext

/**
 * Single source of truth for location data.
 *
 * - Streams live updates from [LocationDataSource], persists each one to Room +
 *   DataStore on the IO dispatcher so the main thread is never touched here.
 * - Exposes the last-known cached location and recent history as flows for the UI.
 */
@Singleton
class LocationRepository @Inject constructor(
    private val source: LocationDataSource,
    private val dao: LocationDao,
    private val cache: LastLocationCache,
    @IoDispatcher private val io: CoroutineDispatcher,
) {

    val cachedLastLocation: Flow<TrackedLocation?> = cache.lastLocation

    val recentHistory: Flow<List<TrackedLocation>> =
        dao.observeRecent().map { list -> list.map { it.toDomain() } }

    /**
     * Live location stream. Each emission is persisted off the main thread.
     * Collectors get the same [TrackedLocation] after persistence completes.
     */
    fun trackLocation(config: TrackingConfig): Flow<TrackedLocation> =
        source.locationUpdates(config)
            .onEach { persist(it) }
            .flowOn(io)

    suspend fun fetchLastKnown(): TrackedLocation? = withContext(io) {
        source.lastKnown()
    }

    private suspend fun persist(location: TrackedLocation) {
        dao.insert(LocationEntity.fromDomain(location))
        cache.save(location)
    }
}
