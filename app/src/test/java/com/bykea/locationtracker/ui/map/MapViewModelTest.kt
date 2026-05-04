package com.bykea.locationtracker.ui.map

import app.cash.turbine.test
import com.bykea.locationtracker.data.repository.LocationRepository
import com.bykea.locationtracker.domain.TrackingController
import com.bykea.locationtracker.domain.model.TrackedLocation
import com.bykea.locationtracker.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MapViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(UnconfinedTestDispatcher())

    private val repository: LocationRepository = mockk(relaxed = true)
    private val tracking: TrackingController = mockk(relaxed = true)

    @Test
    fun `uiState combines cached and history`() = runTest {
        val cached = sampleLocation(timestamp = 100L)
        val current = sampleLocation(timestamp = 200L)
        every { repository.cachedLastLocation } returns flowOf(cached)
        every { repository.recentHistory } returns flowOf(listOf(current, cached))

        val viewModel = MapViewModel(repository, tracking)

        viewModel.uiState.test {
            // Replace any intermediate emissions; we only care about the final combined state.
            val state = expectMostRecentItem()
            assertEquals(current, state.current)
            assertEquals(cached, state.cached)
            assertEquals(2, state.historyCount)
            assertFalse(state.isTracking)
        }
    }

    @Test
    fun `displayLocation prefers current over cached`() = runTest {
        val cached = sampleLocation(timestamp = 100L)
        val current = sampleLocation(timestamp = 999L)
        every { repository.cachedLastLocation } returns flowOf(cached)
        every { repository.recentHistory } returns flowOf(listOf(current))

        val viewModel = MapViewModel(repository, tracking)

        viewModel.uiState.test {
            assertEquals(current, expectMostRecentItem().displayLocation)
        }
    }

    @Test
    fun `displayLocation falls back to cached when history empty`() = runTest {
        val cached = sampleLocation()
        every { repository.cachedLastLocation } returns flowOf(cached)
        every { repository.recentHistory } returns flowOf(emptyList())

        val viewModel = MapViewModel(repository, tracking)

        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertNull(state.current)
            assertEquals(cached, state.displayLocation)
        }
    }

    @Test
    fun `startTracking starts service and flips isTracking`() = runTest {
        every { repository.cachedLastLocation } returns flowOf(null)
        every { repository.recentHistory } returns flowOf(emptyList())

        val viewModel = MapViewModel(repository, tracking)
        viewModel.startTracking()

        viewModel.uiState.test {
            assertTrue(expectMostRecentItem().isTracking)
        }
        verify(exactly = 1) { tracking.start() }
    }

    @Test
    fun `stopTracking stops service and clears isTracking`() = runTest {
        every { repository.cachedLastLocation } returns flowOf(null)
        every { repository.recentHistory } returns flowOf(emptyList())

        val viewModel = MapViewModel(repository, tracking)
        viewModel.startTracking()
        viewModel.stopTracking()

        viewModel.uiState.test {
            assertFalse(expectMostRecentItem().isTracking)
        }
        verify(exactly = 1) { tracking.stop() }
    }

    @Test
    fun `primeLastKnown delegates to repository`() = runTest {
        every { repository.cachedLastLocation } returns flowOf(null)
        every { repository.recentHistory } returns flowOf(emptyList())
        coEvery { repository.fetchLastKnown() } returns sampleLocation()

        val viewModel = MapViewModel(repository, tracking)
        viewModel.primeLastKnown()

        coVerify(exactly = 1) { repository.fetchLastKnown() }
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
}
