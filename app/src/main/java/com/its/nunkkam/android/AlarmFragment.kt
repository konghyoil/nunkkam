package com.its.nunkkam.android

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.NumberPicker
import android.widget.Switch
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import java.util.*

class AlarmFragment : Fragment() {

    private lateinit var switchAlarm: Switch
    private lateinit var btnInterval: Button
    private lateinit var switchSound: Switch
    private lateinit var switchVibration: Switch
    private lateinit var btnMeasurementAlarm: Button
    private lateinit var btnManageAlarm: Button

    private var alarmManager: AlarmManager? = null
    private var pendingIntent: PendingIntent? = null

    private var isManageAlarm = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_alarm, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        switchAlarm = view.findViewById(R.id.switchAlarm)
        btnInterval = view.findViewById(R.id.btnInterval)
        switchSound = view.findViewById(R.id.switchSound)
        switchVibration = view.findViewById(R.id.switchVibration)
        btnMeasurementAlarm = view.findViewById(R.id.btnMeasurementAlarm)
        btnManageAlarm = view.findViewById(R.id.btnManageAlarm)

        alarmManager = requireContext().getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // 알람 간격 설정 버튼 클릭 리스너
        btnInterval.setOnClickListener {
            showIntervalPickerDialog()
        }

        // 알람 활성화/비활성화 스위치 리스너
        switchAlarm.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                setAlarm()
            } else {
                cancelAlarm()
            }
        }

        // 알림음 설정 스위치 리스너
        switchSound.setOnCheckedChangeListener { _, isChecked ->
            // 알림음 설정 저장
            saveSoundSetting(isChecked)
        }

        // 진동 설정 스위치 리스너
        switchVibration.setOnCheckedChangeListener { _, isChecked ->
            // 진동 설정 저장
            saveVibrationSetting(isChecked)
        }

        // 측정 알람 버튼 클릭 리스너
        btnMeasurementAlarm.setOnClickListener {
            isManageAlarm = false
            updateAlarmView()
        }

        // 관리 알람 버튼 클릭 리스너
        btnManageAlarm.setOnClickListener {
            isManageAlarm = true
            updateAlarmView()
        }

        // 초기 뷰 설정
        updateAlarmView()

        // 알림 채널 생성
        createNotificationChannel()
    }

    private fun showIntervalPickerDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_interval_picker, null)
        val numberPickerHours = dialogView.findViewById<NumberPicker>(R.id.numberPickerHours)
        val numberPickerMinutes = dialogView.findViewById<NumberPicker>(R.id.numberPickerMinutes)

        numberPickerHours.maxValue = 23
        numberPickerMinutes.maxValue = 59

        AlertDialog.Builder(requireContext())
            .setTitle("알람 주기 설정")
            .setView(dialogView)
            .setPositiveButton("확인") { _, _ ->
                val hours = numberPickerHours.value
                val minutes = numberPickerMinutes.value
                val intervalText = "${hours}시간 ${minutes}분"
                btnInterval.text = intervalText
                if (switchAlarm.isChecked) {
                    setAlarm()
                }
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun setAlarm() {
        val intervalText = btnInterval.text.toString()
        val intervalParts = intervalText.split(" ")
        val hours = intervalParts[0].replace("시간", "").toInt()
        val minutes = intervalParts[1].replace("분", "").toInt()
        val intervalMillis = (hours * 3600 + minutes * 60) * 1000L

        // AlarmReceiver를 AlarmReceiver2로 변경
        val intent = Intent(context, AlarmReceiver2::class.java).apply {
            putExtra("isManageAlarm", isManageAlarm)
        }
        pendingIntent = PendingIntent.getBroadcast(context, if (isManageAlarm) 1 else 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val triggerTime = System.currentTimeMillis() + intervalMillis

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager?.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
        } else {
            alarmManager?.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
        }
    }

    private fun cancelAlarm() {
        pendingIntent?.let { alarmManager?.cancel(it) }
    }

    private fun saveSoundSetting(isEnabled: Boolean) {
        // SharedPreferences를 사용하여 설정 저장
        val sharedPrefs = requireContext().getSharedPreferences("AlarmSettings", Context.MODE_PRIVATE)
        sharedPrefs.edit().putBoolean("sound_enabled", isEnabled).apply()
    }

    private fun saveVibrationSetting(isEnabled: Boolean) {
        // SharedPreferences를 사용하여 설정 저장
        val sharedPrefs = requireContext().getSharedPreferences("AlarmSettings", Context.MODE_PRIVATE)
        sharedPrefs.edit().putBoolean("vibration_enabled", isEnabled).apply()
    }

    private fun updateAlarmView() {
        if (isManageAlarm) {
            view?.findViewById<View>(R.id.alarmSettingsLayout)?.visibility = View.GONE
            view?.findViewById<View>(R.id.manageAlarmSettingsLayout)?.visibility = View.VISIBLE
            btnManageAlarm.setBackgroundResource(R.drawable.button_selected)
            btnMeasurementAlarm.setBackgroundResource(R.drawable.button_unselected)
        } else {
            view?.findViewById<View>(R.id.alarmSettingsLayout)?.visibility = View.VISIBLE
            view?.findViewById<View>(R.id.manageAlarmSettingsLayout)?.visibility = View.GONE
            btnMeasurementAlarm.setBackgroundResource(R.drawable.btton_selected)
            btnManageAlarm.setBackgroundResource(R.drawable.button_unselectedu)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "알람 채널"
            val descriptionText = "알람 알림을 위한 채널"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel("alarmChannel", name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                requireContext().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}