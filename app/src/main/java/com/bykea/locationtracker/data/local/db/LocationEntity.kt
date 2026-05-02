package com.bykea.locationtracker.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.bykea.locationtracker.domain.model.TrackedLocation

@Entity(tableName = "location_history")
data class LocationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float,
    val provider: String,
    val timestampMillis: Long,
    val speedMps: Float?,
    val bearingDegrees: Float?,
) {
    fun toDomain() = TrackedLocation(
        latitude = latitude,
        longitude = longitude,
        accuracyMeters = accuracyMeters,
        provider = provider,
        timestampMillis = timestampMillis,
        speedMps = speedMps,
        bearingDegrees = bearingDegrees,
    )

    companion object {
        fun fromDomain(loc: TrackedLocation) = LocationEntity(
            latitude = loc.latitude,
            longitude = loc.longitude,
            accuracyMeters = loc.accuracyMeters,
            provider = loc.provider,
            timestampMillis = loc.timestampMillis,
            speedMps = loc.speedMps,
            bearingDegrees = loc.bearingDegrees,
        )
    }
}
