package com.bykea.locationtracker.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.bykea.locationtracker.R
import com.bykea.locationtracker.data.repository.LocationRepository
import com.bykea.locationtracker.domain.model.TrackedLocation
import com.bykea.locationtracker.domain.model.TrackingConfig
import com.bykea.locationtracker.ui.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch

/**
 * Foreground service that drives the location stream while the app is backgrounded
 * or even killed (system restarts via [START_STICKY]).
 *
 * Modern requirements honored:
 * - `foregroundServiceType="location"` declared in manifest.
 * - On Android 14+, [ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION] is passed at start.
 * - Battery / power-save state observed; tracking config drops to low-power dynamically.
 */
@AndroidEntryPoint
class LocationTrackingService : LifecycleService() {

    @Inject lateinit var repository: LocationRepository

    private val configFlow = MutableStateFlow(TrackingConfig.Default)
    private var trackingJob: Job? = null

    private val powerSaveReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            updateConfigForPowerState()
        }
    }
    private val batteryLowReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_BATTERY_LOW -> configFlow.value = TrackingConfig.LowPower
                Intent.ACTION_BATTERY_OKAY -> updateConfigForPowerState()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createChannel()
        ContextCompat.registerReceiver(
            this,
            powerSaveReceiver,
            IntentFilter(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        ContextCompat.registerReceiver(
            this,
            batteryLowReceiver,
            IntentFilter().apply {
                addAction(Intent.ACTION_BATTERY_LOW)
                addAction(Intent.ACTION_BATTERY_OKAY)
            },
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        updateConfigForPowerState()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        when (intent?.action) {
            ACTION_STOP -> {
                stopSelf()
                return START_NOT_STICKY
            }
        }

        startForegroundWithNotification(latest = null)
        startTrackingIfNeeded()
        return START_STICKY
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private fun startTrackingIfNeeded() {
        if (trackingJob?.isActive == true) return
        trackingJob = lifecycleScope.launch {
            configFlow
                .flatMapLatest { repository.trackLocation(it) }
                .collectLatest { loc ->
                    // Persistence already happened in repository on IO. Just refresh notification.
                    startForegroundWithNotification(loc)
                }
        }
    }

    private fun updateConfigForPowerState() {
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        val bm = getSystemService(BATTERY_SERVICE) as BatteryManager
        val level = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        val lowPower = pm.isPowerSaveMode || (level in 0..15)
        configFlow.value = if (lowPower) TrackingConfig.LowPower else TrackingConfig.Default
    }

    private fun startForegroundWithNotification(latest: TrackedLocation?) {
        val contentText = latest?.let {
            "%.5f, %.5f  ±%.0fm".format(it.latitude, it.longitude, it.accuracyMeters)
        } ?: getString(R.string.notification_acquiring)

        val tapIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val stopIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, LocationTrackingService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(tapIntent)
            .addAction(0, getString(R.string.notification_stop), stopIntent)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION,
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun createChannel() {
        val mgr = getSystemService(NotificationManager::class.java)
        if (mgr.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = getString(R.string.notification_channel_desc)
            setShowBadge(false)
        }
        mgr.createNotificationChannel(channel)
    }

    override fun onDestroy() {
        runCatching { unregisterReceiver(powerSaveReceiver) }
        runCatching { unregisterReceiver(batteryLowReceiver) }
        trackingJob?.cancel()
        super.onDestroy()
    }

    companion object {
        const val CHANNEL_ID = "location_tracking"
        const val NOTIFICATION_ID = 4242
        const val ACTION_STOP = "com.bykea.locationtracker.action.STOP"

        fun start(context: Context) {
            val intent = Intent(context, LocationTrackingService::class.java)
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, LocationTrackingService::class.java)
                .setAction(ACTION_STOP)
            context.startService(intent)
        }
    }
}
