package com.pinu.gpslanglongtest

import android.util.Log
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.recycler_item.view.*

class ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
    val TAG: String = "log"
    private val gpsLog = itemView.txt_gpslog
    private val gpsTime = itemView.txt_time

    init{
        Log.i("파","ViewHolder")
    }

    fun bind(gpsLogModel: GpsLogModel){

        Log.i("파","ViewHolder bind")
        gpsLog.text=gpsLogModel.lat
        gpsTime.text=gpsLogModel.logTime

    }
}