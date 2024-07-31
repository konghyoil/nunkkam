package com.its.nunkkam.android

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class AlarmReceiver : BroadcastReceiver() {

    @SuppressLint("MissingPermission")
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context != null) {
            val builder = NotificationCompat.Builder(context, "alarmChannel")
                .setSmallIcon(R.drawable.ic_launcher_background)
                .setContentTitle("알람")
                .setContentText("측정을 진행해주십쇼")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)

            with(NotificationManagerCompat.from(context)) {
                notify(1, builder.build())
            }
        }
    }
}
