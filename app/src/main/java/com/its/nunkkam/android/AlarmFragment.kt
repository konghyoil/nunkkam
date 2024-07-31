package com.its.nunkkam.android

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.NumberPicker
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import java.util.Calendar

class AlarmFragment : Fragment(R.layout.fragment_alarm) {

    private lateinit var btnInterval: Button
    private lateinit var alarmManager: AlarmManager
    private var pendingIntent: PendingIntent? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        btnInterval = view.findViewById(R.id.btnInterval)
        alarmManager = requireContext().getSystemService(Context.ALARM_SERVICE) as AlarmManager

        btnInterval.setOnClickListener {
            showTimePickerDialog()
        }
    }

    private fun showTimePickerDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_interval_picker, null)
        val numberPickerHours = dialogView.findViewById<NumberPicker>(R.id.numberPickerHours)
        val numberPickerMinutes = dialogView.findViewById<NumberPicker>(R.id.numberPickerMinutes)

        numberPickerHours.maxValue = 23
        numberPickerHours.minValue = 0
        numberPickerMinutes.maxValue = 59
        numberPickerMinutes.minValue = 0

        AlertDialog.Builder(requireContext())
            .setTitle("알람 주기 설정")
            .setView(dialogView)
            .setPositiveButton("확인") { _, _ ->
                val hours = numberPickerHours.value
                val minutes = numberPickerMinutes.value
                val interval = hours * 60 + minutes
                btnInterval.text = "${hours}시간 ${minutes}분"
                setAlarm(interval)
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun setAlarm(interval: Int) {
        val intent = Intent(requireContext(), AlarmReceiver::class.java)

        pendingIntent = PendingIntent.getBroadcast(
            requireContext(),
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE // 플래그 추가
        )

        val triggerTime = System.currentTimeMillis() + interval * 60 * 1000

        alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
        Toast.makeText(requireContext(), "알람이 설정되었습니다.", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        val pi = pendingIntent // 로컬 변수에 할당
        if (pi != null) {
            alarmManager.cancel(pi)
        }
    }
}
