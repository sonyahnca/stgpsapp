package com.pinu.pinulog

import android.util.Log
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.recycler_item.view.*

class MyViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {

    private val gpsLog = itemView.txt_gpslog
    private val gpsTime = itemView.txt_time

    init{
        Log.i("파","ViewHolder")
    }

    fun bind(gpsLogModel: GpsLogModel){

        Log.i("파","ViewHolder bind")
        gpsLog.text=gpsLogModel.gpsLogAddress
        gpsTime.text=gpsLogModel.logTime
    }
}