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

        setupRecyclerView()
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

        val context = requireContext()
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_day_info, null)
        val tvDialogDate = dialogView.findViewById<TextView>(R.id.tv_dialog_date)
        val tvDialogInfo = dialogView.findViewById<TextView>(R.id.tv_dialog_info)

        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        tvDialogDate.text = dateFormat.format(date)

        val blinksForDate = blinksData.filter {
            dateFormat.format((it["measurement_date"] as com.google.firebase.Timestamp).toDate()) == dateFormat.format(date)
        }

        val infoText = blinksForDate.joinToString("\n") {
            "Frequency: ${it["average_frequency_per_minute"]} per minute"
        }

        tvDialogInfo.text = if (infoText.isNotEmpty()) infoText else "No information available"

        AlertDialog.Builder(context)
            .setView(dialogView)
            .setPositiveButton("OK", null)
            .show()
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

        val blinksMap = blinksData.groupBy {
            dateFormat.format((it["measurement_date"] as com.google.firebase.Timestamp).toDate())
        }.mapValues { entry ->
            entry.value.map { it["average_frequency_per_minute"] as Double }.average()
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

        return infoList
    }

    private fun updateMonthYearText() {
        val monthYearFormat = SimpleDateFormat("yyyy. MM", Locale.getDefault())
        textViewMonthYear.text = monthYearFormat.format(calendar.time)
    }
}