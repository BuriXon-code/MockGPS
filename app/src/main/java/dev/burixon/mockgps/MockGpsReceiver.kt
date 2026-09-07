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

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class MockGpsReceiver : BroadcastReceiver() {

    companion object {
        const val COMMAND = "command"

        const val COMMAND_SET = "set"
        const val COMMAND_OFF = "off"

        const val EXTRA_LAT = "lat"
        const val EXTRA_LON = "lon"
    }

    override fun onReceive(
        context: Context,
        intent: Intent
    ) {

        when (
            intent.getStringExtra(COMMAND)
        ) {

            COMMAND_SET -> {

                val lat =
                    intent
                        .getStringExtra(EXTRA_LAT)
                        ?.toDoubleOrNull()

                val lon =
                    intent
                        .getStringExtra(EXTRA_LON)
                        ?.toDoubleOrNull()

                if (
                    lat == null ||
                    lon == null ||
                    !State.validLat(lat) ||
                    !State.validLon(lon)
                ) {
                    return
                }

                State.setLocation(
                    context,
                    lat,
                    lon
                )
            }

            COMMAND_OFF -> {

                State.setDesiredEnabled(
                    context,
                    false
                )
            }
        }
    }
}
