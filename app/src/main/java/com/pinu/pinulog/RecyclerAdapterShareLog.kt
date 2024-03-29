package com.pinu.gpslanglongtest

import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.recycler_item_share_log.view.*

class RecyclerAdapterShareLog: RecyclerView.Adapter<MyViewHolderShareLog>() {


    private var modelList = ArrayList<GpsLogModel>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolderShareLog {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.recycler_item_share_log, parent, false)
        return MyViewHolderShareLog(view)
    }

    override fun getItemCount(): Int {
        return this.modelList.size
    }

    override fun onBindViewHolder(holder: MyViewHolderShareLog, position: Int) {
        Log.i("파", "Adapter BindViewHolder")
        holder.bind(this.modelList[position])

        holder.itemView.setOnClickListener {
            Log.i("리", "${this.modelList[position].logTime}")
            if (holder.itemView.gps_log_detail.visibility == View.GONE)
                holder.itemView.gps_log_detail.visibility = View.VISIBLE
            else
                holder.itemView.gps_log_detail.visibility = View.GONE
        }
    }

    fun submitList(modelList: ArrayList<GpsLogModel>){
        this.modelList=modelList
    }

}