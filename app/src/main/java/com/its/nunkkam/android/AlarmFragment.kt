package com.its.nunkkam.android

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.NumberPicker
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.its.nunkkam.android.databinding.FragmentAlarmBinding
import java.util.*

class AlarmFragment : Fragment() {

    private var _binding: FragmentAlarmBinding? = null
    private val binding get() = _binding!!

    private var alarmManager: AlarmManager? = null
    private var isManageAlarm = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAlarmBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        alarmManager = requireContext().getSystemService(Context.ALARM_SERVICE) as AlarmManager

        setupListeners()
        updateAlarmView()
    }

    // 수정: setupListeners 함수 업데이트
    private fun setupListeners() {
        binding.apply {
            btnInterval.setOnClickListener { showIntervalPickerDialog() }

            switchAlarm.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    val intervalText = btnInterval.text.toString()
                    // 수정: 문자열 파싱 로직 개선
                    val minutes = intervalText.replace("분", "").toIntOrNull() ?: 0
                    if (minutes > 0) {
                        setupDailyAlarm(0, minutes)
                    } else {
                        Toast.makeText(context, "유효한 시간을 설정해주세요.", Toast.LENGTH_SHORT).show()
                        switchAlarm.isChecked = false
                    }
                } else {
                    cancelAlarm()
                }
            }

            btnMeasurementAlarm.setOnClickListener {
                isManageAlarm = false
                updateAlarmView()
            }

            btnManageAlarm.setOnClickListener {
                isManageAlarm = true
                updateAlarmView()
            }
        }
    }

    private fun showIntervalPickerDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_interval_picker, null)
        val numberPickerMinutes = dialogView.findViewById<NumberPicker>(R.id.numberPickerMinutes)

        numberPickerMinutes.minValue = 1
        numberPickerMinutes.maxValue = 60

        AlertDialog.Builder(requireContext())
            .setTitle("알람 주기 설정")
            .setView(dialogView)
            .setPositiveButton("확인") { _, _ ->
                val minutes = numberPickerMinutes.value
                binding.btnInterval.text = "${minutes}분"
                if (binding.switchAlarm.isChecked) {
                    setupDailyAlarm(0, minutes)
                }
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun setupDailyAlarm(hours: Int, minutes: Int) {
        val intent = Intent(context, AlarmReceiver2::class.java).apply {
            putExtra("isManageAlarm", isManageAlarm)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            if (isManageAlarm) 1 else 0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar: Calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            add(Calendar.MINUTE, minutes)
        }

        alarmManager?.setRepeating(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            (minutes * 60 * 1000).toLong(),
            pendingIntent
        )
    }

    private fun cancelAlarm() {
        val intent = Intent(context, AlarmReceiver2::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            if (isManageAlarm) 1 else 0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager?.cancel(pendingIntent)
    }

    private fun updateAlarmView() {
        binding.apply {
            if (isManageAlarm) {
                alarmSettingsLayout.visibility = View.GONE
                manageAlarmSettingsLayout.visibility = View.VISIBLE
                btnManageAlarm.setBackgroundResource(R.drawable.button_selected)
                btnMeasurementAlarm.setBackgroundResource(R.drawable.button_unselected)
            } else {
                alarmSettingsLayout.visibility = View.VISIBLE
                manageAlarmSettingsLayout.visibility = View.GONE
                btnMeasurementAlarm.setBackgroundResource(R.drawable.button_selected)
                btnManageAlarm.setBackgroundResource(R.drawable.button_unselected)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}