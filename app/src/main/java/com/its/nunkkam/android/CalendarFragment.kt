package com.its.nunkkam.android

import android.app.AlertDialog
import android.content.Context
import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.*
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import androidx.core.content.ContextCompat

class CalendarFragment : Fragment() {

    private lateinit var calendar: Calendar
    private lateinit var adapter: CalendarAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var textViewMonthYear: TextView
    private val db = Firebase.firestore

    private var blinksData: List<Map<String, Any>> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.d("Fragment", "CalendarFragment onCreateView called")
        return inflater.inflate(R.layout.fragment_calendar, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val sharedPreferences = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        val userId = sharedPreferences.getString("user_id", null)

        if (userId != null) {
            fetchUserData(userId)
        } else {
            Log.e("CalendarFragment", "User ID is null")
        }

        calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        recyclerView = view.findViewById(R.id.recyclerView)
        textViewMonthYear = view.findViewById(R.id.textView_month_year)

        val buttonPrevious: ImageButton = view.findViewById(R.id.button_previous)
        val buttonNext: ImageButton = view.findViewById(R.id.button_next)

        buttonPrevious.contentDescription = "이전 달"
        buttonNext.contentDescription = "다음 달"

        buttonPrevious.setOnClickListener {
            calendar.add(Calendar.MONTH, -1)
            updateCalendar()
        }

        buttonNext.setOnClickListener {
            calendar.add(Calendar.MONTH, 1)
            updateCalendar()
        }

        // Setup the RecyclerView
        setupRecyclerView()

        // Set the legend text with different colors
        val legendTextView: TextView = view.findViewById(R.id.textView_legend)
        val spannableString = SpannableString("    1. 별로  2. 좋음")

// "1. 별로" 부분에 색상 적용
        val colorDarkBar = ContextCompat.getColor(requireContext(), R.color.dark_darktext)
        spannableString.setSpan(
            ForegroundColorSpan(colorDarkBar),
            4, 9,  // "    1. 별로"에서 "1. 별로"에 해당하는 인덱스 범위
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )

// "2. 좋음" 부분에 색상 적용
        val colorLightText = ContextCompat.getColor(requireContext(), R.color.dark_lighttext)
        spannableString.setSpan(
            ForegroundColorSpan(colorLightText),
            11, 16,  // "    1. 별로  2. 좋음"에서 "2. 좋음"에 해당하는 인덱스 범위
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )

//// "3. 에러" 부분에 색상 적용
//        val colorDark = ContextCompat.getColor(requireContext(), R.color.dark_dark)
//        spannableString.setSpan(
//            ForegroundColorSpan(colorDark),
//            14, spannableString.length,  // 인덱스 범위 수정: "3. "부터 끝까지 포함
//            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
//        )

// 최종적으로 텍스트에 적용
        legendTextView.text = spannableString
    }


    private fun setupRecyclerView() {
        val spanCount = 7
        val layoutManager = GridLayoutManager(context, spanCount)
        recyclerView.layoutManager = layoutManager

        recyclerView.post {
            val recyclerViewHeight = recyclerView.height
            val itemHeight = recyclerViewHeight / 6 // 6주로 가정
            val itemWidth = recyclerView.width / spanCount

            adapter = CalendarAdapter(emptyList(), emptyList(), itemWidth, itemHeight) { date, info ->
                showDialog(date)
            }
            recyclerView.adapter = adapter

            updateCalendar()
        }
    }

    private fun fetchUserData(userId: String) {
        val TAG = "Calendar_view_data"
        val docRef = db.collection("USERS").document(userId)

        docRef.get().addOnCompleteListener { task ->
            try {
                if (task.isSuccessful) {
                    val document = task.result
                    if (document != null && document.exists()) {
                        blinksData = document.data?.get("blinks") as? List<Map<String, Any>> ?: emptyList()
                        Log.d(TAG, "Cached document data: ${document.data}")
                        updateCalendar()
                    } else {
                        Log.d(TAG, "No such document")
                    }
                } else {
                    Log.d(TAG, "Cached get failed: ", task.exception)
                }
            } catch (e: ClassCastException) {
                Log.e(TAG, "Error casting data: ", e)
                blinksData = emptyList()
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error: ", e)
                blinksData = emptyList()
            }
        }
    }

