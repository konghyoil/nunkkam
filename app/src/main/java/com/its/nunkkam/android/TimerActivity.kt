package com.its.nunkkam.android

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment

class TimerActivity : AppCompatActivity() {

    private lateinit var startButton: Button
    private lateinit var resultButton: Button
    private lateinit var timerTextView: TextView
    private lateinit var userTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_timer)

        startButton = findViewById(R.id.start_button)
        resultButton = findViewById(R.id.result_button)
        timerTextView = findViewById(R.id.timer_text)
        userTextView = findViewById(R.id.user_text_view)

        val sharedPreferences = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        val isFirstLogin = sharedPreferences.getBoolean("is_first_login", true)

        if (isFirstLogin) {
            showUserNameInputDialog()
        } else {
            val userName = UserManager.userName ?: "Guest User"
            userTextView.text = "User: $userName"
        }

        startButton.setOnClickListener {
            val intent = Intent(this, BlinkActivity::class.java)
            startActivity(intent)
        }

        resultButton.setOnClickListener {
            goToResultScreen()
        }

        addAlarmFragment()

    }

    private fun showUserNameInputDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Enter your name")

        val input = EditText(this)
        builder.setView(input)

        builder.setPositiveButton("OK") { dialog, _ ->
            val userName = input.text.toString()
            saveUserName(userName)
            dialog.dismiss()
        }

        builder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.cancel()
        }

        builder.show()
    }

    private fun saveUserName(userName: String) {
        val sharedPreferences = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            putString("user_name", userName)
            putBoolean("is_first_login", false)
            apply()
        }
        UserManager.userName = userName
        userTextView.text = "User: $userName"
    }

    private fun goToResultScreen() {
        val intent = Intent(this, ResultActivity::class.java)
        startActivity(intent)
    }

    private fun addAlarmFragment() {
        val fragment: Fragment = AlarmFragment()
        supportFragmentManager.beginTransaction()
            .replace(R.id.alarm_fragment_container, fragment)
            .commit()
    }
}
