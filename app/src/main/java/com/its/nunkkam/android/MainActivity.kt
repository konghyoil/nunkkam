package com.its.nunkkam.android

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import android.widget.Button

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // BlinkActivity를 시작하는 버튼 설정
        val startBlinkActivityButton: Button = findViewById(R.id.startBlinkActivityButton)
        startBlinkActivityButton.setOnClickListener {
            startBlinkActivity()
        }

        // TimerFragment를 시작하는 버튼 설정
        val startTimerFragmentButton: Button = findViewById(R.id.startTimerActivityButton)
        startTimerFragmentButton.setOnClickListener {
            loadFragment(TimerFragment())
        }

        // CalendarFragment를 시작하는 버튼 설정
        val startCalendarFragmentButton: Button = findViewById(R.id.startCalendarActivityButton)
        startCalendarFragmentButton.setOnClickListener {
            loadFragment(CalendarFragment())
        }

        // ChartFragment를 시작하는 버튼 설정
        val startChartFragmentButton: Button = findViewById(R.id.startChartActivityButton)
        startChartFragmentButton.setOnClickListener {
            loadFragment(ChartFragment())
        }

        // CardFragment를 시작하는 버튼 설정
        val startCardFragmentButton: Button = findViewById(R.id.startCardActivityButton)
        startCardFragmentButton.setOnClickListener {
            val exampleRatePerMinute = 13
            val fragment = CardFragment()
            val bundle = Bundle()
            bundle.putInt("RATE_PER_MINUTE", exampleRatePerMinute)
            fragment.arguments = bundle
            loadFragment(fragment)
        }

        // ResultActivity를 시작하는 버튼 설정
        val startResultActivityButton: Button = findViewById(R.id.startResultActivityButton)
        startResultActivityButton.setOnClickListener {
            startResultActivity()
        }
    }

    private fun loadFragment(fragment: Fragment) {
        val fragmentManager: FragmentManager = supportFragmentManager
        val fragmentTransaction: FragmentTransaction = fragmentManager.beginTransaction()
        fragmentTransaction.replace(R.id.fragment_container, fragment)
        fragmentTransaction.addToBackStack(null)
        fragmentTransaction.commit()
    }

    // BlinkActivity를 시작하는 함수
    private fun startBlinkActivity() {
        val intent = Intent(this, BlinkActivity::class.java)
        startActivity(intent)
    }

    // ResultActivity를 시작하는 함수
    private fun startResultActivity() {
        val intent = Intent(this, ResultActivity::class.java)
        startActivity(intent)
    }
}
