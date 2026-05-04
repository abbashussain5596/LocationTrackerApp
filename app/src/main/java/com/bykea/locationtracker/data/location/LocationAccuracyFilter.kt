package com.bykea.locationtracker.data.location

/**
 * Pure function so it's testable without [android.location.Location].
 * A fix is usable when accuracy is reported, positive, and within the threshold.
 */
internal fun isLocationUsable(
    hasAccuracy: Boolean,
    accuracyMeters: Float,
    thresholdMeters: Float,
): Boolean {
    if (!hasAccuracy) return false
    if (accuracyMeters <= 0f) return false
    return accuracyMeters <= thresholdMeters
}
