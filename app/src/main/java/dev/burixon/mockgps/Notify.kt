package dev.burixon.mockgps

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.widget.Toast

object Notify {

    const val NONE = "none"
    const val PUSH = "push"
    const val TOAST = "toast"

    private const val CHANNEL_ID = "mockgps_info"
    private const val NOTIFICATION_ID = 10

    fun show(
        context: Context,
        mode: String?,
        title: String,
        text: String
    ) {
        when (mode) {

            NONE -> {
            }

            TOAST -> {
                Toast.makeText(
                    context,
                    "$title: $text",
                    Toast.LENGTH_SHORT
                ).show()
            }

            PUSH -> {
                showPush(
                    context,
                    title,
                    text
                )
            }
        }
    }

    private fun showPush(
        context: Context,
        title: String,
        text: String
    ) {
        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.TIRAMISU &&
            context.checkSelfPermission(
                android.Manifest.permission.POST_NOTIFICATIONS
            ) !=
            android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val manager =
            context.getSystemService(
                NotificationManager::class.java
            )

        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.O
        ) {
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    "MockGPS information",
                    NotificationManager.IMPORTANCE_DEFAULT
                )
            )
        }

        val notification =
            if (
                Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.O
            ) {
                Notification.Builder(
                    context,
                    CHANNEL_ID
                )
            } else {
                Notification.Builder(context)
            }
                .setSmallIcon(
                    android.R.drawable.ic_menu_mylocation
                )
                .setContentTitle(title)
                .setContentText(text)
                .setAutoCancel(true)
                .build()

        manager.notify(
            NOTIFICATION_ID,
            notification
        )
    }
}