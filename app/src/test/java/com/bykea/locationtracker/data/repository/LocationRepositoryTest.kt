package com.bykea.locationtracker.data.repository

import app.cash.turbine.test
import com.bykea.locationtracker.data.local.cache.LastLocationCache
import com.bykea.locationtracker.data.local.db.LocationDao
import com.bykea.locationtracker.data.local.db.LocationEntity
import com.bykea.locationtracker.data.location.LocationDataSource
import com.bykea.locationtracker.domain.model.TrackedLocation
import com.bykea.locationtracker.domain.model.TrackingConfig
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LocationRepositoryTest {

    private val source: LocationDataSource = mockk()
    private val dao: LocationDao = mockk(relaxed = true)
    private val cache: LastLocationCache = mockk(relaxed = true)
    private val ioDispatcher = UnconfinedTestDispatcher()

    private val repository = LocationRepository(source, dao, cache, ioDispatcher)

    @Test
    fun `trackLocation persists each emission then re-emits it`() = runTest {
        val first = sampleLocation(timestamp = 1L)
        val second = sampleLocation(timestamp = 2L)
        every { source.locationUpdates(any()) } returns flowOf(first, second)
        coEvery { dao.insert(any()) } returns 1L

        repository.trackLocation(TrackingConfig.Default).test {
            assertEquals(first, awaitItem())
            assertEquals(second, awaitItem())
            awaitComplete()
        }

        coVerifyOrder {
            dao.insert(LocationEntity.fromDomain(first))
            cache.save(first)
            dao.insert(LocationEntity.fromDomain(second))
            cache.save(second)
        }
    }

    @Test
    fun `cachedLastLocation forwards cache flow`() = runTest {
        val cached = sampleLocation()
        every { cache.lastLocation } returns flowOf(cached)

        repository.cachedLastLocation.test {
            assertEquals(cached, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `recentHistory maps DAO entities to domain`() = runTest {
        val entities = listOf(
            sampleEntity(timestamp = 200L),
            sampleEntity(timestamp = 100L),
        )
        every { dao.observeRecent(any()) } returns flowOf(entities)

        repository.recentHistory.test {
            val mapped = awaitItem()
            assertEquals(entities.map { it.toDomain() }, mapped)
            awaitComplete()
        }
    }

    @Test
    fun `fetchLastKnown delegates to source`() = runTest {
        val expected = sampleLocation()
        coEvery { source.lastKnown() } returns expected

        val actual = repository.fetchLastKnown()

        assertEquals(expected, actual)
        coVerify(exactly = 1) { source.lastKnown() }
    }

    private fun sampleLocation(timestamp: Long = 0L) = TrackedLocation(
        latitude = 24.86,
        longitude = 67.00,
        accuracyMeters = 5f,
        provider = "fused",
        timestampMillis = timestamp,
        speedMps = null,
        bearingDegrees = null,
    )

    private fun sampleEntity(timestamp: Long = 0L) = LocationEntity(
        id = 0,
        latitude = 24.86,
        longitude = 67.00,
        accuracyMeters = 5f,
        provider = "fused",
        timestampMillis = timestamp,
        speedMps = null,
        bearingDegrees = null,
    )
}
