package com.its.nunkkam.android

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment

class ResultActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_result)

        val ratePerMinute = intent.getIntExtra("RATE_PER_MINUTE", 0)

        // 각 Fragment를 생성하고 순서대로 추가합니다.
        // CardFragment 생성 및 추가
        val cardFragment = CardFragment.newInstance(ratePerMinute)
        val chartFragment = ChartFragment()
        val calendarFragment = CalendarFragment()

        // FragmentTransaction을 통해 Fragment들을 추가
        supportFragmentManager.beginTransaction().apply {
            add(R.id.cardContainer, cardFragment) // CardFragment를 cardContainer에 추가
            add(R.id.chartContainer, chartFragment) // ChartFragment를 chartContainer에 추가
            add(R.id.calendarContainer, calendarFragment) // CalendarFragment를 calendarContainer에 추가
        }.commit()
    }
}
