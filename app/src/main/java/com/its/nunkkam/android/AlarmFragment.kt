package com.its.nunkkam.android

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.NumberPicker
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.its.nunkkam.android.databinding.FragmentAlarmBinding
import android.widget.Toast
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
        Log.d("AlarmFragment", "onCreateView called")
        // View Binding 초기화
        _binding = FragmentAlarmBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("AlarmFragment", "onViewCreated called")

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
        Log.d("AlarmFragment", "setupListeners called")
        // 측정 알람 버튼 클릭 리스너
        binding.btnMeasurementAlarm.setOnClickListener {
            binding.layoutMeasurementAlarm.visibility = View.VISIBLE
            binding.layoutManageAlarm.visibility = View.GONE
            binding.alarmTabTitle.text = "측정 알람 주기 관리"
        }

        // 관리 알람 버튼 클릭 리스너
        binding.btnManageAlarm.setOnClickListener {
            binding.layoutMeasurementAlarm.visibility = View.GONE
            binding.layoutManageAlarm.visibility = View.VISIBLE
            binding.alarmTabTitle.text = "관리 알람 주기 관리"
        }

        setupMeasurementAlarmListeners()
        setupManageAlarmListeners()
    }

    // 측정 알람 리스너 설정
    private fun setupMeasurementAlarmListeners() {
        Log.d("AlarmFragment", "setupMeasurementAlarmListeners called")

        // 알람 켜기/끄기 스위치 리스너
        binding.switchMeasurementAlarm.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                binding.btnMeasurementInterval.setOnClickListener {
                    showTimePickerDialog()
                }
                val currentText = binding.btnMeasurementInterval.text.toString()
                val timeParts = currentText.split(":")
                if (timeParts.size == 2) {
                    val hour = timeParts[0].toInt()
                    val minute = timeParts[1].toInt()
                    setupDailyAlarm(hour, minute)
                    saveAlarmValues(hour, minute, isManageAlarm = false)
                } else {
                    Toast.makeText(context, "알람 시간을 설정해주세요.", Toast.LENGTH_SHORT).show()
                }
            } else {
                binding.btnMeasurementInterval.setOnClickListener(null)
                cancelAlarm(false)
            }
        }

        // 소리 스위치 리스너
        binding.switchMeasurementSound.setOnCheckedChangeListener { _, isChecked ->
            sharedPreferences.edit().putBoolean("measurementSoundEnabled", isChecked).apply()
            Toast.makeText(context, "측정 알람 소리 ${if (isChecked) "켜짐" else "꺼짐"}", Toast.LENGTH_SHORT).show()
        }

        // 진동 스위치 리스너
        binding.switchMeasurementVibration.setOnCheckedChangeListener { _, isChecked ->
            sharedPreferences.edit().putBoolean("measurementVibrationEnabled", isChecked).apply()
            Toast.makeText(context, "측정 알람 진동 ${if (isChecked) "켜짐" else "꺼짐"}", Toast.LENGTH_SHORT).show()
        }
    }

    // 관리 알람 리스너 설정
    private fun setupManageAlarmListeners() {
        Log.d("AlarmFragment", "setupManageAlarmListeners called")

        // 알람 켜기/끄기 스위치 리스너
        binding.switchManageAlarm.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                binding.btnManageInterval.setOnClickListener {
                    showIntervalPickerDialog()
                }
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
            } else {
                binding.btnManageInterval.setOnClickListener(null)
                cancelAlarm(true)
            }
        }

        // 소리 스위치 리스너
        binding.switchManageSound.setOnCheckedChangeListener { _, isChecked ->
            sharedPreferences.edit().putBoolean("manageSoundEnabled", isChecked).apply()
            Toast.makeText(context, "관리 알람 소리 ${if (isChecked) "켜짐" else "꺼짐"}", Toast.LENGTH_SHORT).show()
        }

        // 진동 스위치 리스너
        binding.switchManageVibration.setOnCheckedChangeListener { _, isChecked ->
            sharedPreferences.edit().putBoolean("manageVibrationEnabled", isChecked).apply()
            Toast.makeText(context, "관리 알람 진동 ${if (isChecked) "켜짐" else "꺼짐"}", Toast.LENGTH_SHORT).show()
        }
    }

    // 시간 선택 다이얼로그 표시
    private fun showTimePickerDialog() {
        Log.d("AlarmFragment", "showTimePickerDialog called")
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        TimePickerDialog(requireContext(), { _, selectedHour, selectedMinute ->
            binding.btnMeasurementInterval.text = String.format("%02d:%02d", selectedHour, selectedMinute)
            if (binding.switchMeasurementAlarm.isChecked) {
                setupDailyAlarm(selectedHour, selectedMinute)
                saveAlarmValues(selectedHour, selectedMinute, isManageAlarm = false)
            }
        }, hour, minute, true).show()
    }

    // 주기 선택 다이얼로그 표시
    private fun showIntervalPickerDialog() {
        Log.d("AlarmFragment", "showIntervalPickerDialog called")
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
        Log.d("AlarmFragment", "setupDailyAlarm called with hour: $hour, minute: $minute")

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
        Log.d("AlarmFragment","setupDailyAlarm: timezone = $timeZone")

        val now = Calendar.getInstance(timeZone)
        val diffInMillis = calendar.timeInMillis - now.timeInMillis
        val diffInHours = diffInMillis / (1000 * 60 * 60)
        val diffInMinutes = (diffInMillis / (1000 * 60)) % 60
        val diffInSeconds = (diffInMillis / 1000) % 60
        Log.d("AlarmFragment","setupDailyAlarm: now = $now, alarm set for ${calendar.time}")

        Toast.makeText(context, "알람이 ${diffInHours}시간 ${diffInMinutes}분 ${diffInSeconds}초 후에 울립니다.", Toast.LENGTH_LONG).show()

        alarmManager?.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            pendingIntent
        )
        Log.d("AlarmFragment", "setupDailyAlarm: repeating alarm set")
    }

    private fun setupRepeatingAlarm(hours: Int = 0, minutes: Int = 0) {
        Log.d("AlarmFragment", "setupRepeatingAlarm called with hours: $hours, minutes: $minutes")

        // 정확한 알람 스케줄링 권한 확인 및 요청
        if (!canScheduleExactAlarms()) {
            requestExactAlarmPermission()
            Toast.makeText(context, "정확한 알람 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
            return
        }

        val intervalMillis = (hours * 60 * 60 * 1000 + minutes * 60 * 1000).toLong()
        Log.d("AlarmFragment", "setupRepeatingAlarm: intervalMillis: ${intervalMillis / 60000}분")

        val intent = Intent(context, AlarmReceiver2::class.java).apply {
            putExtra("isManageAlarm", true)
            putExtra("intervalMillis", intervalMillis)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, 1002, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 첫 번째 알람 시간 설정
        val triggerTime = System.currentTimeMillis() + intervalMillis
        Log.d("AlarmFragment", "setupRepeatingAlarm: First alarm set for ${Date(triggerTime)}")

        // 첫 번째 알람 설정
        alarmManager?.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerTime,
            pendingIntent
        )
        Log.d("AlarmFragment", "setupRepeatingAlarm: First exact alarm set")
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

    // 저장된 알람 설정 값 불러오기
    private fun loadAlarmValues() {
        val measurementHour = sharedPreferences.getInt("measurementAlarmHour", 0)
        val measurementMinute = sharedPreferences.getInt("measurementAlarmMinute", 0)
        val measurementEnabled = sharedPreferences.getBoolean("measurementAlarmEnabled", false)
        val measurementSoundEnabled = sharedPreferences.getBoolean("measurementSoundEnabled", true)
        val measurementVibrationEnabled = sharedPreferences.getBoolean("measurementVibrationEnabled", true)

        binding.btnMeasurementInterval.text = String.format("%02d:%02d", measurementHour, measurementMinute)
        binding.switchMeasurementAlarm.isChecked = measurementEnabled
        binding.switchMeasurementSound.isChecked = measurementSoundEnabled
        binding.switchMeasurementVibration.isChecked = measurementVibrationEnabled
        if (measurementEnabled) {
            binding.btnMeasurementInterval.setOnClickListener {
                showTimePickerDialog()
            }
        }

        val manageHours = sharedPreferences.getInt("manageAlarmHours", 0)
        val manageMinutes = sharedPreferences.getInt("manageAlarmMinutes", 0)
        val manageEnabled = sharedPreferences.getBoolean("manageAlarmEnabled", false)
        val manageSoundEnabled = sharedPreferences.getBoolean("manageSoundEnabled", true)
        val manageVibrationEnabled = sharedPreferences.getBoolean("manageVibrationEnabled", true)

        binding.btnManageInterval.text = "${manageHours}시간 ${manageMinutes}분 마다"
        binding.switchManageAlarm.isChecked = manageEnabled
        binding.switchManageSound.isChecked = manageSoundEnabled
        binding.switchManageVibration.isChecked = manageVibrationEnabled
        if (manageEnabled) {
            binding.btnManageInterval.setOnClickListener {
                showIntervalPickerDialog()
            }
        }
    }

    // 알람 취소
    private fun cancelAlarm(isManageAlarm: Boolean) {
        Log.d("AlarmFragment", "cancelAlarm called for isManageAlarm: $isManageAlarm")
        val intent = Intent(context, AlarmReceiver2::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context, if (isManageAlarm) 1002 else 1001, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager?.cancel(pendingIntent)
        Toast.makeText(context, "알림off", Toast.LENGTH_SHORT).show()
        Log.d("AlarmFragment", "cancelAlarm: alarm cancelled")

        // 알람 설정 값 저장
        saveAlarmValues(0, 0, isManageAlarm)
    }

    // View 해제
    override fun onDestroyView() {
        super.onDestroyView()
        Log.d("AlarmFragment", "onDestroyView called")
        _binding = null
    }
}
