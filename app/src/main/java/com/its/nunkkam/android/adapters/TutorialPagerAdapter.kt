package com.its.nunkkam.android.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.its.nunkkam.android.R

class TutorialPagerAdapter(private val context: Context) : RecyclerView.Adapter<TutorialPagerAdapter.PagerViewHolder>() {

    private val layouts: List<Int> = listOf(
        R.layout.tutorial1_1,
        R.layout.tutorial1_2,
        R.layout.tutorial1_3,
        R.layout.tutorial1_4,
        R.layout.activity_tutorial
    )

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PagerViewHolder {
        val view = LayoutInflater.from(context).inflate(layouts[viewType], parent, false)
        return PagerViewHolder(view)
    }

    override fun onBindViewHolder(holder: PagerViewHolder, position: Int) {
        // 필요한 경우 여기에 뷰 바인딩 로직을 추가할 수 있습니다.
    }

    override fun getItemCount(): Int = layouts.size

    override fun getItemViewType(position: Int): Int = position

    inner class PagerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
}