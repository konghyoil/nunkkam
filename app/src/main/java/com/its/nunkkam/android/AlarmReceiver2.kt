package com.its.nunkkam.android

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import java.util.Date

class AlarmReceiver2 : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("AlarmReceiver2", "onReceive called")

        createNotificationChannel(context)

        val isManageAlarm = intent.getBooleanExtra("isManageAlarm", false)
        Log.d("AlarmReceiver2", "isManageAlarm: $isManageAlarm")

        val notificationTitle = if (isManageAlarm) "관리 알람" else "측정 알람"
        val notificationText = if (isManageAlarm) "눈 건강을 위해 관리를 진행해주세요." else "눈 건강을 위해 측정을 진행해주세요."
        Log.d("AlarmReceiver2", "Notification Title: $notificationTitle, Text: $notificationText")

        val sharedPrefs = context.getSharedPreferences("AlarmPreferences", Context.MODE_PRIVATE)
        val isSoundEnabled = if (isManageAlarm) {
            sharedPrefs.getBoolean("manageSoundEnabled", true)
        } else {
            sharedPrefs.getBoolean("measurementSoundEnabled", true)
        }
        val isVibrationEnabled = if (isManageAlarm) {
            sharedPrefs.getBoolean("manageVibrationEnabled", true)
        } else {
            sharedPrefs.getBoolean("measurementVibrationEnabled", true)
        }
        Log.d("AlarmReceiver2", "Sound Enabled: $isSoundEnabled, Vibration Enabled: $isVibrationEnabled")

        val notificationBuilder = NotificationCompat.Builder(context, "alarmChannel")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(notificationTitle)
            .setContentText(notificationText)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        // 소리 설정
        if (isSoundEnabled) {
            val soundUri: Uri = android.provider.Settings.System.DEFAULT_NOTIFICATION_URI
            notificationBuilder.setSound(soundUri)
            Log.d("AlarmReceiver2", "Sound is enabled")
        } else {
            notificationBuilder.setSound(null)
        }

        // 진동 설정
        if (isVibrationEnabled) {
            notificationBuilder.setVibrate(longArrayOf(0, 1000, 500, 1000))
            Log.d("AlarmReceiver2", "Vibration is enabled")
        } else {
            notificationBuilder.setVibrate(null)
        }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(if (isManageAlarm) 1002 else 1001, notificationBuilder.build())
        Log.d("AlarmReceiver2", "Notification sent")

        // 다음 알람 설정
        if (isManageAlarm) {
            val intervalMillis = intent.getLongExtra("intervalMillis", 0)
            if (intervalMillis > 0) {
                val nextAlarmTime = System.currentTimeMillis() + intervalMillis
                val nextIntent = Intent(context, AlarmReceiver2::class.java).apply {
                    putExtra("isManageAlarm", true)
                    putExtra("intervalMillis", intervalMillis)
                }
                val nextPendingIntent = PendingIntent.getBroadcast(
                    context, 1002, nextIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    nextAlarmTime,
                    nextPendingIntent
                )
                Log.d("AlarmReceiver2", "Next repeating alarm set for ${Date(nextAlarmTime)}")
            }
        }
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
