package dev.burixon.mockgps

import java.util.Locale
import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.location.Criteria
import android.location.Location
import android.location.LocationManager
import android.location.provider.ProviderProperties
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock
import kotlin.math.cos
import kotlin.random.Random

class MockLocationService : Service() {

    companion object {

        const val COMMAND = "command"

        const val COMMAND_ON = "on"
        const val COMMAND_OFF = "off"

        const val EXTRA_TOAST = "toast"

        private const val GPS =
            LocationManager.GPS_PROVIDER

        private const val NETWORK =
            LocationManager.NETWORK_PROVIDER

        private const val NOTIFICATION_ID =
            1001

        private const val CHANNEL_ID =
            "mockgps"

        private const val RESTART_REQUEST_CODE =
            1001

        private const val UPDATE_INTERVAL =
            1000L

        private const val DRIFT_INTERVAL =
            2500L

        private const val MAX_DRIFT_METERS =
            15.0
    }

    private lateinit var locationManager: LocationManager

    private val handler =
        Handler(
            Looper.getMainLooper()
        )

    private var gpsReady = false
    private var networkReady = false

    private var driftLat: Double? = null
    private var driftLon: Double? = null
    private var driftAccuracy = 0f
    private var nextDriftUpdate = 0L

    private var shouldToastOnStart = false

    private val updateRunnable =
        object : Runnable {

            override fun run() {

                if (
                    !State.isDesiredEnabled(
                        this@MockLocationService
                    )
                ) {
                    stopServiceCleanly()
                    return
                }

                publishCurrent()

                /*
                 * Re-post the same notification continuously.
                 * It is the single persistent notification for the service.
                 * If an OEM removes it, the next update recreates it.
                 */

                updateNotification()

                handler.postDelayed(
                    this,
                    UPDATE_INTERVAL
                )
            }
        }

