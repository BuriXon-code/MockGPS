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