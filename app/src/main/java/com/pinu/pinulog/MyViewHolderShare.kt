package com.pinu.gpslanglongtest

import android.content.Intent
import android.util.Log
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.recycler_item_share.view.*

class MyViewHolderShare(itemView: View): RecyclerView.ViewHolder(itemView) {

    private val email = itemView.txt_share_email
    //private val nickname = itemView.txt_share_nickname

    init{
        Log.i("파","ViewHolderShare")
    }

    fun bind(shareEmailModel: ShareEmailModel){

        Log.i("파","ViewHolder bind")
        email.text = shareEmailModel.email
        //nickname.text = shareEmailModel.nickname
    }
}