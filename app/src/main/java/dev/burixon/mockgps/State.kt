package dev.burixon.mockgps

import android.content.Context

object State {

    private const val PREFS = "mockgps"

    private const val LAT = "lat"
    private const val LON = "lon"

    private const val REAL_LAT = "real_lat"
    private const val REAL_LON = "real_lon"

    private const val DESIRED_ENABLED = "desired_enabled"
    private const val SERVICE_RUNNING = "service_running"

    private const val AUTO_START = "auto_start"
    private const val BROADCAST_ENABLED = "broadcast_enabled"

    private const val NOTIFY_MODE = "notify_mode"
    private const val DRIFTING = "drifting"

    const val NOTIFY_NONE = "none"
    const val NOTIFY_PUSH = "push"
    const val NOTIFY_TOAST = "toast"

    private fun prefs(context: Context) =
        context.createDeviceProtectedStorageContext()
            .getSharedPreferences(
                PREFS,
                Context.MODE_PRIVATE
            )

    fun setLocation(
        context: Context,
        lat: Double,
        lon: Double
    ) {
        prefs(context)
            .edit()
            .putString(
                LAT,
                lat.toString()
            )
            .putString(
                LON,
                lon.toString()
            )
            .commit()
    }

    fun getLat(
        context: Context
    ): Double? =
        prefs(context)
            .getString(
                LAT,
                null
            )
            ?.toDoubleOrNull()

    fun getLon(
        context: Context
    ): Double? =
        prefs(context)
            .getString(
                LON,
                null
            )
            ?.toDoubleOrNull()

    fun ensureLocation(
        context: Context
    ) {
        if (
            getLat(context) == null ||
            getLon(context) == null
        ) {
            setLocation(
                context,
                0.0,
                0.0
            )
        }
    }

    fun setRealLocation(
        context: Context,
        lat: Double,
        lon: Double
    ) {
        prefs(context)
            .edit()
            .putString(
                REAL_LAT,
                lat.toString()
            )
            .putString(
                REAL_LON,
                lon.toString()
            )
            .commit()
    }

    fun getRealLat(
        context: Context
    ): Double? =
        prefs(context)
            .getString(
                REAL_LAT,
                null
            )
            ?.toDoubleOrNull()

    fun getRealLon(
        context: Context
    ): Double? =
        prefs(context)
            .getString(
                REAL_LON,
                null
            )
            ?.toDoubleOrNull()

    fun setDesiredEnabled(
        context: Context,
        enabled: Boolean
    ) {
        prefs(context)
            .edit()
            .putBoolean(
                DESIRED_ENABLED,
                enabled
            )
            .commit()
    }

    fun isDesiredEnabled(
        context: Context
    ): Boolean =
        prefs(context)
            .getBoolean(
                DESIRED_ENABLED,
                false
            )

    fun setServiceRunning(
        context: Context,
        running: Boolean
    ) {
        prefs(context)
            .edit()
            .putBoolean(
                SERVICE_RUNNING,
                running
            )
            .commit()
    }

    fun isServiceRunning(
        context: Context
    ): Boolean =
        prefs(context)
            .getBoolean(
                SERVICE_RUNNING,
                false
            )

    fun setAutoStart(
        context: Context,
        enabled: Boolean
    ) {
        prefs(context)
            .edit()
            .putBoolean(
                AUTO_START,
                enabled
            )
            .commit()
    }

    fun isAutoStart(
        context: Context
    ): Boolean =
        prefs(context)
            .getBoolean(
                AUTO_START,
                false
            )

    fun setBroadcastEnabled(
        context: Context,
        enabled: Boolean
    ) {
        prefs(context)
            .edit()
            .putBoolean(
                BROADCAST_ENABLED,
                enabled
            )
            .commit()
    }

    fun isBroadcastEnabled(
        context: Context
    ): Boolean =
        prefs(context)
            .getBoolean(
                BROADCAST_ENABLED,
                true
            )

    fun setNotifyMode(
        context: Context,
        mode: String
    ) {
        val normalized =
            when (mode) {
                NOTIFY_NONE,
                NOTIFY_PUSH,
                NOTIFY_TOAST -> mode

                else -> NOTIFY_TOAST
            }

        prefs(context)
            .edit()
            .putString(
                NOTIFY_MODE,
                normalized
            )
            .commit()
    }

    fun getNotifyMode(
        context: Context
    ): String =
        prefs(context)
            .getString(
                NOTIFY_MODE,
                NOTIFY_TOAST
            )
            ?: NOTIFY_TOAST

    fun setDrifting(
        context: Context,
        enabled: Boolean
    ) {
        prefs(context)
            .edit()
            .putBoolean(
                DRIFTING,
                enabled
            )
            .commit()
    }

    fun isDrifting(
        context: Context
    ): Boolean =
        prefs(context)
            .getBoolean(
                DRIFTING,
                false
            )

    fun validLat(
        value: Double
    ): Boolean =
        value.isFinite() &&
                value >= -90.0 &&
                value <= 90.0

    fun validLon(
        value: Double
    ): Boolean =
        value.isFinite() &&
                value >= -180.0 &&
                value <= 180.0
}