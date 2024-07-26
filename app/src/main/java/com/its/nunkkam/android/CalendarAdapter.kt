package com.its.nunkkam.android

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.DecimalFormat
import java.util.*

// RecyclerView의 Adapter 클래스를 정의합니다.
class CalendarAdapter(private val days: List<Date?>, private val infoList: List<String?>) : RecyclerView.Adapter<CalendarAdapter.DayViewHolder>() {

    private val today: Calendar = Calendar.getInstance()  // 오늘 날짜를 저장

    // ViewHolder 클래스를 정의합니다.
    class DayViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val dayTextView: TextView = itemView.findViewById(R.id.tv_day)  // 날짜를 표시할 TextView
        val infoTextView: TextView = itemView.findViewById(R.id.tv_info)  // 추가 정보를 표시할 TextView
    }

    // ViewHolder를 생성합니다.
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DayViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.calendar_day_item, parent, false)
        return DayViewHolder(view)
    }

    // ViewHolder에 데이터를 바인딩합니다.
    override fun onBindViewHolder(holder: DayViewHolder, position: Int) {
        val date = days[position]

        if (date != null) {
            val calendar = Calendar.getInstance().apply { time = date }
            holder.dayTextView.text = calendar.get(Calendar.DAY_OF_MONTH).toString()  // 날짜 설정
            val info = infoList[position]  // 추가 정보 설정

            // 오늘 날짜와 일치하는지 확인
            if (calendar.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                calendar.get(Calendar.MONTH) == today.get(Calendar.MONTH) &&
                calendar.get(Calendar.DAY_OF_MONTH) == today.get(Calendar.DAY_OF_MONTH)
            ) {
                holder.dayTextView.setBackgroundColor(Color.MAGENTA)  // 오늘 날짜의 배경색을 마젠타로 설정
            } else {
                holder.dayTextView.setBackgroundColor(Color.TRANSPARENT)  // 다른 날짜의 배경색을 투명으로 설정
            }

            if (info != null) {
                val infoFloat = info.toFloat()
                val df = DecimalFormat("0.##")  // 소수점 두 자리까지 표시하도록 포맷 설정
                val formattedInfo = df.format(infoFloat)

                // 정보 값에 따라 텍스트 색상 설정
                if (infoFloat >= 15) {
                    holder.infoTextView.setTextColor(Color.GREEN)
                } else if (infoFloat in 1f..14f) {
                    holder.infoTextView.setTextColor(Color.RED)
                } else {
                    holder.infoTextView.setTextColor(Color.BLACK)
                }
                holder.infoTextView.text = formattedInfo  // 포맷된 정보 텍스트 설정

            } else {
                holder.infoTextView.text = ""  // 정보가 없으면 빈 문자열 설정
            }
        } else {
            // 날짜가 null인 경우 (달력의 빈 공간)
            holder.dayTextView.text = ""
            holder.infoTextView.text = ""
            holder.dayTextView.setBackgroundColor(Color.TRANSPARENT)  // 빈 공간의 배경색을 투명으로 설정
        }
    }

    // 아이템의 총 개수를 반환합니다.
    override fun getItemCount(): Int {
        return days.size
    }
}