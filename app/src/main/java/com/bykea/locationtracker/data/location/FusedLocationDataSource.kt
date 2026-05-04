package com.bykea.locationtracker.data.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.content.ContextCompat
import com.bykea.locationtracker.domain.model.TrackedLocation
import com.bykea.locationtracker.domain.model.TrackingConfig
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.Priority
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Wraps [FusedLocationProviderClient] as a cold [Flow]. The fused provider already
 * handles GPS/network/wifi fusion and falls back gracefully under weak signal —
 * we just choose the right [Priority] for the requested mode.
 */
class FusedLocationDataSource(
    private val context: Context,
    private val client: FusedLocationProviderClient,
) : LocationDataSource {

    @SuppressLint("MissingPermission")
    override fun locationUpdates(config: TrackingConfig): Flow<TrackedLocation> = callbackFlow {
        if (!hasLocationPermission()) {
            close(SecurityException("Location permission not granted"))
            return@callbackFlow
        }

        val priority = if (config.highAccuracy) {
            Priority.PRIORITY_HIGH_ACCURACY
        } else {
            Priority.PRIORITY_BALANCED_POWER_ACCURACY
        }

        val request = LocationRequest.Builder(priority, config.intervalMillis)
            .setMinUpdateIntervalMillis(config.minUpdateIntervalMillis)
            .setMinUpdateDistanceMeters(config.minUpdateDistanceMeters)
            .setWaitForAccurateLocation(config.highAccuracy)
            .build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.locations.forEach { loc ->
                    if (loc.isUsable(config.accuracyThresholdMeters)) {
                        trySend(loc.toTracked())
                    }
                }
            }
        }

        client.requestLocationUpdates(request, callback, context.mainLooper)

        awaitClose { client.removeLocationUpdates(callback) }
    }

    @SuppressLint("MissingPermission")
    override suspend fun lastKnown(): TrackedLocation? {
        if (!hasLocationPermission()) return null
        return runCatching { client.lastLocation.await()?.toTracked() }.getOrNull()
    }

    private fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    private fun Location.isUsable(thresholdMeters: Float): Boolean =
        isLocationUsable(hasAccuracy(), accuracy, thresholdMeters)

    private fun Location.toTracked() = TrackedLocation(
        latitude = latitude,
        longitude = longitude,
        accuracyMeters = if (hasAccuracy()) accuracy else Float.MAX_VALUE,
        provider = provider ?: "fused",
        timestampMillis = time,
        speedMps = if (hasSpeed()) speed else null,
        bearingDegrees = if (hasBearing()) bearing else null,
    )
}
