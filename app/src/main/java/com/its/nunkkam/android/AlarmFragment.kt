package com.its.nunkkam.android

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.NumberPicker
import android.widget.Switch
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.its.nunkkam.android.databinding.FragmentAlarmBinding
import java.util.*

class AlarmFragment : Fragment() {

    // View Binding 객체
    private var _binding: FragmentAlarmBinding? = null
    private val binding get() = _binding!!

    // AlarmManager 객체
    private var alarmManager: AlarmManager? = null

    // SharedPreferences 객체
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        // View Binding 초기화
        _binding = FragmentAlarmBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // AlarmManager 초기화
        alarmManager = requireContext().getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // SharedPreferences 초기화
        sharedPreferences = requireContext().getSharedPreferences("AlarmPreferences", Context.MODE_PRIVATE)

        // 저장된 알람 값 로드
        loadAlarmValues()

        // 리스너 설정
        setupListeners()
    }

    // 리스너 설정
    private fun setupListeners() {
        // 측정 알람 버튼 클릭 리스너
        binding.btnMeasurementAlarm.setOnClickListener {
            binding.layoutMeasurementAlarm.visibility = View.VISIBLE
            binding.layoutManageAlarm.visibility = View.GONE
            binding.alarmTabTitle.text = "측정 알람 시간 설정"

            // 버튼 색상 변경
            binding.btnMeasurementAlarm.setBackgroundColor(Color.parseColor("#2E2E2E"))
            binding.btnManageAlarm.setBackgroundColor(Color.parseColor("#666666"))
        }

        // 관리 알람 버튼 클릭 리스너
        binding.btnManageAlarm.setOnClickListener {
            binding.layoutMeasurementAlarm.visibility = View.GONE
            binding.layoutManageAlarm.visibility = View.VISIBLE
            binding.alarmTabTitle.text = "관리 알람 주기 관리"

            // 버튼 색상 변경
            binding.btnManageAlarm.setBackgroundColor(Color.parseColor("#2E2E2E"))
            binding.btnMeasurementAlarm.setBackgroundColor(Color.parseColor("#666666"))
        }

        // 측정 알람 관련 리스너 설정
        setupMeasurementAlarmListeners()

        // 관리 알람 관련 리스너 설정
        setupManageAlarmListeners()
    }

    // 측정 알람 리스너 설정
    private fun setupMeasurementAlarmListeners() {
        // 측정 알람 켜기/끄기 스위치 리스너
        binding.switchMeasurementAlarm.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                Log.d("AlarmFragment", "측정 알람이 켜졌습니다.")
                // 알람 시간이 설정되어 있으면 알람을 설정
                val currentText = binding.btnMeasurementInterval.text.toString()
                val timeParts = currentText.split(":")
                if (timeParts.size == 2) {
                    val hour = timeParts[0].toInt()
                    val minute = timeParts[1].toInt()
                    setupDailyAlarm(hour, minute)
                    saveAlarmValues(hour, minute, isManageAlarm = false)
                }

                // 알람 시간 설정 버튼 리스너
                binding.btnMeasurementInterval.setOnClickListener {
                    showTimePickerDialog()
                }

                setSwitchColor(binding.switchMeasurementAlarm, R.color.white)
                saveSwitchColor(isManageAlarm = false, R.color.white)

            } else {
                Log.d("AlarmFragment", "측정 알람이 꺼졌습니다.")
                binding.btnMeasurementInterval.setOnClickListener(null)
                cancelAlarm(false)

                setSwitchColor(binding.switchMeasurementAlarm, R.color.dark_label)
                saveSwitchColor(isManageAlarm = false, R.color.dark_label)
            }
        }
    }

    // 관리 알람 리스너 설정
    private fun setupManageAlarmListeners() {
        // 관리 알람 켜기/끄기 스위치 리스너
        binding.switchManageAlarm.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                Log.d("AlarmFragment", "관리 알람이 켜졌습니다.")
                // 알람 주기가 설정되어 있으면 반복 알람 설정
                val intervalText = binding.btnManageInterval.text.toString()
                val parts = intervalText.split("시간 ", "분")
                if (parts.size == 2) {
                    val hours = parts[0].toInt()
                    val minutes = parts[1].toInt()
                    setupRepeatingAlarm(hours, minutes)
                    saveAlarmValues(hours, minutes, isManageAlarm = true)
                } else {
                    Toast.makeText(context, "알람 주기를 설정해주세요.", Toast.LENGTH_SHORT).show()
                }

                // 알람 주기 설정 버튼 리스너
                binding.btnManageInterval.setOnClickListener {
                    showIntervalPickerDialog()
                }

                setSwitchColor(binding.switchManageAlarm, R.color.white)
                saveSwitchColor(isManageAlarm = true, R.color.white)

            } else {
                Log.d("AlarmFragment", "관리 알람이 꺼졌습니다.")
                binding.btnManageInterval.setOnClickListener(null)
                cancelAlarm(true)

                setSwitchColor(binding.switchManageAlarm, R.color.dark_label)
                saveSwitchColor(isManageAlarm = true, R.color.dark_label)
            }
        }
    }

    // 스위치 색상 변경 함수
    private fun setSwitchColor(switch: Switch, colorResId: Int) {
        context?.let {
            switch.thumbTintList = ContextCompat.getColorStateList(it, colorResId)
        }
    }

    // 스위치 색상 저장 함수
    private fun saveSwitchColor(isManageAlarm: Boolean, colorResId: Int) {
        with(sharedPreferences.edit()) {
            if (isManageAlarm) {
                putInt("manageAlarmSwitchColor", colorResId)
            } else {
                putInt("measurementAlarmSwitchColor", colorResId)
            }
            apply()
        }
    }

    // 저장된 알람 설정 값 불러오기
    private fun loadAlarmValues() {
        // 측정 알람 설정 값 로드
        val measurementHour = sharedPreferences.getInt("measurementAlarmHour", 0)
        val measurementMinute = sharedPreferences.getInt("measurementAlarmMinute", 0)
        val measurementEnabled = sharedPreferences.getBoolean("measurementAlarmEnabled", false)
        val measurementSwitchColor = sharedPreferences.getInt("measurementAlarmSwitchColor", R.color.dark_label)

        binding.btnMeasurementInterval.text = String.format("%02d:%02d", measurementHour, measurementMinute)
        binding.switchMeasurementAlarm.isChecked = measurementEnabled
        setSwitchColor(binding.switchMeasurementAlarm, measurementSwitchColor)

        // 관리 알람 설정 값 로드
        val manageHours = sharedPreferences.getInt("manageAlarmHours", 0)
        val manageMinutes = sharedPreferences.getInt("manageAlarmMinutes", 0)
        val manageEnabled = sharedPreferences.getBoolean("manageAlarmEnabled", false)
        val manageSwitchColor = sharedPreferences.getInt("manageAlarmSwitchColor", R.color.dark_label)

        binding.btnManageInterval.text = "${manageHours}시간 ${manageMinutes}분 마다"
        binding.switchManageAlarm.isChecked = manageEnabled
        setSwitchColor(binding.switchManageAlarm, manageSwitchColor)

        // 알람이 활성화되어 있을 때 시간/주기 설정 버튼을 활성화
        if (measurementEnabled) {
            binding.btnMeasurementInterval.setOnClickListener {
                showTimePickerDialog()
            }
        }
        if (manageEnabled) {
            binding.btnManageInterval.setOnClickListener {
                showIntervalPickerDialog()
            }
        }
    }

    // 알람 취소
    private fun cancelAlarm(isManageAlarm: Boolean) {
        val intent = Intent(context, AlarmReceiver2::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context, if (isManageAlarm) 1002 else 1001, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager?.cancel(pendingIntent)
        Toast.makeText(context, "알람이 취소되었습니다.", Toast.LENGTH_SHORT).show()

        // 알람 설정 값 초기화
        saveAlarmValues(0, 0, isManageAlarm)
    }

    // 시간 선택 다이얼로그 표시
    private fun showTimePickerDialog() {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        val dialog = TimePickerDialog(
            requireContext(),
            R.style.CustomTimePickerDialog, // 커스텀 스타일 적용
            { _, selectedHour, selectedMinute ->
                binding.btnMeasurementInterval.text = String.format("%02d:%02d", selectedHour, selectedMinute)
                if (binding.switchMeasurementAlarm.isChecked) {
                    setupDailyAlarm(selectedHour, selectedMinute)
                    saveAlarmValues(selectedHour, selectedMinute, isManageAlarm = false)
                }
            },
            hour,
            minute,
            true
        )
        dialog.show()
    }

    // 주기 선택 다이얼로그 표시
    private fun showIntervalPickerDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_interval_picker, null)
        val numberPickerHours = dialogView.findViewById<NumberPicker>(R.id.numberPickerHours)
        val numberPickerMinutes = dialogView.findViewById<NumberPicker>(R.id.numberPickerMinutes)

        numberPickerHours.apply {
            minValue = 0
            maxValue = 23
        }
        numberPickerMinutes.apply {
            minValue = 0
            maxValue = 59
        }

        AlertDialog.Builder(requireContext())
            .setTitle("알람 주기 설정")
            .setView(dialogView)
            .setPositiveButton("확인") { _, _ ->
                val hours = numberPickerHours.value
                val minutes = numberPickerMinutes.value
                binding.btnManageInterval.text = "${hours}시간 ${minutes}분 마다"
                if (binding.switchManageAlarm.isChecked) {
                    setupRepeatingAlarm(hours, minutes)
                    saveAlarmValues(hours, minutes, isManageAlarm = true)
                }
            }
            .setNegativeButton("취소", null)
            .show()
    }

    // 정확한 알람 스케줄링 권한 확인
    @SuppressLint("NewApi")
    private fun canScheduleExactAlarms(): Boolean {
        return alarmManager?.canScheduleExactAlarms() == true
    }

    // 정확한 알람 스케줄링 권한 요청
    private fun requestExactAlarmPermission() {
        val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
        startActivity(intent)
    }

    // 일일 알람 설정
    private fun setupDailyAlarm(hour: Int = 0, minute: Int = 0) {
        // 정확한 알람 스케줄링 권한 확인 및 요청
        if (!canScheduleExactAlarms()) {
            requestExactAlarmPermission()
            Toast.makeText(context, "정확한 알람 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
            return
        }

        val intent = Intent(context, AlarmReceiver2::class.java).apply {
            putExtra("isManageAlarm", false)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, 1001, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val timeZone = TimeZone.getTimeZone("Asia/Seoul")
        val calendar: Calendar = Calendar.getInstance(timeZone).apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (before(Calendar.getInstance(timeZone))) {
                add(Calendar.DAY_OF_MONTH, 1)
            }
        }

        alarmManager?.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            pendingIntent
        )
    }

    // 반복 알람 설정
    private fun setupRepeatingAlarm(hours: Int = 0, minutes: Int = 0) {
        // 정확한 알람 스케줄링 권한 확인 및 요청
        if (!canScheduleExactAlarms()) {
            requestExactAlarmPermission()
            Toast.makeText(context, "정확한 알람 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
            return
        }

        val intervalMillis = (hours * 60 * 60 * 1000 + minutes * 60 * 1000).toLong()

        val intent = Intent(context, AlarmReceiver2::class.java).apply {
            putExtra("isManageAlarm", true)
            putExtra("intervalMillis", intervalMillis)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, 1002, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 첫 번째 알람 시간 설정
        val triggerTime = System.currentTimeMillis() + intervalMillis

        // 첫 번째 알람 설정
        alarmManager?.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerTime,
            pendingIntent
        )
    }

    // 알람 설정 값 저장
    private fun saveAlarmValues(hourOrHours: Int, minuteOrMinutes: Int, isManageAlarm: Boolean) {
        with(sharedPreferences.edit()) {
            if (isManageAlarm) {
                putInt("manageAlarmHours", hourOrHours)
                putInt("manageAlarmMinutes", minuteOrMinutes)
                putBoolean("manageAlarmEnabled", binding.switchManageAlarm.isChecked)
            } else {
                putInt("measurementAlarmHour", hourOrHours)
                putInt("measurementAlarmMinute", minuteOrMinutes)
                putBoolean("measurementAlarmEnabled", binding.switchMeasurementAlarm.isChecked)
            }
            apply()
        }
    }

    // View 해제
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
