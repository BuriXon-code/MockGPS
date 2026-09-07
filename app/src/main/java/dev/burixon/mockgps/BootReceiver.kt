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

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(
        context: Context,
        intent: Intent
    ) {

        when (intent.action) {

            Intent.ACTION_LOCKED_BOOT_COMPLETED,
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_USER_UNLOCKED,
            Intent.ACTION_MY_PACKAGE_REPLACED -> {

                if (
                    !State.isDesiredEnabled(context)
                ) {
                    return
                }

                State.ensureLocation(context)

                val serviceIntent =
                    Intent(
                        context,
                        MockLocationService::class.java
                    ).apply {
                        putExtra(
                            "command",
                            "on"
                        )
                    }

                try {
                    context.startForegroundService(
                        serviceIntent
                    )
                } catch (_: Exception) {
                }
            }
        }
    }
}
