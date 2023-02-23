package com.example.clock.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.clock.R
import com.example.clock.models.Alarm
import com.google.android.material.materialswitch.MaterialSwitch

class AlarmAdapter(
    private val dataset: MutableList<Alarm>
) : RecyclerView.Adapter<AlarmAdapter.AlarmViewHolder>() {

    private lateinit var mListener: OnRecyclerViewItemClickedListener

    interface OnRecyclerViewItemClickedListener {
        fun onAlarmClicked(position: Int)
        fun onSwitchClicked(position: Int)
    }


    fun setOnRecyclerViewItemClickedListener(listener: OnRecyclerViewItemClickedListener) {
        mListener = listener
    }


    class AlarmViewHolder(view: View, listener: OnRecyclerViewItemClickedListener) : RecyclerView.ViewHolder(view) {
        val textView: TextView = view.findViewById(R.id.alarm_list_item_time)
        val switch: MaterialSwitch = view.findViewById(R.id.alarm_list_item_switch)

        init {
            view.setOnClickListener {
                listener.onAlarmClicked(adapterPosition)
            }

            switch.setOnClickListener {
                listener.onSwitchClicked(adapterPosition)
            }
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlarmViewHolder {
        val adapterLayout = LayoutInflater.from(parent.context)
            .inflate(R.layout.alarm_list_item, parent, false)

        return AlarmViewHolder(adapterLayout, mListener)
    }

    override fun onBindViewHolder(holder: AlarmViewHolder, position: Int) {
        val item = dataset[position]
        holder.textView.text = getAlarmTime(item)
        holder.switch.isChecked = item.state == 1
    }

    override fun getItemCount(): Int {
        return dataset.size
    }

    private fun getAlarmTime(alarm: Alarm): String {
        var time: String = ""

        if (alarm.hour < 10) {
            time += "0" + alarm.hour
        } else {
            time += alarm.hour
        }
        time += ':'
        if (alarm.minute < 10) {
            time += "0" + alarm.minute
        } else {
            time += alarm.minute
        }

        return time
    }

}