package com.bykea.locationtracker.domain.model

data class TrackedLocation(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float,
    val provider: String,
    val timestampMillis: Long,
    val speedMps: Float? = null,
    val bearingDegrees: Float? = null,
)
