package com.bykea.locationtracker.ui.permission

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi

/**
 * Wraps the main UI behind the permission flow.
 *
 * - If foreground location is missing, request it (re-prompts as long as the user
 *   stays here — covers the "continuously prompts if denied" requirement).
 * - Once granted, asks for background location separately (Android 10+ rule).
 * - If the user permanently denies, falls back to a "Open settings" CTA.
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PermissionGate(
    permission: LocationPermissionState,
    onAllGranted: () -> Unit = {},
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current

    LaunchedEffect(permission.allGranted) {
        if (permission.allGranted) onAllGranted()
    }

    when {
        permission.allGranted -> content()
        !permission.foregroundGranted -> PermissionRationale(
            title = "Location permission needed",
            body = "We use your location to track your route. Updates run in the background while the app is minimized.",
            primaryLabel = if (permission.foreground.shouldShowRationale) "Grant" else "Allow location",
            onPrimary = permission::requestForeground,
            secondaryLabel = "App settings".takeIf { !permission.foreground.shouldShowRationale },
            onSecondary = {
                context.startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    },
                )
            },
        )
        !permission.backgroundGranted -> PermissionRationale(
            title = "Allow background location",
            body = "To keep tracking when the app is minimized, allow location access \"All the time\" on the next screen.",
            primaryLabel = "Allow in background",
            onPrimary = permission::requestBackground,
            secondaryLabel = "Continue without",
            onSecondary = onAllGranted,
        )
    }
}

@Composable
private fun PermissionRationale(
    title: String,
    body: String,
    primaryLabel: String,
    onPrimary: () -> Unit,
    secondaryLabel: String? = null,
    onSecondary: (() -> Unit)? = null,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(title, style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(12.dp))
        Text(body, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(20.dp))
        Button(onClick = onPrimary) { Text(primaryLabel) }
        if (secondaryLabel != null && onSecondary != null) {
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = onSecondary) { Text(secondaryLabel) }
        }
    }
}
