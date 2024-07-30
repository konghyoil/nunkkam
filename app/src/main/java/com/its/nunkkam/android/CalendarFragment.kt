package com.its.nunkkam.android

import android.content.Context
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

    // 필요한 변수들을 선언합니다.
    private lateinit var calendar: Calendar
    private lateinit var adapter: CalendarAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var textViewMonthYear: TextView
    private val db = Firebase.firestore  // Firebase Firestore 인스턴스를 초기화합니다.

    private var blinksData: List<Map<String, Any>> = emptyList()  // 눈 깜빡임 데이터를 저장할 리스트

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
        val isGoogleLogin = sharedPreferences.getBoolean("is_google_login", false)

        if (userId != null) {
            fetchUserData(userId)
        } else {
            Log.e("CalendarFragment", "User ID is null")
        }

        // 캘린더 및 UI 요소들을 초기화합니다.
        calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        recyclerView = view.findViewById(R.id.recyclerView)
        recyclerView.layoutManager = GridLayoutManager(context, 7)  // 7열의 그리드 레이아웃을 사용합니다.
        textViewMonthYear = view.findViewById(R.id.textView_month_year)

        // 버튼 초기화 및 클릭 리스너 설정
        val buttonPrevious: ImageButton = view.findViewById(R.id.button_previous)
        val buttonNext: ImageButton = view.findViewById(R.id.button_next)

        // 이전 달 버튼 클릭 리스너
        buttonPrevious.setOnClickListener {
            calendar.add(Calendar.MONTH, -1)
            calendar.set(Calendar.DAY_OF_MONTH, 1)
            updateCalendar()
        }

        // 다음 달 버튼 클릭 리스너
        buttonNext.setOnClickListener {
            calendar.add(Calendar.MONTH, 1)
            calendar.set(Calendar.DAY_OF_MONTH, 1)
            updateCalendar()
        }

        updateCalendar()  // 초기 캘린더를 표시합니다.
    }

    // Firestore에서 사용자 데이터를 가져오는 함수
    private fun fetchUserData(userId: String) {
        val TAG = "Calendar_view_data"
        val docRef = db.collection("USERS").document(userId)

        // Firestore에서 사용자 데이터를 가져옵니다.
        docRef.get().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val document = task.result
                if (document != null) {
                    blinksData = document.data?.get("blinks") as List<Map<String, Any>>? ?: emptyList()
                    Log.d(TAG, "Cached document data: ${document.data}")
                    updateCalendar()  // 데이터를 가져온 후 캘린더를 업데이트합니다.
                }
            } else {
                Log.d(TAG, "Cached get failed: ", task.exception)
            }
        }
    }

    // 캘린더를 업데이트하는 함수
    private fun updateCalendar() {
        val days = getDaysInMonthWithEmptySpaces()  // 현재 월의 일자들을 가져옵니다.
        val infoList = getInfoForDays(days)  // 각 일자에 해당하는 정보를 가져옵니다.

        adapter = CalendarAdapter(days, infoList)  // 어댑터를 생성합니다.
        recyclerView.adapter = adapter  // RecyclerView에 어댑터를 설정합니다.

        updateMonthYearText()  // 월/년 텍스트를 업데이트합니다.
    }

    // 현재 월의 일자들과 이전 월의 빈 공간을 포함한 리스트를 반환하는 함수
    private fun getDaysInMonthWithEmptySpaces(): List<Date?> {
        val tempCalendar = calendar.clone() as Calendar

        val daysInMonth = tempCalendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        val firstDayOfWeek = tempCalendar.get(Calendar.DAY_OF_WEEK) - 1

        val days = mutableListOf<Date?>()

        // 이전 월의 빈 공간을 추가합니다.
        for (i in 0 until firstDayOfWeek) {
            days.add(null)
        }

        // 현재 월의 일자들을 추가합니다.
        for (i in 0 until daysInMonth) {
            days.add(tempCalendar.time)
            tempCalendar.add(Calendar.DAY_OF_MONTH, 1)
        }

        tempCalendar.set(Calendar.DAY_OF_MONTH, 1)

        return days
    }

    // 각 일자에 해당하는 정보(평균 눈 깜빡임 횟수)를 가져오는 함수
    private fun getInfoForDays(days: List<Date?>): List<String?> {
        val infoList = mutableListOf<String?>()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        // blinksData를 날짜별로 매핑합니다.
        val blinksMap = blinksData.groupBy {
            dateFormat.format((it["measurement_date"] as com.google.firebase.Timestamp).toDate())
        }.mapValues { entry ->
            entry.value.map { it["average_frequency_per_minute"] as Double }.average()
        }
        Log.d("TAG", "blinksMap = $blinksMap")

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

    // 월/년 텍스트를 업데이트하는 함수
    private fun updateMonthYearText() {
        val monthYearFormat = SimpleDateFormat("yyyy. MM", Locale.getDefault())
        textViewMonthYear.text = monthYearFormat.format(calendar.time)
    }
}
