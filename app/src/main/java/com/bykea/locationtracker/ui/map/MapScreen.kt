package com.bykea.locationtracker.ui.map

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bykea.locationtracker.domain.model.TrackedLocation
import com.bykea.locationtracker.ui.permission.PermissionGate
import com.bykea.locationtracker.ui.permission.rememberLocationPermissionState
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    viewModel: MapViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val permission = rememberLocationPermissionState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("GPS Tracker") }) },
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            PermissionGate(
                permission = permission,
                onAllGranted = { viewModel.primeLastKnown() },
            ) {
                MapContent(
                    state = state,
                    onStart = viewModel::startTracking,
                    onStop = viewModel::stopTracking,
                )
            }
        }
    }
}

@Composable
private fun MapContent(
    state: MapUiState,
    onStart: () -> Unit,
    onStop: () -> Unit,
) {
    val cameraPositionState: CameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(0.0, 0.0), 2f)
    }

    // Recenter only on meaningful changes; the displayLocation already filters cache-vs-live.
    LaunchedEffect(state.displayLocation?.latitude, state.displayLocation?.longitude) {
        state.displayLocation?.let {
            cameraPositionState.position =
                CameraPosition.fromLatLngZoom(LatLng(it.latitude, it.longitude), 16f)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(isMyLocationEnabled = false),
                uiSettings = MapUiSettings(
                    zoomControlsEnabled = false,
                    myLocationButtonEnabled = false,
                ),
            ) {
                state.displayLocation?.let { loc ->
                    Marker(
                        state = MarkerState(LatLng(loc.latitude, loc.longitude)),
                        title = "Current",
                        snippet = "±%.0fm · %s".format(loc.accuracyMeters, loc.provider),
                    )
                }
            }

            if (state.current == null && state.cached != null) {
                CachedBanner(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(12.dp),
                )
            }
        }

        StatusCard(state = state, onStart = onStart, onStop = onStop)
    }
}

@Composable
private fun CachedBanner(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
    ) {
        Text(
            text = "Showing cached location · waiting for live fix",
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

@Composable
private fun StatusCard(
    state: MapUiState,
    onStart: () -> Unit,
    onStop: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(12.dp),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            val loc: TrackedLocation? = state.displayLocation
            if (loc == null) {
                Text("No location yet", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Tap Start to begin tracking.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            } else {
                Text(
                    text = "%.6f, %.6f".format(loc.latitude, loc.longitude),
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Accuracy: ±%.0fm · Provider: %s".format(
                        loc.accuracyMeters, loc.provider,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = "Last update: ${DateFormat.getTimeInstance().format(Date(loc.timestampMillis))}",
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    text = "History: ${state.historyCount} points",
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            Spacer(Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (state.isTracking) {
                    OutlinedButton(onClick = onStop, modifier = Modifier.weight(1f)) {
                        Text("Stop")
                    }
                } else {
                    Button(onClick = onStart, modifier = Modifier.weight(1f)) {
                        Text("Start tracking")
                    }
                }
            }
        }
    }
}
