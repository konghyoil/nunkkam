package com.its.nunkkam.android

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val startBlinkActivityButton: Button = findViewById(R.id.startBlinkActivityButton)
        startBlinkActivityButton.setOnClickListener {
            startBlinkActivity()
        }

        val startChartActivityButton: Button = findViewById(R.id.startChartActivityButton)
        startChartActivityButton.setOnClickListener {
            startChartActivity()
        }
    }

    private fun startBlinkActivity() {
        val intent = Intent(this, BlinkActivity::class.java)
        startActivity(intent)
    }

    private fun startChartActivity() {
        val intent = Intent(this, ChartActivity::class.java)
        startActivity(intent)
    }
}