package com.its.nunkkam.android

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*
import android.util.Log // 수정: Log import 추가

class CalendarAdapter(
    private var days: List<Date?>,
    private var infoList: List<String?>,
    private val itemWidth: Int,
    private val itemHeight: Int,
    private val onItemClick: (Date?, String?) -> Unit
) : RecyclerView.Adapter<CalendarAdapter.DayViewHolder>() {

    private val today: Calendar = Calendar.getInstance()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    class DayViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val dayTextView: TextView = itemView.findViewById(R.id.tv_day)
        val infoTextView: TextView = itemView.findViewById(R.id.tv_info)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DayViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.calendar_day_item, parent, false)
        view.layoutParams.width = itemWidth
        view.layoutParams.height = itemHeight
        return DayViewHolder(view)
    }

    override fun onBindViewHolder(holder: DayViewHolder, position: Int) {
        // 수정: 예외 처리 추가
        try {
            val date = days.getOrNull(position)
            val info = infoList.getOrNull(position)

            holder.itemView.setOnClickListener {
                onItemClick(date, info)
            }

            if (date != null) {
                val calendar = Calendar.getInstance().apply { time = date }
                holder.dayTextView.text = calendar.get(Calendar.DAY_OF_MONTH).toString()
                holder.dayTextView.visibility = View.VISIBLE

                if (calendar.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                    calendar.get(Calendar.MONTH) == today.get(Calendar.MONTH) &&
                    calendar.get(Calendar.DAY_OF_MONTH) == today.get(Calendar.DAY_OF_MONTH)
                ) {
                    holder.dayTextView.setBackgroundColor(Color.parseColor("#505050"))
                } else {
                    holder.dayTextView.setBackgroundColor(Color.TRANSPARENT)
                }

                if (info != null) {
                    val infoFloat = info.toFloatOrNull() ?: 0f
                    val df = DecimalFormat("0.##")
                    val formattedInfo = df.format(infoFloat)

                    holder.infoTextView.text = formattedInfo
                    holder.infoTextView.visibility = View.VISIBLE

                    when {
                        infoFloat >= 15 -> holder.infoTextView.setTextColor(Color.parseColor("#848484"))
                        infoFloat in 1f..14f -> holder.infoTextView.setTextColor(Color.parseColor("#595959"))
                        else -> holder.infoTextView.setTextColor(Color.BLACK)
                    }
                } else {
                    holder.infoTextView.text = ""
                    holder.infoTextView.visibility = View.INVISIBLE
                }
            } else {
                holder.dayTextView.text = ""
                holder.dayTextView.visibility = View.INVISIBLE
                holder.infoTextView.text = ""
                holder.infoTextView.visibility = View.INVISIBLE
            }
        } catch (e: Exception) {
            // 수정: 예외 발생 시 로그 출력
            Log.e("CalendarAdapter", "Error in onBindViewHolder", e)
            holder.dayTextView.text = ""
            holder.dayTextView.visibility = View.INVISIBLE
            holder.infoTextView.text = ""
            holder.infoTextView.visibility = View.INVISIBLE
        }
    }

    override fun getItemCount(): Int {
        return days.size
    }

    // 수정: updateData 함수에 예외 처리 추가
    fun updateData(newDays: List<Date?>, newInfoList: List<String?>) {
        try {
            days = newDays
            infoList = newInfoList
            notifyDataSetChanged()
        } catch (e: Exception) {
            // 수정: 예외 발생 시 로그 출력
            Log.e("CalendarAdapter", "Error updating data", e)
        }
    }
}