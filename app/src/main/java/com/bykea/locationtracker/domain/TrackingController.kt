package com.bykea.locationtracker.domain

/**
 * Abstraction over starting/stopping the foreground tracking service.
 * Lets the ViewModel be tested without touching Android's service machinery.
 */
interface TrackingController {
    fun start()
    fun stop()
}
