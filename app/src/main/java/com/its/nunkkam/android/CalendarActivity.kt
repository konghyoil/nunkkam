package com.its.nunkkam.android // 패키지 선언: 이 코드가 속한 패키지를 지정

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.util.*

class CalendarActivity : AppCompatActivity() {

    private lateinit var calendar: Calendar  // 캘린더 인스턴스
    private lateinit var adapter: CalendarAdapter  // 어댑터 인스턴스
    private lateinit var recyclerView: RecyclerView  // 리사이클러뷰 인스턴스
    private lateinit var textViewMonthYear: TextView  // 월, 년 텍스트뷰 인스턴스

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calendar)

        // 초기화
        calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)  // 항상 현재 달의 첫 번째 날로 설정
        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = GridLayoutManager(this, 7)  // 7열 레이아웃으로 설정 (1주일 7일 기준)
        textViewMonthYear = findViewById(R.id.textView_month_year)

        // 버튼 초기화 및 클릭 리스너 설정
        val buttonPrevious: Button = findViewById(R.id.button_previous)
        val buttonNext: Button = findViewById(R.id.button_next)

        buttonPrevious.setOnClickListener {
            calendar.add(Calendar.MONTH, -1)
            calendar.set(Calendar.DAY_OF_MONTH, 1)  // 이전 달의 첫 번째 날로 설정
            updateCalendar()
        }

        buttonNext.setOnClickListener {
            calendar.add(Calendar.MONTH, 1)
            calendar.set(Calendar.DAY_OF_MONTH, 1)  // 다음 달의 첫 번째 날로 설정
            updateCalendar()
        }

        // 초기 달력 업데이트
        updateCalendar()
    }

    private fun updateCalendar() {
        // 빈 공간이 포함된 날짜 목록 생성
        val days = getDaysInMonthWithEmptySpaces()
        // 각 날짜에 대한 추가 정보 생성
        val infoList = getInfoForDays(days)

        // 어댑터 설정 또는 갱신
        adapter = CalendarAdapter(days, infoList)
        recyclerView.adapter = adapter

        // 텍스트뷰 업데이트
        updateMonthYearText()
    }

    // 빈 공간을 포함한 한 달의 날짜 목록을 반환
    private fun getDaysInMonthWithEmptySpaces(): List<Date?> {
        val tempCalendar = calendar.clone() as Calendar

        // Log the initial state of the calendar
        Log.d("CalendarDebug", "Initial Calendar: ${tempCalendar.time}")

        val daysInMonth = tempCalendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        val firstDayOfWeek = tempCalendar.get(Calendar.DAY_OF_WEEK) - 1  // 일요일 = 1, 월요일 = 2, ..., 토요일 = 7 (0부터 시작하도록 변환)

        val days = mutableListOf<Date?>()

        // 달의 첫날 전까지 빈 공간 추가
        for (i in 0 until firstDayOfWeek) {
            days.add(null)
        }

        // 실제 날짜 추가
        for (i in 0 until daysInMonth) {
            days.add(tempCalendar.time)
            // Log each day being added
            Log.d("CalendarDebug", "Adding Date: ${tempCalendar.time}")
            tempCalendar.add(Calendar.DAY_OF_MONTH, 1)
        }

        // 달이 변경되었으므로 다시 1일로 설정
        tempCalendar.set(Calendar.DAY_OF_MONTH, 1)

        return days
    }

    // 날짜에 대한 추가 랜덤 정보 생성-> 나중에 파이어 베이스 연동
    private fun getInfoForDays(days: List<Date?>): List<String?> {
        val random = Random()
        val today = Calendar.getInstance().time

        return days.map { date ->
            if (date != null && (date.before(today) || date.equals(today))) {
                val randomNumber = random.nextInt(30) + 1  // 1부터 30까지의 랜덤 숫자 생성
                "$randomNumber"
            } else {
                null
            }
        }
    }

    // 현재 월과 년도를 텍스트뷰에 업데이트
    private fun updateMonthYearText() {
        val monthYearFormat = java.text.SimpleDateFormat("yyyy MMMM", Locale.getDefault())
        textViewMonthYear.text = monthYearFormat.format(calendar.time)
    }
}