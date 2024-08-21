package com.its.nunkkam.android

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import java.util.Date

class AlarmReceiver2 : BroadcastReceiver() {
    @SuppressLint("ScheduleExactAlarm")
    override fun onReceive(context: Context, intent: Intent) {
        // 알림 채널 생성
        createNotificationChannel(context)

        // 알람이 관리 알람인지 측정 알람인지 여부 확인
        val isManageAlarm = intent.getBooleanExtra("isManageAlarm", false)

        // 알람 제목과 내용 설정
        val notificationTitle = if (isManageAlarm) "관리 알람" else "측정 알람"
        val notificationText = if (isManageAlarm) "눈 건강을 위해 관리를 진행해주세요." else "눈 건강을 위해 측정을 진행해주세요."

        // 알림 빌더 설정
        val notificationBuilder = NotificationCompat.Builder(context, "alarmChannel")
            .setSmallIcon(R.drawable.app_icon) // 알림 아이콘 설정
            .setContentTitle(notificationTitle) // 알림 제목 설정
            .setContentText(notificationText) // 알림 내용 설정
            .setPriority(NotificationCompat.PRIORITY_HIGH) // 알림 우선순위 설정
            .setAutoCancel(true) // 알림 클릭 시 자동 제거

        // 알림 매니저를 통해 알림 전송
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(if (isManageAlarm) 1002 else 1001, notificationBuilder.build())

        // 다음 알람 설정 (반복 알람인 경우)
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
            }
        }
    }

    // 알림 채널 생성 메서드
    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "알람 채널"
            val descriptionText = "알람을 위한 채널입니다."
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel("alarmChannel", name, importance).apply {
                description = descriptionText
                setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION), AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build())
                enableVibration(true)
            }
            val notificationManager: NotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}