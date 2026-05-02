package com.bykea.locationtracker.domain.model

data class TrackingConfig(
    val intervalMillis: Long,
    val minUpdateIntervalMillis: Long,
    val minUpdateDistanceMeters: Float,
    val accuracyThresholdMeters: Float,
    val highAccuracy: Boolean,
) {
    companion object {
        // Normal operation: high-accuracy GPS, ~5s cadence, 5m distance filter.
        val Default = TrackingConfig(
            intervalMillis = 5_000L,
            minUpdateIntervalMillis = 2_000L,
            minUpdateDistanceMeters = 5f,
            accuracyThresholdMeters = 50f,
            highAccuracy = true,
        )

        // Low-power mode: balanced provider, slower cadence, larger distance filter.
        val LowPower = TrackingConfig(
            intervalMillis = 30_000L,
            minUpdateIntervalMillis = 15_000L,
            minUpdateDistanceMeters = 25f,
            accuracyThresholdMeters = 100f,
            highAccuracy = false,
        )
    }
}
