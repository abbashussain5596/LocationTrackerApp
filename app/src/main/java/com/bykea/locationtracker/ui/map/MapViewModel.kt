package com.bykea.locationtracker.ui.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bykea.locationtracker.data.repository.LocationRepository
import com.bykea.locationtracker.domain.TrackingController
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class MapViewModel @Inject constructor(
    private val repository: LocationRepository,
    private val tracking: TrackingController,
) : ViewModel() {

    private val isTracking = MutableStateFlow(false)

    val uiState: StateFlow<MapUiState> = combine(
        repository.cachedLastLocation,
        repository.recentHistory,
        isTracking,
    ) { cached, history, tracking ->
        MapUiState(
            current = history.firstOrNull(),
            cached = cached,
            historyCount = history.size,
            isTracking = tracking,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = MapUiState(),
    )

    fun startTracking() {
        tracking.start()
        isTracking.value = true
    }

    fun stopTracking() {
        tracking.stop()
        isTracking.value = false
    }

    /** Show *something* before the service streams its first reading. */
    fun primeLastKnown() {
        viewModelScope.launch { repository.fetchLastKnown() }
    }
}
