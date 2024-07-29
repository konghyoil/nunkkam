package com.its.nunkkam.android

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class TimerActivity : AppCompatActivity() {

    private lateinit var startButton: Button
    private lateinit var resultButton: Button
    private lateinit var timerTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_timer)

        startButton = findViewById(R.id.start_button)
        resultButton = findViewById(R.id.result_button)
        timerTextView = findViewById(R.id.timer_text)


        startButton.setOnClickListener {
            val intent = Intent(this, BlinkActivity::class.java)
            startActivity(intent)
        }

        resultButton.setOnClickListener {
            goToResultScreen()
        }
    }


    private fun goToResultScreen() {
        val intent = Intent(this, ResultActivity::class.java)
        startActivity(intent)
    }
}
