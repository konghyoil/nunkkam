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
import android.widget.NumberPicker
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.its.nunkkam.android.databinding.FragmentAlarmBinding
import java.util.*

class AlarmFragment : Fragment() {

    // View Binding 객체 선언
    private var _binding: FragmentAlarmBinding? = null
    private val binding get() = _binding!!

    private var alarmManager: AlarmManager? = null
    private var pendingIntent: PendingIntent? = null

    private var isManageAlarm = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // View Binding을 사용하여 레이아웃 인플레이트
        _binding = FragmentAlarmBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // AlarmManager 초기화
        alarmManager = requireContext().getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // 버튼 및 스위치 리스너 설정
        setupListeners()

        // 초기 뷰 설정
        updateAlarmView()

        // 알림 채널 생성
        createNotificationChannel()
    }
//
    private fun setupListeners() {
        // 알람 간격 설정 버튼 클릭 리스너
        binding.btnInterval.setOnClickListener {
            showIntervalPickerDialog()
        }

        // 알람 활성화/비활성화 스위치 리스너
        binding.switchAlarm.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                setAlarm()
            } else {
                cancelAlarm()
            }
        }

        // 알림음 설정 스위치 리스너
        binding.switchSound.setOnCheckedChangeListener { _, isChecked ->
            saveSoundSetting(isChecked)
        }

        // 진동 설정 스위치 리스너
        binding.switchVibration.setOnCheckedChangeListener { _, isChecked ->
            saveVibrationSetting(isChecked)
        }

        // 측정 알람 버튼 클릭 리스너
        binding.btnMeasurementAlarm.setOnClickListener {
            isManageAlarm = false
            updateAlarmView()
        }

        // 관리 알람 버튼 클릭 리스너
        binding.btnManageAlarm.setOnClickListener {
            isManageAlarm = true
            updateAlarmView()
        }
    }

    // 알람 간격 선택 다이얼로그 표시
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
                binding.btnInterval.text = intervalText
                if (binding.switchAlarm.isChecked) {
                    setAlarm()
                }
            }
            .setNegativeButton("취소", null)
            .show()
    }

    // 알람 설정
    private fun setAlarm() {
        val intervalText = binding.btnInterval.text.toString()
        val intervalParts = intervalText.split(" ")
        val hours = intervalParts[0].replace("시간", "").toInt()
        val minutes = intervalParts[1].replace("분", "").toInt()
        val intervalMillis = (hours * 3600 + minutes * 60) * 1000L

        val intent = Intent(context, AlarmReceiver2::class.java).apply {
            putExtra("isManageAlarm", isManageAlarm)
        }
        pendingIntent = PendingIntent.getBroadcast(
            context,
            if (isManageAlarm) 1 else 0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerTime = System.currentTimeMillis() + intervalMillis

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager?.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
        } else {
            alarmManager?.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
        }
    }

    // 알람 취소
    private fun cancelAlarm() {
        pendingIntent?.let { alarmManager?.cancel(it) }
    }

    // 알림음 설정 저장
    private fun saveSoundSetting(isEnabled: Boolean) {
        val sharedPrefs = requireContext().getSharedPreferences("AlarmSettings", Context.MODE_PRIVATE)
        sharedPrefs.edit().putBoolean("sound_enabled", isEnabled).apply()
    }

    // 진동 설정 저장
    private fun saveVibrationSetting(isEnabled: Boolean) {
        val sharedPrefs = requireContext().getSharedPreferences("AlarmSettings", Context.MODE_PRIVATE)
        sharedPrefs.edit().putBoolean("vibration_enabled", isEnabled).apply()
    }

    // 알람 뷰 업데이트 (측정 알람 / 관리 알람)
    private fun updateAlarmView() {
        if (isManageAlarm) {
            binding.alarmSettingsLayout.visibility = View.GONE
            binding.manageAlarmSettingsLayout.visibility = View.VISIBLE
            binding.btnManageAlarm.setBackgroundResource(R.drawable.button_selected)
            binding.btnMeasurementAlarm.setBackgroundResource(R.drawable.button_unselected)
        } else {
            binding.alarmSettingsLayout.visibility = View.VISIBLE
            binding.manageAlarmSettingsLayout.visibility = View.GONE
            binding.btnMeasurementAlarm.setBackgroundResource(R.drawable.button_selected)
            binding.btnManageAlarm.setBackgroundResource(R.drawable.button_unselected)
        }
    }

    // 알림 채널 생성 (Android 8.0 이상에서 필요)
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}