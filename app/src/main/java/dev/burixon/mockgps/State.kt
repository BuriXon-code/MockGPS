/*
 * MockGPS
 * Copyright (C) 2026 BuriXon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package dev.burixon.mockgps

import android.content.Context

object State {

    private const val PREFS = "mockgps"

    private const val LAT = "lat"
    private const val LON = "lon"
    private const val DESIRED_ENABLED = "desired_enabled"

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
            .putString(LAT, lat.toString())
            .putString(LON, lon.toString())
            .commit()
    }

    fun getLat(context: Context): Double? =
        prefs(context)
            .getString(LAT, null)
            ?.toDoubleOrNull()

    fun getLon(context: Context): Double? =
        prefs(context)
            .getString(LON, null)
            ?.toDoubleOrNull()

    fun ensureLocation(context: Context) {

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
