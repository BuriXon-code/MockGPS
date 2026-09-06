package dev.burixon.mockgps

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.location.Location
import android.location.LocationManager
import android.location.provider.ProviderProperties
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock

class MockLocationService : Service() {

    companion object {
        private const val CHANNEL_ID = "mockgps"
        private const val ALERT_CHANNEL_ID = "mockgps_alerts"

        private const val NOTIFICATION_ID = 1
        private const val ALERT_NOTIFICATION_ID = 2

        private const val COMMAND = "command"
        private const val COMMAND_ON = "on"

        private const val EXTRA_LAT = "lat"
        private const val EXTRA_LON = "lon"

        private const val UPDATE_INTERVAL = 1000L

        private const val GPS =
            LocationManager.GPS_PROVIDER

        private const val NETWORK =
            LocationManager.NETWORK_PROVIDER
    }

    private lateinit var locationManager: LocationManager

    private val handler =
        Handler(Looper.getMainLooper())

    private var gpsReady = false
    private var networkReady = false

    private var notificationLat: Double? = null
    private var notificationLon: Double? = null

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

        createNotificationChannels()

        try {

            startForeground(
                NOTIFICATION_ID,
                createStartingNotification(),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            )

            if (!createProviders()) {
                stopServiceCleanly()
                return
            }

        } catch (_: Exception) {
            stopServiceCleanly()
            return
        }

        if (
            State.isDesiredEnabled(this)
        ) {
            State.ensureLocation(this)

            handler.removeCallbacks(
                updateRunnable
            )

            publishCurrent()

            handler.post(
                updateRunnable
            )
        }
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {

        val command =
            intent?.getStringExtra(COMMAND)

        if (command == COMMAND_ON) {

            val lat =
                intent
                    .getStringExtra(EXTRA_LAT)
                    ?.toDoubleOrNull()

            val lon =
                intent
                    .getStringExtra(EXTRA_LON)
                    ?.toDoubleOrNull()

            if (
                lat != null &&
                lon != null &&
                State.validLat(lat) &&
                State.validLon(lon)
            ) {
                State.setLocation(
                    this,
                    lat,
                    lon
                )
            }

            State.ensureLocation(this)

            State.setDesiredEnabled(
                this,
                true
            )

            startMocking()
        }

        else if (intent == null) {

            if (
                State.isDesiredEnabled(this)
            ) {
                State.ensureLocation(this)
                startMocking()
            }
        }

        return START_STICKY
    }

    private fun startMocking() {

        if (
            !gpsReady &&
            !networkReady
        ) {
            if (!createProviders()) {

                State.setDesiredEnabled(
                    this,
                    false
                )

                stopServiceCleanly()
                return
            }
        }

        handler.removeCallbacks(
            updateRunnable
        )

        publishCurrent()

        handler.post(
            updateRunnable
        )
    }

    private fun createProviders(): Boolean {

        if (!gpsReady) {
            try {
                createGpsProvider()
            } catch (_: Exception) {
                gpsReady = false
            }
        }

        if (!networkReady) {
            try {
                createNetworkProvider()
            } catch (_: Exception) {
                networkReady = false
            }
        }

        return gpsReady || networkReady
    }

    private fun createGpsProvider() {

        val properties =
            ProviderProperties.Builder()
                .setAccuracy(
                    ProviderProperties.ACCURACY_FINE
                )
                .setPowerUsage(
                    ProviderProperties.POWER_USAGE_LOW
                )
                .setHasSpeedSupport(true)
                .setHasBearingSupport(true)
                .setHasAltitudeSupport(true)
                .setHasSatelliteRequirement(false)
                .build()

        try {
            locationManager.removeTestProvider(
                GPS
            )
        } catch (_: Exception) {
        }

        locationManager.addTestProvider(
            GPS,
            properties
        )

        locationManager.setTestProviderEnabled(
            GPS,
            true
        )

        gpsReady = true
    }

    private fun createNetworkProvider() {

        val properties =
            ProviderProperties.Builder()
                .setAccuracy(
                    ProviderProperties.ACCURACY_COARSE
                )
                .setPowerUsage(
                    ProviderProperties.POWER_USAGE_LOW
                )
                .setHasSpeedSupport(true)
                .setHasBearingSupport(true)
                .setHasAltitudeSupport(true)
                .setHasSatelliteRequirement(false)
                .build()

        try {
            locationManager.removeTestProvider(
                NETWORK
            )
        } catch (_: Exception) {
        }

        locationManager.addTestProvider(
            NETWORK,
            properties
        )

        locationManager.setTestProviderEnabled(
            NETWORK,
            true
        )

        networkReady = true
    }

    private fun publishCurrent() {

        val lat =
            State.getLat(this)

        val lon =
            State.getLon(this)

        if (
            lat == null ||
            lon == null ||
            !State.validLat(lat) ||
            !State.validLon(lon)
        ) {
            return
        }

        if (gpsReady) {
            publish(
                GPS,
                lat,
                lon
            )
        }

        if (networkReady) {
            publish(
                NETWORK,
                lat,
                lon
            )
        }

        updateNotification(
            lat,
            lon
        )
    }

