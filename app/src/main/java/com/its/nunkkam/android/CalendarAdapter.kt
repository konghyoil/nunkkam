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

class CalendarAdapter(
    private val days: List<Date?>,
    private val infoList: List<String?>,
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
        return DayViewHolder(view)
    }

    override fun onBindViewHolder(holder: DayViewHolder, position: Int) {
        val date = days[position]
        val info = infoList[position]

        holder.itemView.setOnClickListener {
            onItemClick(date, info)
        }

        if (date != null) {
            val calendar = Calendar.getInstance().apply { time = date }
            holder.dayTextView.text = calendar.get(Calendar.DAY_OF_MONTH).toString()

            if (calendar.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                calendar.get(Calendar.MONTH) == today.get(Calendar.MONTH) &&
                calendar.get(Calendar.DAY_OF_MONTH) == today.get(Calendar.DAY_OF_MONTH)
            ) {
                holder.dayTextView.setBackgroundColor(Color.MAGENTA)
            } else {
                holder.dayTextView.setBackgroundColor(Color.TRANSPARENT)
            }

            if (info != null) {
                val infoFloat = info.toFloat()
                val df = DecimalFormat("0.##")
                val formattedInfo = df.format(infoFloat)

                if (infoFloat >= 15) {
                    holder.infoTextView.setTextColor(Color.GREEN)
                } else if (infoFloat in 1f..14f) {
                    holder.infoTextView.setTextColor(Color.RED)
                } else {
                    holder.infoTextView.setTextColor(Color.BLACK)
                }
                holder.infoTextView.text = formattedInfo

            } else {
                holder.infoTextView.text = ""
            }
        } else {
            holder.dayTextView.text = ""
            holder.infoTextView.text = ""
            holder.dayTextView.setBackgroundColor(Color.TRANSPARENT)
        }
    }

    override fun getItemCount(): Int {
        return days.size
    }
}
