package com.its.nunkkam.android

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat

class AlarmReceiver2 : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val isManageAlarm = intent.getBooleanExtra("isManageAlarm", false)
        val notificationTitle = if (isManageAlarm) "관리 알람" else "측정 알람"
        val notificationText = if (isManageAlarm) "눈 건강을 위해 관리를 진행해주세요." else "눈 건강을 위해 측정을 진행해주세요."

        val sharedPrefs = context.getSharedPreferences("AlarmSettings", Context.MODE_PRIVATE)
        val isSoundEnabled = sharedPrefs.getBoolean("sound_enabled", true)
        val isVibrationEnabled = sharedPrefs.getBoolean("vibration_enabled", true)

        val notification = NotificationCompat.Builder(context, "alarmChannel")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(notificationTitle)
            .setContentText(notificationText)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        if (isSoundEnabled) {
            notification.setDefaults(NotificationCompat.DEFAULT_SOUND)
        }

        if (isVibrationEnabled) {
            notification.setDefaults(NotificationCompat.DEFAULT_VIBRATE)
        }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(if (isManageAlarm) 1 else 0, notification.build())
    }
}