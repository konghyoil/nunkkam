package com.its.nunkkam.android

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.NumberPicker
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.its.nunkkam.android.databinding.FragmentAlarmBinding
import com.its.nunkkam.android.databinding.FragmentAlarm2Binding
import java.util.*

class AlarmFragment : Fragment() {

    // View Binding 객체
    private var _binding: FragmentAlarmBinding? = null
    private var _binding2: FragmentAlarm2Binding? = null
    private val binding get() = _binding!!
    private val binding2 get() = _binding2!!

    private var alarmManager: AlarmManager? = null
    private var isManageAlarm = false

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d("AlarmFragment", "onCreateView: isManageAlarm1 = $isManageAlarm")

        super.onCreate(savedInstanceState)
        // Restore isManageAlarm state from savedInstanceState or arguments
        isManageAlarm = savedInstanceState?.getBoolean("isManageAlarm")
            ?: arguments?.getBoolean("isManageAlarm") ?: false
    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        Log.d("AlarmFragment", "onCreateView: isManageAlarm2 = $isManageAlarm")
        return if (isManageAlarm) {
            _binding2 = FragmentAlarm2Binding.inflate(inflater, container, false)
            binding2.root
        } else {
            _binding = FragmentAlarmBinding.inflate(inflater, container, false)
            binding.root
        }
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
        val btnMeasurementAlarm = if (isManageAlarm) binding2.tabLayout.btnMeasurementAlarm else binding.tabLayout.btnMeasurementAlarm
        btnMeasurementAlarm.setOnClickListener {
            Log.d("AlarmFragment", "Measurement alarm button clicked. isManageAlarm: $isManageAlarm")
            if (isManageAlarm) {
                isManageAlarm = false

            }
        }

        // 관리 알람 버튼 클릭 리스너
        val btnManageAlarm = if (isManageAlarm) binding2.tabLayout.btnManageAlarm else binding.tabLayout.btnManageAlarm
        btnManageAlarm.setOnClickListener {
            Log.d("AlarmFragment", "Manage alarm button clicked. isManageAlarm: $isManageAlarm")
            if (!isManageAlarm) {
                isManageAlarm = true

            }
        }

        if (isManageAlarm) {
            setupManageAlarmListeners()
        } else {
            setupMeasurementAlarmListeners()
        }
    }

    private fun setupMeasurementAlarmListeners() {
        // 알람 시간 설정 버튼 클릭 리스너
        binding.btnInterval.setOnClickListener {
            showTimePickerDialog()
        }

        // 알람 켜기/끄기 스위치 리스너
        binding.switchAlarm.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                setupDailyAlarm()
            } else {
                cancelAlarm(false)
            }
        }
    }

    private fun setupManageAlarmListeners() {
        // 알람 주기 설정 버튼 클릭 리스너
        binding2.btnInterval.setOnClickListener {
            showIntervalPickerDialog()
        }

        // 알람 켜기/끄기 스위치 리스너
        binding2.switchAlarm.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                setupRepeatingAlarm()
            } else {
                cancelAlarm(true)
            }
        }
    }

//    private fun updateAlarmView() {
//        Log.d("AlarmFragment", "Updating alarm view. isManageAlarm1: $isManageAlarm")
//        // Fragment를 교체하여 UI 전환
//        parentFragmentManager.beginTransaction()
//            .replace(R.id.alarm_fragment_container, AlarmFragment().apply {
//                arguments = Bundle().apply {
//                    putBoolean("isManageAlarm", isManageAlarm)
//                }
//            })
//            .commit()
//        Log.d("AlarmFragment", "Updating alarm view. isManageAlarm2: $isManageAlarm")
//
//    }


    private fun showTimePickerDialog() {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        TimePickerDialog(requireContext(), { _, selectedHour, selectedMinute ->
            binding.btnInterval.text = String.format("%02d:%02d", selectedHour, selectedMinute)
            if (binding.switchAlarm.isChecked) {
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
                binding2.btnInterval.text = "${hours}시간 ${minutes}분 마다"
                binding2.tvCurrentInterval.text = "현재 설정: ${hours}시간 ${minutes}분 마다"
                if (binding2.switchAlarm.isChecked) {
                    setupRepeatingAlarm(hours, minutes)
                }
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun setupDailyAlarm(hour: Int = 0, minute: Int = 0) {
        val intent = Intent(context, AlarmReceiver2::class.java).apply {
            putExtra("isManageAlarm", false)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar: Calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (before(Calendar.getInstance())) {
                add(Calendar.DAY_OF_MONTH, 1)
            }
        }

        alarmManager?.setRepeating(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )
    }

    private fun setupRepeatingAlarm(hours: Int = 0, minutes: Int = 0) {
        val intent = Intent(context, AlarmReceiver2::class.java).apply {
            putExtra("isManageAlarm", true)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, 1, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val intervalMillis = (hours * 60 * 60 * 1000 + minutes * 60 * 1000).toLong()

        alarmManager?.setRepeating(
            AlarmManager.RTC_WAKEUP,
            System.currentTimeMillis() + intervalMillis,
            intervalMillis,
            pendingIntent
        )
    }

    private fun cancelAlarm(isManageAlarm: Boolean) {
        val intent = Intent(context, AlarmReceiver2::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context, if (isManageAlarm) 1 else 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager?.cancel(pendingIntent)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("isManageAlarm", isManageAlarm)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        _binding2 = null
    }
}
