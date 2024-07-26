package com.its.nunkkam.android

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.*

class CalendarActivity : AppCompatActivity() {

    private lateinit var calendar: Calendar
    private lateinit var adapter: CalendarAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var textViewMonthYear: TextView
    private val db = Firebase.firestore

    private var blinksData: List<Map<String, Any>> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calendar)

        val user_id = "user1234"
        val TAG = "Calendar_view_data"
        val docRef = db.collection("USERS").document(user_id)

        docRef.get().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val document = task.result
                if (document != null) {
                    blinksData = document.data?.get("blinks") as List<Map<String, Any>>? ?: emptyList()
                    Log.d(TAG, "Cached document data: ${document.data}")
                    updateCalendar()
                }
            } else {
                Log.d(TAG, "Cached get failed: ", task.exception)
            }
        }

        calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = GridLayoutManager(this, 7)
        textViewMonthYear = findViewById(R.id.textView_month_year)

        val buttonPrevious: Button = findViewById(R.id.button_previous)
        val buttonNext: Button = findViewById(R.id.button_next)

        buttonPrevious.setOnClickListener {
            calendar.add(Calendar.MONTH, -1)
            calendar.set(Calendar.DAY_OF_MONTH, 1)
            updateCalendar()
        }

        buttonNext.setOnClickListener {
            calendar.add(Calendar.MONTH, 1)
            calendar.set(Calendar.DAY_OF_MONTH, 1)
            updateCalendar()
        }

        updateCalendar()
    }

    private fun updateCalendar() {
        val days = getDaysInMonthWithEmptySpaces()
        val infoList = getInfoForDays(days)

        adapter = CalendarAdapter(days, infoList)
        recyclerView.adapter = adapter

        updateMonthYearText()
    }

    private fun getDaysInMonthWithEmptySpaces(): List<Date?> {
        val tempCalendar = calendar.clone() as Calendar

        val daysInMonth = tempCalendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        val firstDayOfWeek = tempCalendar.get(Calendar.DAY_OF_WEEK) - 1

        val days = mutableListOf<Date?>()

        for (i in 0 until firstDayOfWeek) {
            days.add(null)
        }

        for (i in 0 until daysInMonth) {
            days.add(tempCalendar.time)
            tempCalendar.add(Calendar.DAY_OF_MONTH, 1)
        }

        tempCalendar.set(Calendar.DAY_OF_MONTH, 1)

        return days
    }

    private fun getInfoForDays(days: List<Date?>): List<String?> {
        val infoList = mutableListOf<String?>()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        val blinksMap = blinksData.associateBy {
            dateFormat.format((it["measurement_date"] as com.google.firebase.Timestamp).toDate())
        }
        Log.d("TAG","blinksMap = $blinksMap")

        for (date in days) {
            if (date != null) {
                val dateString = dateFormat.format(date)
                val blinkData = blinksMap[dateString]
                val averageFrequency = blinkData?.get("average_frequency_per_minute")?.toString()
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