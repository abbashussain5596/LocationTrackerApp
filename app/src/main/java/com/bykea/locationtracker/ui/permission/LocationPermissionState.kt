package com.bykea.locationtracker.ui.permission

import android.Manifest
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.MultiplePermissionsState
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.accompanist.permissions.rememberPermissionState

/**
 * Two-stage permission flow:
 *  1. Foreground (FINE + COARSE + POST_NOTIFICATIONS on API 33+).
 *  2. Background location — must be requested separately *after* foreground is granted
 *     (Android 10+ system requirement).
 */
@OptIn(ExperimentalPermissionsApi::class)
class LocationPermissionState(
    val foreground: MultiplePermissionsState,
    val background: PermissionState?,
) {
    val foregroundGranted: Boolean
        get() = foreground.allPermissionsGranted

    val backgroundGranted: Boolean
        get() = background?.status?.isGranted ?: true

    val allGranted: Boolean
        get() = foregroundGranted && backgroundGranted

    fun requestForeground() = foreground.launchMultiplePermissionRequest()
    fun requestBackground() = background?.launchPermissionRequest()
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun rememberLocationPermissionState(): LocationPermissionState {
    val foregroundPermissions = buildList {
        add(Manifest.permission.ACCESS_FINE_LOCATION)
        add(Manifest.permission.ACCESS_COARSE_LOCATION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    val foreground = rememberMultiplePermissionsState(foregroundPermissions)
    val background = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        rememberPermissionState(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
    } else {
        null
    }

    return remember(foreground, background) {
        LocationPermissionState(foreground, background)
    }
}

@OptIn(ExperimentalPermissionsApi::class)
private val com.google.accompanist.permissions.PermissionStatus.isGranted: Boolean
    get() = this == com.google.accompanist.permissions.PermissionStatus.Granted
