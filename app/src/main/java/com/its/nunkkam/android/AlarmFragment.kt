package com.its.nunkkam.android

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.NumberPicker
import android.widget.Switch
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.anychart.data.View
import java.util.*

class AlarmFragment : Fragment(R.layout.fragment_alarm) {

    private lateinit var switchAlarm: Switch
    private lateinit var btnInterval: Button
    private lateinit var switchSound: Switch
    private lateinit var switchVibration: Switch

    private var alarmManager: AlarmManager? = null
    private var pendingIntent: PendingIntent? = null

    override fun onViewCreated(view: android.view.View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        switchAlarm = view.findViewById(R.id.switchAlarm)
        btnInterval = view.findViewById(R.id.btnInterval)
        switchSound = view.findViewById(R.id.switchSound)
        switchVibration = view.findViewById(R.id.switchVibration)

        alarmManager = context?.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        btnInterval.setOnClickListener {
            showIntervalPickerDialog()
        }

        switchAlarm.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                setAlarm()
            } else {
                cancelAlarm()
            }
        }
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
            }
            .setNegativeButton("취소", null)
            .show()
    }

    @SuppressLint("MissingPermission")
    private fun setAlarm() {
        val intervalText = btnInterval.text.toString()
        val intervalParts = intervalText.split(" ")
        val hours = intervalParts[0].replace("시간", "").toInt()
        val minutes = intervalParts[1].replace("분", "").toInt()
        val intervalMillis = (hours * 3600 + minutes * 60) * 1000L

        val intent = Intent(context, MeasurementAlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)

        val triggerTime = System.currentTimeMillis() + intervalMillis

        alarmManager?.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
    }


    private fun cancelAlarm() {
        val intent = Intent(context, MeasurementAlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        alarmManager?.cancel(pendingIntent)
    }
}