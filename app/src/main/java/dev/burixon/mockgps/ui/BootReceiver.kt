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