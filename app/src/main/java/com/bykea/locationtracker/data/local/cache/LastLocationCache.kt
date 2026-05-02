package com.bykea.locationtracker.data.local.cache

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.bykea.locationtracker.domain.model.TrackedLocation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.lastLocationDataStore by preferencesDataStore(name = "last_location")

class LastLocationCache(private val context: Context) {

    private object Keys {
        val LAT = doublePreferencesKey("lat")
        val LON = doublePreferencesKey("lon")
        val ACC = floatPreferencesKey("accuracy")
        val PROVIDER = stringPreferencesKey("provider")
        val TIMESTAMP = longPreferencesKey("timestamp")
        val SPEED = floatPreferencesKey("speed")
        val BEARING = floatPreferencesKey("bearing")
    }

    val lastLocation: Flow<TrackedLocation?> =
        context.lastLocationDataStore.data.map { it.toLocation() }

    suspend fun save(location: TrackedLocation) {
        context.lastLocationDataStore.edit { prefs ->
            prefs[Keys.LAT] = location.latitude
            prefs[Keys.LON] = location.longitude
            prefs[Keys.ACC] = location.accuracyMeters
            prefs[Keys.PROVIDER] = location.provider
            prefs[Keys.TIMESTAMP] = location.timestampMillis
            location.speedMps?.let { prefs[Keys.SPEED] = it }
            location.bearingDegrees?.let { prefs[Keys.BEARING] = it }
        }
    }

    private fun Preferences.toLocation(): TrackedLocation? {
        val lat = this[Keys.LAT] ?: return null
        val lon = this[Keys.LON] ?: return null
        return TrackedLocation(
            latitude = lat,
            longitude = lon,
            accuracyMeters = this[Keys.ACC] ?: Float.MAX_VALUE,
            provider = this[Keys.PROVIDER] ?: "cache",
            timestampMillis = this[Keys.TIMESTAMP] ?: 0L,
            speedMps = this[Keys.SPEED],
            bearingDegrees = this[Keys.BEARING],
        )
    }
}