    private fun publish(
        provider: String,
        lat: Double,
        lon: Double
    ) {

        val location =
            Location(provider)

        location.latitude = lat
        location.longitude = lon

        location.accuracy =
            if (provider == GPS) {
                3f
            } else {
                50f
            }

        location.altitude = 0.0
        location.time =
            System.currentTimeMillis()

        location.elapsedRealtimeNanos =
            SystemClock.elapsedRealtimeNanos()

        location.speed = 0f
        location.bearing = 0f

        try {
            locationManager.setTestProviderLocation(
                provider,
                location
            )
        } catch (_: Exception) {
        }
    }

    private fun updateNotification(
        lat: Double,
        lon: Double
    ) {

        if (
            notificationLat == lat &&
            notificationLon == lon
        ) {
            return
        }

        val changed =
            notificationLat != null &&
                    notificationLon != null

        notificationLat = lat
        notificationLon = lon

        getSystemService(
            NotificationManager::class.java
        ).notify(
            NOTIFICATION_ID,
            createLocationNotification(
                lat,
                lon
            )
        )

        if (changed) {
            getSystemService(
                NotificationManager::class.java
            ).notify(
                ALERT_NOTIFICATION_ID,
                createLocationChangedNotification(
                    lat,
                    lon
                )
            )
        }
    }

    private fun createNotificationChannels() {

        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.O
        ) {

            val notificationChannel =
                NotificationChannel(
                    CHANNEL_ID,
                    "MockGPS",
                    NotificationManager.IMPORTANCE_LOW
                )

            notificationChannel.setSound(
                null,
                null
            )

            notificationChannel.enableVibration(
                false
            )

            val alertChannel =
                NotificationChannel(
                    ALERT_CHANNEL_ID,
                    "Location updates",
                    NotificationManager.IMPORTANCE_HIGH
                )

            alertChannel.enableVibration(
                true
            )

            alertChannel.vibrationPattern =
                longArrayOf(
                    0,
                    200
                )

            getSystemService(
                NotificationManager::class.java
            ).createNotificationChannels(
                listOf(
                    notificationChannel,
                    alertChannel
                )
            )
        }
    }

    private fun notificationBuilder(
        channelId: String
    ): Notification.Builder {

        return if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.O
        ) {
            Notification.Builder(
                this,
                channelId
            )
        } else {
            Notification.Builder(this)
        }
    }

    private fun createStartingNotification():
            Notification {

        return notificationBuilder(
            CHANNEL_ID
        )
            .setContentTitle("MockGPS")
            .setContentText(
                "Starting mock location..."
            )
            .setSmallIcon(
                android.R.drawable.ic_menu_mylocation
            )
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(
                Notification.CATEGORY_SERVICE
            )
            .setShowWhen(false)
            .build()
    }

    private fun createLocationNotification(
        lat: Double,
        lon: Double
    ): Notification {

        return notificationBuilder(
            CHANNEL_ID
        )
            .setContentTitle("MockGPS")
            .setContentText(
                "Location: $lat, $lon"
            )
            .setSmallIcon(
                android.R.drawable.ic_menu_mylocation
            )
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(
                Notification.CATEGORY_SERVICE
            )
            .setShowWhen(false)
            .build()
    }

    private fun createLocationChangedNotification(
        lat: Double,
        lon: Double
    ): Notification {

        return notificationBuilder(
            ALERT_CHANNEL_ID
        )
            .setContentTitle("MockGPS")
            .setContentText(
                "Location changed: $lat, $lon"
            )
            .setSmallIcon(
                android.R.drawable.ic_menu_mylocation
            )
            .setAutoCancel(true)
            .setTimeoutAfter(3000)
            .setOnlyAlertOnce(false)
            .setCategory(
                Notification.CATEGORY_EVENT
            )
            .setShowWhen(false)
            .build()
    }

    private fun cleanupProviders() {

        try {
            if (gpsReady) {
                locationManager.setTestProviderEnabled(
                    GPS,
                    false
                )
            }
        } catch (_: Exception) {
        }

        try {
            locationManager.removeTestProvider(
                GPS
            )
        } catch (_: Exception) {
        }

        try {
            if (networkReady) {
                locationManager.setTestProviderEnabled(
                    NETWORK,
                    false
                )
            }
        } catch (_: Exception) {
        }

        try {
            locationManager.removeTestProvider(
                NETWORK
            )
        } catch (_: Exception) {
        }

        gpsReady = false
        networkReady = false
    }

    private fun stopServiceCleanly() {

        handler.removeCallbacksAndMessages(
            null
        )

        cleanupProviders()

        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.N
        ) {
            stopForeground(
                STOP_FOREGROUND_REMOVE
            )
        }

        stopSelf()
    }

    override fun onDestroy() {

        handler.removeCallbacksAndMessages(
            null
        )

        cleanupProviders()

        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.N
        ) {
            stopForeground(
                STOP_FOREGROUND_REMOVE
            )
        }

        super.onDestroy()
    }

    override fun onBind(
        intent: Intent?
    ): IBinder? {
        return null
    }
}