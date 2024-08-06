package com.its.nunkkam.android

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        // View Binding 초기화
        _binding = FragmentAlarmBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // AlarmManager 초기화
        alarmManager = requireContext().getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // 리스너 설정
        setupListeners()
    }

    private fun setupListeners() {
        // 측정 알람 버튼 클릭 리스너
        binding.btnMeasurementAlarm.setOnClickListener {
            binding.layoutMeasurementAlarm.visibility = View.VISIBLE
            binding.layoutManageAlarm.visibility = View.GONE
        }

        // 관리 알람 버튼 클릭 리스너
        binding.btnManageAlarm.setOnClickListener {
            binding.layoutMeasurementAlarm.visibility = View.GONE
            binding.layoutManageAlarm.visibility = View.VISIBLE
        }

        setupMeasurementAlarmListeners()
        setupManageAlarmListeners()
    }

    private fun setupMeasurementAlarmListeners() {
        // 알람 시간 설정 버튼 클릭 리스너
        binding.btnMeasurementInterval.setOnClickListener {
            showTimePickerDialog()
        }

        // 알람 켜기/끄기 스위치 리스너
        binding.switchMeasurementAlarm.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                val currentText = binding.btnMeasurementInterval.text.toString()
                val timeParts = currentText.split(":")
                if (timeParts.size == 2) {
                    val hour = timeParts[0].toInt()
                    val minute = timeParts[1].toInt()
                    setupDailyAlarm(hour, minute)
                } else {
                    Toast.makeText(context, "알람 시간을 설정해주세요.", Toast.LENGTH_SHORT).show()
                }
            } else {
                cancelAlarm(false)
                Toast.makeText(context, "알림off", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupManageAlarmListeners() {
        // 알람 주기 설정 버튼 클릭 리스너
        binding.btnManageInterval.setOnClickListener {
            showIntervalPickerDialog()
        }

        // 알람 켜기/끄기 스위치 리스너
        binding.switchManageAlarm.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                val intervalText = binding.btnManageInterval.text.toString()
                val parts = intervalText.split("시간 ", "분")
                if (parts.size == 2) {
                    val hours = parts[0].toInt()
                    val minutes = parts[1].toInt()
                    setupRepeatingAlarm(hours, minutes)
                } else {
                    Toast.makeText(context, "알람 주기를 설정해주세요.", Toast.LENGTH_SHORT).show()
                }
            } else {
                cancelAlarm(true)
                Toast.makeText(context, "알림off", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showTimePickerDialog() {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        TimePickerDialog(requireContext(), { _, selectedHour, selectedMinute ->
            binding.btnMeasurementInterval.text = String.format("%02d:%02d", selectedHour, selectedMinute)
            if (binding.switchMeasurementAlarm.isChecked) {
                setupDailyAlarm(selectedHour, selectedMinute)
            }
        }, hour, minute, true).show()
    }

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
                binding.tvCurrentInterval.text = "현재 설정: ${hours}시간 ${minutes}분 마다"
                if (binding.switchManageAlarm.isChecked) {
                    setupRepeatingAlarm(hours, minutes)
                }
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun setupDailyAlarm(hour: Int = 0, minute: Int = 0) {
        // 일일 알람 설정을 위한 인텐트 생성
        val intent = Intent(context, AlarmReceiver2::class.java).apply {
            putExtra("isManageAlarm", false)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, 1001, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // KTC 시간 기준으로 알람 시간 설정
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
        Log.d("alarmfragment","setupDailyAlarm : hour: ${hour}시, minute: ${minute}")
        // 현재 시간과 알람 시간의 차이 계산
        val now = Calendar.getInstance(timeZone)
        val diffInMillis = calendar.timeInMillis - now.timeInMillis
        val diffInHours = diffInMillis / (1000 * 60 * 60)
        val diffInMinutes = (diffInMillis / (1000 * 60)) % 60
        val diffInSeconds = (diffInMillis / 1000) % 60

        // Toast 메시지로 알림
        Toast.makeText(context, "알람이 ${diffInHours}시간 ${diffInMinutes}분 ${diffInSeconds}초 후에 울립니다.", Toast.LENGTH_LONG).show()

        // 일일 반복 알람 설정 (KTC 기준)
        alarmManager?.setRepeating(
            AlarmManager.ELAPSED_REALTIME_WAKEUP,
            SystemClock.elapsedRealtime() + diffInMillis,
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )
    }

    private fun setupRepeatingAlarm(hours: Int = 0, minutes: Int = 0) {
        // 반복 알람 설정을 위한 인텐트 생성
        val intent = Intent(context, AlarmReceiver2::class.java).apply {
            putExtra("isManageAlarm", true)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, 1002, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 알람 간격 계산 (밀리초 단위)
        val intervalMillis = (hours * 60 * 60 * 1000 + minutes * 60 * 1000).toLong()
        Log.d("alarmfragment","setupRepeatingAlarm intervalMillis: ${intervalMillis/60000}")
        // 반복 알람 설정 (로컬 시간 기준)
        alarmManager?.setRepeating(
            AlarmManager.RTC_WAKEUP,
            System.currentTimeMillis() + intervalMillis,
            intervalMillis,
            pendingIntent
        )
    }

    private fun cancelAlarm(isManageAlarm: Boolean) {
        // 알람 취소를 위한 인텐트 생성
        val intent = Intent(context, AlarmReceiver2::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context, if (isManageAlarm) 1002 else 1001, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        // 알람 취소
        alarmManager?.cancel(pendingIntent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // View Binding 해제
        _binding = null
    }
}
