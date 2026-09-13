package dev.burixon.mockgps

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast

class MockGpsReceiver : BroadcastReceiver() {

    companion object {
        const val COMMAND = "command"

        const val COMMAND_SET = "set"
        const val COMMAND_ON = "on"
        const val COMMAND_OFF = "off"
        const val COMMAND_DRIFT = "drift"

        const val EXTRA_LAT = "lat"
        const val EXTRA_LON = "lon"
        const val EXTRA_NOTIFY = "notify"
        const val EXTRA_TOAST = "toast"

        const val EXTRA_BOOT = "boot"
        const val EXTRA_DRIFTING = "drifting"
    }

    override fun onReceive(
        context: Context,
        intent: Intent
    ) {
        /*
         * Start/stop launched directly through am startservice/am stopservice
         * do not pass through this receiver.
         * This switch therefore controls broadcast commands handled here.
         */

        if (
            !State.isBroadcastEnabled(context)
        ) {
            return
        }

        intent.getStringExtra(
            EXTRA_BOOT
        )?.let { value ->

            when (value) {

                "enable",
                "on" -> {
                    State.setAutoStart(
                        context,
                        true
                    )

                    showToast(
                        context,
                        intent,
                        "Launch on boot enabled."
                    )
                }

                "disable",
                "off" -> {
                    State.setAutoStart(
                        context,
                        false
                    )

                    showToast(
                        context,
                        intent,
                        "Launch on boot disabled."
                    )
                }
            }

            return
        }

        intent.getStringExtra(
            EXTRA_NOTIFY
        )?.let {
            State.setNotifyMode(
                context,
                it
            )
        }

        val command =
            intent.getStringExtra(
                COMMAND
            )

        when (command) {

            COMMAND_SET -> {

                val lat =
                    intent.getStringExtra(
                        EXTRA_LAT
                    )
                        ?.toDoubleOrNull()

                val lon =
                    intent.getStringExtra(
                        EXTRA_LON
                    )
                        ?.toDoubleOrNull()

                if (
                    lat == null ||
                    lon == null ||
                    !State.validLat(lat) ||
                    !State.validLon(lon)
                ) {
                    showToast(
                        context,
                        intent,
                        "Invalid location."
                    )
                    return
                }

                State.setLocation(
                    context,
                    lat,
                    lon
                )

                showToast(
                    context,
                    intent,
                    "Location changed."
                )
            }

            COMMAND_ON -> {

                State.ensureLocation(
                    context
                )

                State.setDesiredEnabled(
                    context,
                    true
                )

                val serviceIntent =
                    Intent(
                        context,
                        MockLocationService::class.java
                    ).apply {

                        putExtra(
                            MockLocationService.COMMAND,
                            MockLocationService.COMMAND_ON
                        )

                        putExtra(
                            MockLocationService.EXTRA_TOAST,
                            shouldToast(intent)
                        )
                    }

                try {
                    context.startForegroundService(
                        serviceIntent
                    )
                } catch (_: Exception) {
                }

                if (
                    shouldToast(intent)
                ) {
                    Toast.makeText(
                        context,
                        "Mocking started.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            COMMAND_OFF -> {

                State.setDesiredEnabled(
                    context,
                    false
                )

                State.setServiceRunning(
                    context,
                    false
                )

                context.stopService(
                    Intent(
                        context,
                        MockLocationService::class.java
                    )
                )

                showToast(
                    context,
                    intent,
                    "Mocking stopped."
                )
            }

            COMMAND_DRIFT -> {

                val drifting =
                    when (
                        intent.getStringExtra(
                            EXTRA_DRIFTING
                        )
                    ) {
                        "enable",
                        "on",
                        "yes",
                        "true" -> true

                        "disable",
                        "off",
                        "no",
                        "false" -> false

                        else -> {
                            showToast(
                                context,
                                intent,
                                "Invalid drift setting."
                            )
                            return
                        }
                    }

                State.setDrifting(
                    context,
                    drifting
                )

                showToast(
                    context,
                    intent,
                    if (drifting) {
                        "Location drift enabled."
                    } else {
                        "Location drift disabled."
                    }
                )
            }
        }
    }

    private fun shouldToast(
        intent: Intent
    ): Boolean {
        return intent.getStringExtra(
            EXTRA_TOAST
        )?.lowercase() != "no"
    }

    private fun showToast(
        context: Context,
        intent: Intent,
        message: String
    ) {
        if (
            shouldToast(intent)
        ) {
            Toast.makeText(
                context,
                message,
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}