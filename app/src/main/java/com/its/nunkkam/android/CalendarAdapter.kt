package com.its.nunkkam.android

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.util.*

class CalendarAdapter(private val days: List<Date?>, private val infoList: List<String?>) : RecyclerView.Adapter<CalendarAdapter.DayViewHolder>() {

    private val today: Calendar = Calendar.getInstance()  // 오늘 날짜를 저장

    class DayViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val dayTextView: TextView = itemView.findViewById(R.id.tv_day)
        val infoTextView: TextView = itemView.findViewById(R.id.tv_info)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DayViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.calendar_day_item, parent, false)
        return DayViewHolder(view)
    }

    override fun onBindViewHolder(holder: DayViewHolder, position: Int) {
        val date = days[position]
        if (date != null) {
            val calendar = Calendar.getInstance().apply { time = date }
            holder.dayTextView.text = calendar.get(Calendar.DAY_OF_MONTH).toString()
            val info = infoList[position]  // 추가 정보 설정

            // 오늘 날짜와 일치하는지 확인
            if (calendar.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                calendar.get(Calendar.MONTH) == today.get(Calendar.MONTH) &&
                calendar.get(Calendar.DAY_OF_MONTH) == today.get(Calendar.DAY_OF_MONTH)
            ) {
                holder.dayTextView.setBackgroundColor(Color.MAGENTA)  // 오늘 날짜의 배경색을 노란색으로 설정
            } else {
                holder.dayTextView.setBackgroundColor(Color.TRANSPARENT)  // 다른 날짜의 배경색을 투명으로 설정
            }

            if (info != null) {
                if (info.toInt() >= 15) {
                    holder.infoTextView.setTextColor(Color.GREEN)
                } else if (info.toInt() in 1..14) {
                    holder.infoTextView.setTextColor(Color.RED)
                } else {
                    holder.infoTextView.setTextColor(Color.BLACK)
                }
                holder.infoTextView.text = info
                //holder.infoTextView.text = "●"
            } else {
                holder.infoTextView.text = ""
            }
        } else {
            holder.dayTextView.text = ""
            holder.infoTextView.text = ""
            holder.dayTextView.setBackgroundColor(Color.TRANSPARENT)  // 빈 공간의 배경색을 투명으로 설정
        }
    }

    override fun getItemCount(): Int {
        return days.size
    }
}