package com.its.nunkkam.android

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat

class AlarmReceiver2 : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("AlarmReceiver2", "onReceive called")

        createNotificationChannel(context) // 알림 채널 생성

        val isManageAlarm = intent.getBooleanExtra("isManageAlarm", false)
        Log.d("AlarmReceiver2", "isManageAlarm: $isManageAlarm")

        val notificationTitle = if (isManageAlarm) "관리 알람" else "측정 알람"
        val notificationText = if (isManageAlarm) "눈 건강을 위해 관리를 진행해주세요." else "눈 건강을 위해 측정을 진행해주세요."
        Log.d("AlarmReceiver2", "Notification Title: $notificationTitle, Text: $notificationText")

        val sharedPrefs = context.getSharedPreferences("AlarmSettings", Context.MODE_PRIVATE)
        val isSoundEnabled = sharedPrefs.getBoolean("sound_enabled", true)
        val isVibrationEnabled = sharedPrefs.getBoolean("vibration_enabled", true)
        Log.d("AlarmReceiver2", "Sound Enabled: $isSoundEnabled, Vibration Enabled: $isVibrationEnabled")

        val notification = NotificationCompat.Builder(context, "alarmChannel")
            .setSmallIcon(R.drawable.ic_notification) // 실제 존재하는 아이콘 리소스 사용
            .setContentTitle(notificationTitle)
            .setContentText(notificationText)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        if (isSoundEnabled) {
            notification.setDefaults(NotificationCompat.DEFAULT_SOUND)
            Log.d("AlarmReceiver2", "Sound is enabled")
        }

        if (isVibrationEnabled) {
            notification.setDefaults(NotificationCompat.DEFAULT_VIBRATE)
            Log.d("AlarmReceiver2", "Vibration is enabled")
        }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(if (isManageAlarm) 1002 else 1001, notification.build())
        Log.d("AlarmReceiver2", "Notification sent")
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "알람 채널"
            val descriptionText = "알람을 위한 채널입니다."
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel("alarmChannel", name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