    private fun updateCalendar() {
        val days = getDaysInMonthWithEmptySpaces()
        val infoList = getInfoForDays(days)

        adapter.updateData(days, infoList)
        updateMonthYearText()

        Log.d("CalendarFragment", "Calendar updated with ${days.size} days and ${infoList.size} info items")
    }

    private fun showDialog(date: Date?) {
        if (date == null) return

        try {
            val context = requireContext()
            val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_day_info, null)
            val tvDialogDate = dialogView.findViewById<TextView>(R.id.tv_dialog_date)
            val tvDialogInfo = dialogView.findViewById<TextView>(R.id.tv_dialog_info)

            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            tvDialogDate.text = dateFormat.format(date)

            val blinksForDate = blinksData.filter {
                dateFormat.format((it["measurement_date"] as? com.google.firebase.Timestamp)?.toDate() ?: Date()) == dateFormat.format(date)
            }

            val infoText = blinksForDate.joinToString("\n") {
                "Frequency: ${it["average_frequency_per_minute"]} per minute"
            }

            tvDialogInfo.text = if (infoText.isNotEmpty()) infoText else "No information available"

            AlertDialog.Builder(context)
                .setView(dialogView)
                .setPositiveButton("OK", null)
                .show()
        } catch (e: IllegalArgumentException) {
            Log.e("CalendarFragment", "Error creating dialog: ", e)
        } catch (e: Exception) {
            Log.e("CalendarFragment", "Unexpected error in showDialog: ", e)
        }
    }

    private fun getDaysInMonthWithEmptySpaces(): List<Date?> {
        val tempCalendar = calendar.clone() as Calendar
        tempCalendar.set(Calendar.DAY_OF_MONTH, 1)

        val daysInMonth = tempCalendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        val firstDayOfWeek = tempCalendar.get(Calendar.DAY_OF_WEEK) - 1

        val days = mutableListOf<Date?>()

        for (i in 0 until firstDayOfWeek) {
            days.add(null)
        }

        for (i in 1..daysInMonth) {
            tempCalendar.set(Calendar.DAY_OF_MONTH, i)
            days.add(tempCalendar.time)
        }

        Log.d("CalendarFragment", "Generated days: ${days.size}, First day: ${days.firstOrNull()}, Last day: ${days.lastOrNull()}")

        return days
    }

    private fun getInfoForDays(days: List<Date?>): List<String?> {
        val infoList = mutableListOf<String?>()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        try {
            val blinksMap = blinksData.groupBy {
                dateFormat.format((it["measurement_date"] as? com.google.firebase.Timestamp)?.toDate() ?: Date())
            }.mapValues { entry ->
                entry.value.mapNotNull { it["average_frequency_per_minute"] as? Double }.average()
            }

            for (date in days) {
                if (date != null) {
                    val dateString = dateFormat.format(date)
                    val averageFrequency = blinksMap[dateString]?.toString()
                    infoList.add(averageFrequency)
                } else {
                    infoList.add(null)
                }
            }
        } catch (e: ClassCastException) {
            Log.e("CalendarFragment", "Error casting data in getInfoForDays: ", e)
        } catch (e: NullPointerException) {
            Log.e("CalendarFragment", "Null pointer in getInfoForDays: ", e)
        } catch (e: Exception) {
            Log.e("CalendarFragment", "Unexpected error in getInfoForDays: ", e)
        }

        return infoList
    }

    private fun updateMonthYearText() {
        val monthYearFormat = SimpleDateFormat("yyyy. MM", Locale.getDefault())
        textViewMonthYear.text = monthYearFormat.format(calendar.time)
    }
}