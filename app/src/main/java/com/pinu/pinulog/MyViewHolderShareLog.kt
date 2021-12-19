package com.pinu.gpslanglongtest

import android.content.Intent
import android.location.Address
import android.location.Geocoder
import android.util.Log
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.recycler_item_share_log.view.*
import java.util.*

class MyViewHolderShareLog(itemView: View): RecyclerView.ViewHolder(itemView) {

    private val gpsLog = itemView.txt_gpslog
    private val gpsTime = itemView.txt_time
    private val gpsMemo = itemView.txt_memo
    private val gpsLat = itemView.txt_lat
    private val gpsLon = itemView.txt_lon

    init{
        Log.i("파","ViewHolderShare")
    }

    fun bind(gpsLogModel: GpsLogModel){

        Log.i("파","ViewHolder bind")
        gpsLog.text=gpsLogModel.gpsLogAddress
        gpsTime.text=gpsLogModel.logTime
        gpsMemo.text=gpsLogModel.memo
        gpsLat.text=gpsLogModel.lat
        gpsLon.text=gpsLogModel.lon

    }

}