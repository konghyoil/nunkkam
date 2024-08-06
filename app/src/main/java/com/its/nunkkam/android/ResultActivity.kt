package com.its.nunkkam.android

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment

class ResultActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_result)

        // 툴바 설정
        val toolbar: androidx.appcompat.widget.Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        // 수정: 앱 타이틀 제거
        supportActionBar?.setDisplayShowTitleEnabled(false)

        // tips 버튼 클릭 리스너 설정
        val tipsButton: TextView = findViewById(R.id.tipsButton)
        tipsButton.setOnClickListener {
            showPopup()
        }

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

    // 수정: showPopup 메서드 추가
    private fun showPopup() {
        val popupFragment = PopupFragment.newInstance()
        popupFragment.show(supportFragmentManager, "popup")
    }
}