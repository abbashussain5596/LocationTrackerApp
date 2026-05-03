package com.bykea.locationtracker.ui.map

import com.bykea.locationtracker.domain.model.TrackedLocation

data class MapUiState(
    val current: TrackedLocation? = null,
    val cached: TrackedLocation? = null,
    val historyCount: Int = 0,
    val isTracking: Boolean = false,
) {
    val displayLocation: TrackedLocation? get() = current ?: cached
}
