package com.pinu.pinulog

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

class RecyclerAdapter: RecyclerView.Adapter<MyViewHolder>() {


    private var modelList = ArrayList<GpsLogModel>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        return MyViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.recycler_item, parent, false)
        )
    }

    override fun getItemCount(): Int {
        return this.modelList.size
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        Log.i("파","Adapter BindViewHolder")
        holder.bind(this.modelList[position])
    }

    fun submitList(modelList: ArrayList<GpsLogModel>){
        this.modelList=modelList
    }

}