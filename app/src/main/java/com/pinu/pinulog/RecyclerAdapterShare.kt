package com.pinu.gpslanglongtest

import android.content.Context
import android.content.Intent
import android.opengl.Visibility
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.recycler_item_share.view.*

import org.jetbrains.anko.*
import java.io.IOException
import java.util.*
import kotlin.collections.ArrayList


class RecyclerAdapterShare(private val context: Context): RecyclerView.Adapter<MyViewHolderShare>() {

    private var shareModelList = ArrayList<ShareEmailModel>()

    // 공유 계정의 기록 리사이클러뷰용
    var modelList=ArrayList<GpsLogModel>()
    private lateinit var recyclerAdapterShareLog: RecyclerAdapterShareLog


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolderShare {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.recycler_item_share, parent, false)
        return MyViewHolderShare(view)
    }

    override fun getItemCount(): Int {
        return this.shareModelList.size
    }

    override fun onBindViewHolder(holder: MyViewHolderShare, position: Int) {
        Log.i("파", "Adapter BindViewHolder")
        holder.bind(this.shareModelList[position])

        holder.itemView.setOnClickListener {
            if (holder.itemView.share_btn.visibility == View.GONE) {
                holder.itemView.share_btn.visibility = View.VISIBLE
                holder.itemView.shareRecyclerViewDetail.visibility = View.VISIBLE
            } else {
                holder.itemView.share_btn.visibility = View.GONE
                holder.itemView.shareRecyclerViewDetail.visibility = View.GONE
            }
        }
            // 공유 기록 보기
            holder.itemView.btn_share_email_log.setOnClickListener {
                if(holder.itemView.shareRecyclerViewLogFrame.visibility == View.GONE) {
                    holder.itemView.shareRecyclerViewLogFrame.visibility = View.VISIBLE
                    var permissionCheck: Boolean = false
                    Firebase.database.reference.child("users").child(this.shareModelList[position].uid.toString()).get().addOnSuccessListener {
                        for (i in it.child("share").children){
                            if(i.key == FirebaseAuth.getInstance().currentUser?.email.toString().replace("@","-at-").replace(".","-dot-")) {
                                permissionCheck = true
                                break
                            }
                        }
                        if(permissionCheck) {
                            holder.itemView.share_alert.visibility=View.GONE
                            holder.itemView.btn_share_email_log.text = "덮기"
                            this.modelList.clear() //modelList 중복 입력 방지 임시방편

                            for (i in it.children) {
                                if (i.child("lat").exists()) {
                                    val gpsLogModel = GpsLogModel(
                                        i.key.toString(),
                                        i.child("lon").value.toString(),
                                        i.child("lat").value.toString(),
                                        i.child("addressLine").value.toString(),
                                        i.child("memo").value.toString()
                                    )
                                    this.modelList.add(0, gpsLogModel)

                                    recyclerAdapterShareLog = RecyclerAdapterShareLog()
                                    recyclerAdapterShareLog.submitList(this.modelList)

                                    holder.itemView.shareRecyclerViewLog.apply {
                                        layoutManager =
                                            LinearLayoutManager(
                                                context,
                                                LinearLayoutManager.VERTICAL,
                                                false
                                            )
                                        adapter = recyclerAdapterShareLog
                                        Log.i("파이어베이스", "apply")
                                    }
                                }
                            }
                        }else{holder.itemView.share_alert.visibility=View.VISIBLE}
                    }.addOnFailureListener {
                        Log.i("파이어베이스", "DB READ ERROR")
                    }
                }
                else{
                    holder.itemView.shareRecyclerViewLogFrame.visibility = View.GONE
                    holder.itemView.btn_share_email_log.text = "보기"
                }
            }

            // 공유 이메일 삭제
            holder.itemView.btn_share_email_delete.setOnClickListener {
                Firebase.database.reference.child("users")
                    .child(FirebaseAuth.getInstance().currentUser?.uid.toString())
                    .child("share")
                    .child(this.shareModelList[position].email.toString().replace("@","-at-").replace(".","-dot-")).removeValue()
                shareModelList.removeAt(position)
                notifyDataSetChanged()
                holder.itemView.share_btn.visibility = View.GONE
            }

/*
            // 공유 이메일 수정
            holder.itemView.btn_share_email_modify.setOnClickListener {
                holder.itemView.share_btn1.visibility = View.GONE
                holder.itemView.share_btn2.visibility = View.VISIBLE
            }
            holder.itemView.btn_share_email_modify_done.setOnClickListener {
                holder.itemView.share_btn1.visibility = View.VISIBLE
                holder.itemView.share_btn2.visibility = View.GONE
                Firebase.database.reference.child("users")
                    .child(FirebaseAuth.getInstance().currentUser?.uid.toString())
                    .child("share")
                    .child(this.shareModelList[position].email.toString().replace("@","-at-").replace(".","-dot-"))
                    .removeValue()
                Firebase.database.reference.child("users")
                    .child("userEmail")
                    .child(holder.itemView.editText_share_email_modify.text.toString().replace("@", "-at-")
                        .replace(".", "-dot-"))
                    .get().addOnSuccessListener {
                        //toast("공유 대상을 추가합니다.")
                        if(it.value.toString() != "null") {
                            this.shareModelList.clear()
                            Firebase.database.reference.child("users")
                                .child(FirebaseAuth.getInstance().currentUser?.uid.toString())
                                .child("share")
                                .child(
                                    holder.itemView.editText_share_email_modify.text.toString()
                                        .replace("@", "-at-")
                                        .replace(".", "-dot-")
                                ).setValue(it.value.toString())
                            this.shareModelList[position].email =
                                holder.itemView.editText_share_email_modify.text.toString()
                            notifyDataSetChanged()
                        }else {
                            // toast("존재하지 않는 계정입니다.")
                        }
                    }.addOnFailureListener{
                        // toast("DB ERROR")
                    }
            }
            holder.itemView.btn_share_email_modify_cancel.setOnClickListener {
                holder.itemView.share_btn1.visibility = View.VISIBLE
                holder.itemView.share_btn2.visibility = View.GONE
            }
*/
    }

    fun submitList(shareModelList: ArrayList<ShareEmailModel>){
        this.shareModelList=shareModelList
    }

}