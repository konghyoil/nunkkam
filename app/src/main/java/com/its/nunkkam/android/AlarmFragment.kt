package com.its.nunkkam.android

import android.app.AlertDialog
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.*
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

class AlarmFragment : Fragment() {

    private lateinit var switchAlarm: Switch
    private lateinit var btnInterval: Button
    private lateinit var switchSound: Switch
    private lateinit var switchVibration: Switch
    private lateinit var btnMeasurementAlarm: Button
    private lateinit var btnManageAlarm: Button
    private lateinit var alarmSettingsLayout: View
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.activity_alarm, container, false)

        switchAlarm = view.findViewById(R.id.switchAlarm)
        btnInterval = view.findViewById(R.id.btnInterval)
        switchSound = view.findViewById(R.id.switchSound)
        switchVibration = view.findViewById(R.id.switchVibration)
        btnMeasurementAlarm = view.findViewById(R.id.btnMeasurementAlarm)
        btnManageAlarm = view.findViewById(R.id.btnManageAlarm)
        alarmSettingsLayout = view.findViewById(R.id.alarmSettingsLayout)

        sharedPreferences = requireActivity().getSharedPreferences("AlarmPreferences", Context.MODE_PRIVATE)

        // 버튼 클릭 리스너 설정
        btnMeasurementAlarm.setOnClickListener {
            switchToMeasurementAlarm()
        }

        btnManageAlarm.setOnClickListener {
            switchToManageAlarm()
        }

        btnInterval.setOnClickListener {
            showIntervalDialog()
        }

        // 스위치 및 기타 UI 요소 초기화
        initUI()

        return view
    }

    private fun switchToMeasurementAlarm() {
        btnMeasurementAlarm.setBackgroundColor(resources.getColor(R.color.colorPrimary))
        btnManageAlarm.setBackgroundColor(resources.getColor(R.color.colorPrimaryDark))
        // 측정 알람 설정을 처리하는 로직 추가
        Toast.makeText(context, "측정 알람 설정", Toast.LENGTH_SHORT).show()
        // 여기서 측정 알람과 관련된 설정을 로드하거나 처리합니다.
    }

    private fun switchToManageAlarm() {
        btnManageAlarm.setBackgroundColor(resources.getColor(R.color.colorPrimary))
        btnMeasurementAlarm.setBackgroundColor(resources.getColor(R.color.colorPrimaryDark))
        // 관리 알람 설정을 처리하는 로직 추가
        Toast.makeText(context, "관리 알람 설정", Toast.LENGTH_SHORT).show()
        // 여기서 관리 알람과 관련된 설정을 로드하거나 처리합니다.
    }

    private fun initUI() {
        // SharedPreferences를 사용하여 이전에 저장된 값을 로드하고 UI를 초기화합니다.
        switchAlarm.isChecked = sharedPreferences.getBoolean("alarm_enabled", false)
        switchSound.isChecked = sharedPreferences.getBoolean("alarm_sound_enabled", false)
        switchVibration.isChecked = sharedPreferences.getBoolean("alarm_vibration_enabled", false)

        val hours = sharedPreferences.getInt("alarm_hours", 0)
        val minutes = sharedPreferences.getInt("alarm_minutes", 20)
        btnInterval.text = formatIntervalText(hours, minutes)

        switchAlarm.setOnCheckedChangeListener { _, isChecked ->
            // 알람 on/off 스위치 처리
            val editor = sharedPreferences.edit()
            editor.putBoolean("alarm_enabled", !isChecked) // 반대로 저장
            editor.apply()
            Toast.makeText(context, if (!isChecked) "알람이 켜졌습니다" else "알람이 꺼졌습니다", Toast.LENGTH_SHORT).show()
        }

        switchSound.setOnCheckedChangeListener { _, isChecked ->
            // 소리 on/off 스위치 처리
            val editor = sharedPreferences.edit()
            editor.putBoolean("alarm_sound_enabled", !isChecked) // 반대로 저장
            editor.apply()
            Toast.makeText(context, if (!isChecked) "알람 소리가 켜졌습니다" else "알람 소리가 꺼졌습니다", Toast.LENGTH_SHORT).show()
        }

        switchVibration.setOnCheckedChangeListener { _, isChecked ->
            // 진동 on/off 스위치 처리
            val editor = sharedPreferences.edit()
            editor.putBoolean("alarm_vibration_enabled", !isChecked) // 반대로 저장
            editor.apply()
            Toast.makeText(context, if (!isChecked) "알람 진동이 켜졌습니다" else "알람 진동이 꺼졌습니다", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showIntervalDialog() {
        val builder = AlertDialog.Builder(requireContext())
        val dialogView = layoutInflater.inflate(R.layout.dialog_interval_picker, null)
        val numberPickerHours: NumberPicker = dialogView.findViewById(R.id.numberPickerHours)
        val numberPickerMinutes: NumberPicker = dialogView.findViewById(R.id.numberPickerMinutes)

        numberPickerHours.minValue = 0
        numberPickerHours.maxValue = 23
        numberPickerMinutes.minValue = 0
        numberPickerMinutes.maxValue = 59

        val hours = sharedPreferences.getInt("alarm_hours", 0)
        val minutes = sharedPreferences.getInt("alarm_minutes", 20)

        numberPickerHours.value = hours
        numberPickerMinutes.value = minutes

        builder.setView(dialogView)
        builder.setTitle("알람 주기 설정")
        builder.setPositiveButton("확인") { dialog, _ ->
            val selectedHours = numberPickerHours.value
            val selectedMinutes = numberPickerMinutes.value

            if (selectedHours == 0 && selectedMinutes == 0) {
                // 모든 값이 0이면 알람을 끕니다.
                switchAlarm.isChecked = true
                Toast.makeText(context, "알람이 꺼졌습니다", Toast.LENGTH_SHORT).show()
            } else {
                btnInterval.text = formatIntervalText(selectedHours, selectedMinutes)
                val editor = sharedPreferences.edit()
                editor.putInt("alarm_hours", selectedHours)
                editor.putInt("alarm_minutes", selectedMinutes)
                editor.putBoolean("alarm_enabled", true)
                editor.apply()
                switchAlarm.isChecked = false
                Toast.makeText(context, "알람 주기가 ${formatIntervalText(selectedHours, selectedMinutes)}으로 설정되었습니다", Toast.LENGTH_SHORT).show()
            }
            dialog.dismiss()
        }
        builder.setNegativeButton("취소") { dialog, _ ->
            dialog.dismiss()
        }
        builder.create().show()
    }

    private fun formatIntervalText(hours: Int, minutes: Int): String {
        val parts = mutableListOf<String>()
        if (hours > 0) parts.add("${hours}시간")
        if (minutes > 0) parts.add("${minutes}분")
        return if (parts.isEmpty()) "0분" else parts.joinToString(" ")
    }
}
