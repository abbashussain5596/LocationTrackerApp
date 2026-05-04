package com.bykea.locationtracker.data.location

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocationAccuracyFilterTest {

    @Test
    fun `rejects fix without accuracy`() {
        assertFalse(isLocationUsable(hasAccuracy = false, accuracyMeters = 5f, thresholdMeters = 50f))
    }

    @Test
    fun `rejects fix with zero accuracy`() {
        assertFalse(isLocationUsable(hasAccuracy = true, accuracyMeters = 0f, thresholdMeters = 50f))
    }

    @Test
    fun `rejects fix with negative accuracy`() {
        assertFalse(isLocationUsable(hasAccuracy = true, accuracyMeters = -1f, thresholdMeters = 50f))
    }

    @Test
    fun `accepts fix within threshold`() {
        assertTrue(isLocationUsable(hasAccuracy = true, accuracyMeters = 10f, thresholdMeters = 50f))
    }

    @Test
    fun `accepts fix exactly at threshold`() {
        assertTrue(isLocationUsable(hasAccuracy = true, accuracyMeters = 50f, thresholdMeters = 50f))
    }

    @Test
    fun `rejects fix above threshold`() {
        assertFalse(isLocationUsable(hasAccuracy = true, accuracyMeters = 51f, thresholdMeters = 50f))
    }
}
