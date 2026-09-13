package dev.burixon.mockgps

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_RESTART_SERVICE =
            "dev.burixon.mockgps.RESTART_SERVICE"
    }

    override fun onReceive(
        context: Context,
        intent: Intent
    ) {

        when (intent.action) {

            Intent.ACTION_BOOT_COMPLETED -> {

                if (
                    !State.isAutoStart(
                        context
                    )
                ) {
                    return
                }

                startService(
                    context
                )
            }

            ACTION_RESTART_SERVICE -> {

                if (
                    !State.isDesiredEnabled(
                        context
                    )
                ) {
                    return
                }

                startService(
                    context
                )
            }
        }
    }

    private fun startService(
        context: Context
    ) {

        State.ensureLocation(
            context
        )

        State.setDesiredEnabled(
            context,
            true
        )

        State.setNotifyMode(
            context,
            State.NOTIFY_NONE
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
                    false
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