    override fun onCreate() {
        super.onCreate()

        locationManager =
            getSystemService(
                LocationManager::class.java
            )

        State.setServiceRunning(
            this,
            false
        )

        createNotificationChannel()

        startForeground(
            NOTIFICATION_ID,
            createNotification(),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
        )

        createProviders()
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {

        when (
            intent?.getStringExtra(
                COMMAND
            )
        ) {

            COMMAND_ON -> {

                shouldToastOnStart =
                    intent.getBooleanExtra(
                        EXTRA_TOAST,
                        false
                    )

                State.setDesiredEnabled(
                    this,
                    true
                )

                startMocking()

                if (shouldToastOnStart) {
                    android.widget.Toast
                        .makeText(
                            this,
                            "Mocking started.",
                            android.widget.Toast.LENGTH_SHORT
                        )
                        .show()
                }

                shouldToastOnStart =
                    false
            }

            COMMAND_OFF -> {

                State.setDesiredEnabled(
                    this,
                    false
                )

                stopServiceCleanly()

                stopSelfResult(
                    startId
                )

                return START_NOT_STICKY
            }

            null -> {

                if (
                    State.isDesiredEnabled(this)
                ) {
                    startMocking()
                } else {
                    stopServiceCleanly()

                    stopSelfResult(
                        startId
                    )

                    return START_NOT_STICKY
                }
            }
        }

        return START_STICKY
    }

    private fun startMocking() {

        if (
            !gpsReady ||
            !networkReady
        ) {
            createProviders()
        }

        if (
            !gpsReady ||
            !networkReady
        ) {

            State.setServiceRunning(
                this,
                false
            )

            scheduleRestart()

            return
        }

        State.ensureLocation(
            this
        )

        State.setServiceRunning(
            this,
            true
        )

        handler.removeCallbacks(
            updateRunnable
        )

        updateRunnable.run()
    }

    private fun createProviders() {

        createProvider(
            GPS,
            true
        )

        createProvider(
            NETWORK,
            false
        )
    }

    private fun createProvider(
        provider: String,
        isGps: Boolean
    ) {

        try {

            if (
                Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.S
            ) {

                val properties: ProviderProperties =
                    ProviderProperties.Builder()
                        .setAccuracy(
                            if (isGps) {
                                ProviderProperties
                                    .ACCURACY_FINE
                            } else {
                                ProviderProperties
                                    .ACCURACY_COARSE
                            }
                        )
                        .setPowerUsage(
                            ProviderProperties
                                .POWER_USAGE_LOW
                        )
                        .setHasSpeedSupport(true)
                        .setHasBearingSupport(true)
                        .setHasAltitudeSupport(true)
                        .setHasSatelliteRequirement(false)
                        .build()

                @Suppress("DEPRECATION")
                try {
                    locationManager.removeTestProvider(
                        provider
                    )
                } catch (_: Exception) {
                }

                locationManager.addTestProvider(
                    provider,
                    properties
                )

            } else {

                @Suppress("DEPRECATION")
                try {
                    locationManager.removeTestProvider(
                        provider
                    )
                } catch (_: Exception) {
                }

                @Suppress("DEPRECATION")
                locationManager.addTestProvider(
                    provider,
                    false,
                    false,
                    false,
                    false,
                    true,
                    true,
                    true,
                    Criteria.POWER_LOW,
                    if (isGps) {
                        Criteria.ACCURACY_FINE
                    } else {
                        Criteria.ACCURACY_COARSE
                    }
                )
            }

            @Suppress("DEPRECATION")
            locationManager.setTestProviderEnabled(
                provider,
                true
            )

            if (isGps) {
                gpsReady = true
            } else {
                networkReady = true
            }

        } catch (_: Exception) {

            if (isGps) {
                gpsReady = false
            } else {
                networkReady = false
            }
        }
    }

    private fun publishCurrent() {

        val baseLat =
            State.getLat(this)
                ?: return

        val baseLon =
            State.getLon(this)
                ?: return

        val drifting =
            State.isDrifting(this)

        val now =
            System.currentTimeMillis()

        var lat = baseLat
        var lon = baseLon
        var accuracy = 0f

        if (drifting) {

            if (
                now >= nextDriftUpdate ||
                driftLat == null ||
                driftLon == null
            ) {

                val angle =
                    Random.nextDouble(
                        0.0,
                        Math.PI * 2.0
                    )

                val distance =
                    Random.nextDouble(
                        2.0,
                        MAX_DRIFT_METERS
                    )

                val metersPerDegreeLat =
                    111_320.0

                val metersPerDegreeLon =
                    111_320.0 *
                            cos(
                                Math.toRadians(
                                    baseLat
                                )
                            )

                val offsetLat =
                    cos(angle) *
                            distance /
                            metersPerDegreeLat

                val offsetLon =
                    kotlin.math.sin(angle) *
                            distance /
                            metersPerDegreeLon

                driftLat =
                    baseLat +
                            offsetLat

                driftLon =
                    baseLon +
                            offsetLon

                driftAccuracy =
                    Random.nextFloat() *
                            50.0f

                nextDriftUpdate =
                    now +
                            DRIFT_INTERVAL
            }

            lat =
                driftLat
                    ?: baseLat

            lon =
                driftLon
                    ?: baseLon

            accuracy =
                driftAccuracy

        } else {

            /*
             * Drift disabled:
             * exact selected coordinates and 0 accuracy.
             */

            driftLat = null
            driftLon = null
            driftAccuracy = 0f
            nextDriftUpdate = 0L

            lat = baseLat
            lon = baseLon
            accuracy = 0f
        }

        publish(
            GPS,
            lat,
            lon,
            accuracy,
            now
        )

        publish(
            NETWORK,
            lat,
            lon,
            accuracy,
            now
        )
    }

    private fun publish(
        provider: String,
        lat: Double,
        lon: Double,
        accuracy: Float,
        time: Long
    ) {

        try {

            val location =
                Location(provider).apply {

                    latitude = lat
                    longitude = lon

                    this.accuracy =
                        accuracy

                    altitude = 0.0
                    speed = 0f
                    bearing = 0f

                    this.time =
                        time

                    elapsedRealtimeNanos =
                        SystemClock
                            .elapsedRealtimeNanos()
                }

            @Suppress("DEPRECATION")
            locationManager.setTestProviderLocation(
                provider,
                location
            )

        } catch (_: Exception) {
        }
    }

    private fun createNotificationChannel() {

        val manager =
            getSystemService(
                NotificationManager::class.java
            )

        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "MockGPS",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description =
                    "Persistent MockGPS service"
            }
        )
    }

    private fun createNotification(): Notification {

        val launchIntent =
            packageManager.getLaunchIntentForPackage(
                packageName
            )

        val pendingIntent =
            launchIntent?.let {
                PendingIntent.getActivity(
                    this,
                    0,
                    it,
                    PendingIntent.FLAG_UPDATE_CURRENT or
                            PendingIntent.FLAG_IMMUTABLE
                )
            }

        val lat =
            State.getLat(this)

        val lon =
            State.getLon(this)

        val coordinateText =
            if (
                lat != null &&
                lon != null
            ) {
                String.format(
                    Locale.US,
                    "%.6f, %.6f",
                    lat,
                    lon
                )
            } else {
                "No location selected"
            }

        val builder =
            Notification.Builder(
                this,
                CHANNEL_ID
            )
                .setSmallIcon(
                    R.mipmap.ic_launcher
                )
                .setContentTitle(
                    "Running..."
                )
                .setContentText(
                    coordinateText
                )
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setShowWhen(false)
                .setPriority(
                    Notification.PRIORITY_MIN
                )

        if (pendingIntent != null) {
            builder.setContentIntent(
                pendingIntent
            )
        }

        return builder.build()
    }

    private fun updateNotification() {

        val manager =
            getSystemService(
                NotificationManager::class.java
            )

        manager.notify(
            NOTIFICATION_ID,
            createNotification()
        )
    }

    private fun scheduleRestart() {

        if (
            !State.isDesiredEnabled(this)
        ) {
            return
        }

        val alarmManager =
            getSystemService(
                AlarmManager::class.java
            )

        val intent =
            Intent(
                this,
                BootReceiver::class.java
            ).apply {
                action =
                    BootReceiver
                        .ACTION_RESTART_SERVICE
            }

        val pendingIntent =
            PendingIntent.getBroadcast(
                this,
                RESTART_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or
                        PendingIntent.FLAG_IMMUTABLE
            )

        alarmManager.set(
            AlarmManager.RTC_WAKEUP,
            System.currentTimeMillis() + 1000L,
            pendingIntent
        )
    }

    private fun cancelRestart() {

        val alarmManager =
            getSystemService(
                AlarmManager::class.java
            )

        val intent =
            Intent(
                this,
                BootReceiver::class.java
            ).apply {
                action =
                    BootReceiver
                        .ACTION_RESTART_SERVICE
            }

        val pendingIntent =
            PendingIntent.getBroadcast(
                this,
                RESTART_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or
                        PendingIntent.FLAG_IMMUTABLE
            )

        alarmManager.cancel(
            pendingIntent
        )
    }

    private fun stopServiceCleanly() {

        handler.removeCallbacks(
            updateRunnable
        )

        cancelRestart()

        try {
            @Suppress("DEPRECATION")
            locationManager.setTestProviderEnabled(
                GPS,
                false
            )
        } catch (_: Exception) {
        }

        try {
            @Suppress("DEPRECATION")
            locationManager.setTestProviderEnabled(
                NETWORK,
                false
            )
        } catch (_: Exception) {
        }

        try {
            @Suppress("DEPRECATION")
            locationManager.removeTestProvider(
                GPS
            )
        } catch (_: Exception) {
        }

        try {
            @Suppress("DEPRECATION")
            locationManager.removeTestProvider(
                NETWORK
            )
        } catch (_: Exception) {
        }

        gpsReady = false
        networkReady = false

        driftLat = null
        driftLon = null
        driftAccuracy = 0f
        nextDriftUpdate = 0L

        State.setServiceRunning(
            this,
            false
        )

        stopForeground(
            STOP_FOREGROUND_REMOVE
        )
    }

    override fun onTaskRemoved(
        rootIntent: Intent?
    ) {

        if (
            State.isDesiredEnabled(this)
        ) {
            scheduleRestart()
        }

        super.onTaskRemoved(
            rootIntent
        )
    }

    override fun onDestroy() {

        handler.removeCallbacks(
            updateRunnable
        )

        /*
         * Do NOT cancel a scheduled restart here.
         * onDestroy may happen because the process/service was killed.
         * The alarm must remain alive while desired_enabled=true.
         */

        if (
            !State.isDesiredEnabled(this)
        ) {
            cancelRestart()
        }

        try {
            @Suppress("DEPRECATION")
            locationManager.removeTestProvider(
                GPS
            )
        } catch (_: Exception) {
        }

        try {
            @Suppress("DEPRECATION")
            locationManager.removeTestProvider(
                NETWORK
            )
        } catch (_: Exception) {
        }

        State.setServiceRunning(
            this,
            false
        )

        super.onDestroy()
    }

    override fun onBind(
        intent: Intent?
    ): IBinder? = null
}