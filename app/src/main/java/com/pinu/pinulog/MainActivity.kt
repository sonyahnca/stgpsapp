package com.pinu.gpslanglongtest

import android.Manifest
import android.app.Dialog
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.location.*
import android.os.Build
import android.os.Bundle
import android.os.Handler
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.android.synthetic.main.activity_main.*

import android.util.Log
import android.view.View
import android.widget.CompoundButton
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.maps.GoogleMap
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.drawer.*
import org.jetbrains.anko.*

import java.io.IOException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.collections.ArrayList
import kotlin.concurrent.schedule
import kotlin.concurrent.timer


class MainActivity : AppCompatActivity() {


    var longitude: Double = 1.0
    var latitude: Double = 1.0
    var markedTime: String? = null

    var periodSecond: Long = 0
    var repeatBool: Boolean = false

    var partMinute: Long = 0

    //리사이클러뷰 및 지도 fragment 용
    var modelList=ArrayList<GpsLogModel>()
    private lateinit var recyclerAdapter: RecyclerAdapter

    //공유 이용자 리사이클러뷰용
    var shareModelList=ArrayList<ShareEmailModel>()
    private lateinit var recyclerAdapterShare: RecyclerAdapterShare

    val db = Firebase.database.reference

    private val currentUser=FirebaseAuth.getInstance().currentUser?.email
    private val currentUserUid=FirebaseAuth.getInstance().currentUser?.uid
    val modifiedEmailStr = currentUser.toString().replace("@","-at-").replace(".","-dot-")

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.drawer) // It is drawer layout that includes main

        // drawer(드로어) 설정
        toolbarBtn.setOnClickListener {
            drawer_layout.openDrawer((GravityCompat.START))
        }


        //gps 세팅
        val lm = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val isGPSEnabled: Boolean = lm.isProviderEnabled(LocationManager.GPS_PROVIDER)

            if (Build.VERSION.SDK_INT >= 23 &&
                ContextCompat.checkSelfPermission(
                    applicationContext,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this@MainActivity,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    0
                )
                if (Build.VERSION.SDK_INT>=29) {backgroundPermission()}
            } else {
                when {
                    isGPSEnabled -> {
                        lm.requestLocationUpdates(
                            LocationManager.GPS_PROVIDER,
                            1000,
                            1F,
                            gpsLocationListener
                        )
                    }
                    else -> {
                    }
                }
            }


        var mGeocoder = Geocoder(applicationContext, Locale.KOREAN)
        var mResultList: List<Address>? = null
        //데이터베이스 상 회원의 gpslog 읽기, share 읽기
        db.child("users").child(currentUserUid.toString()).get().addOnSuccessListener {
            Log.i("파이어베이스", "Got value ${(it.childrenCount)}")

            for (i in it.children) {
                if(i.child("lat").exists()) {
                    val gpsLogModel = GpsLogModel(
                        i.key.toString(),
                        i.child("lon").value.toString(),
                        i.child("lat").value.toString(),
                        i.child("addressLine").value.toString(),
                        i.child("memo").value.toString()
                    )
                    this.modelList.add(0, gpsLogModel)

                    recyclerAdapter = RecyclerAdapter()
                    recyclerAdapter.submitList(this.modelList)
                }
            }

            for (i in it.child("share").children){
                // 공유 이용자 읽기
                val shareEmailModel = ShareEmailModel(
                    i.key.toString().replace("-at-","@").replace("-dot-","."),
                    i.value.toString()
                )
                this.shareModelList.add(0,shareEmailModel)

                recyclerAdapterShare = RecyclerAdapterShare(this)
                recyclerAdapterShare.submitList(this.shareModelList)

                shareRecyclerView.apply {
                    layoutManager =
                        LinearLayoutManager(this@MainActivity, LinearLayoutManager.VERTICAL, false)
                    adapter = recyclerAdapterShare
                    Log.i("파이어베이스", "apply")
                }
            }
        }.addOnFailureListener {
            toast("db read error")
        }


//

        if(!currentUser.isNullOrEmpty()){
            recyclerAdapter = RecyclerAdapter()
            recyclerAdapter.submitList(this.modelList)

            logRecyclerView.apply {
                layoutManager =
                    LinearLayoutManager(this@MainActivity, LinearLayoutManager.VERTICAL, false)
                adapter = recyclerAdapter
                Log.i("파이어베이스", "apply")
            }
        }
        else
            startActivity(Intent(this, LoginActivity::class.java))

        btn_logout.setOnClickListener {
            Firebase.auth.signOut()
            startActivity(Intent(this, LoginActivity::class.java))
        }

        setFragment()

        //val binding = ActivityMainBinding.inflate(layoutInflater)

        // 상시기록, 특정시간기록 라디오 버튼
        var partLogButton: Boolean = false
        radioButtonGroup.setOnCheckedChangeListener{group, checkedId ->
            when(checkedId){
                R.id.radioButtonCon -> {
                    textLogPart.visibility = View.GONE
                    partLogButton = false
                }
                R.id.radioButtonPart -> {
                    textLogPart.visibility = View.VISIBLE
                    partLogButton = true
                }
            }
        }

        // 기록 스위치: 상시기록, 특정시간 기록에 따른 시간 설정 등
        switch1.setOnCheckedChangeListener{CompoundButton, onSwitch ->
            if (onSwitch){
                if(editText_second.text.isBlank())
                {
                    toast("시간 간격을 입력해주세요.")
                    switch1.isChecked=false
                }else {
                    if (partLogButton){
                        if(editText_min.text.isBlank()){
                            toast("기록 지속 시간을 입력해주세요.")
                            switch1.isChecked=false
                        }else {
                            partMinute = editText_min.text.toString().toLong()
                            toast("${partMinute}분 동안 기록합니다.")
                            periodSecond = editText_second.text.toString().toLong()
                            repeatBool = true
                            periodicGpsLog(periodSecond)
                            editText_second.isEnabled = false
                            editText_min.isEnabled = false
                            radioButtonCon.isEnabled = false
                            radioButtonPart.isEnabled = false

                            Handler().postDelayed({
                                switch1.isChecked=false
                            }, partMinute*60000+500L)
                        }
                    }else {
                        toast("기록을 시작합니다.")
                        periodSecond = editText_second.text.toString().toLong()
                        repeatBool = true
                        periodicGpsLog(periodSecond)
                        editText_second.isEnabled = false
                        editText_min.isEnabled = false
                        radioButtonCon.isEnabled = false
                        radioButtonPart.isEnabled = false
                    }
                }
            }
            else{
                toast("기록을 종료합니다.")
                repeatBool=false
                editText_second.isEnabled = true
                editText_min.isEnabled = true
                radioButtonCon.isEnabled = true
                radioButtonPart.isEnabled = true
            }

        }

        // 현재위치 즉시 기록 버튼
        button?.setOnClickListener {
            toast("현재 위치를 기록합니다.")
            gpsLog()
        }
        btn_viewMap.setOnClickListener{
            layout_recycler.visibility = View.GONE
            btn_viewMap.visibility = View.GONE
            btn_viewLog.visibility = View.VISIBLE
        }
        btn_viewLog.setOnClickListener{
            layout_recycler.visibility = View.VISIBLE
            btn_viewLog.visibility = View.GONE
            btn_viewMap.visibility = View.VISIBLE
        }


        // 공유 유저 이메일 추가 버튼
        btn_share_email_add.setOnClickListener {
            if(editText_share_email_add.text.isBlank())
                toast("이메일을 입력해주세요.")
            else {
                // 추가하려는 대상의 uid를 받아 저장함
                db.child("users")
                    .child("userEmail")
                    .child(editText_share_email_add.text.toString().replace("@", "-at-")
                    .replace(".", "-dot-"))
                    .get().addOnSuccessListener {
                        if(it.value.toString() != "null") {
                            toast("공유 대상을 추가합니다.")
                            this.shareModelList.clear()
                            db.child("users")
                                .child(currentUserUid.toString())
                                .child("share")
                                .child(
                                    editText_share_email_add.text.toString().replace("@", "-at-")
                                        .replace(".", "-dot-")
                                ).setValue(it.value.toString())

                            db.child("users").child(currentUserUid.toString()).get().addOnSuccessListener {
                                for (i in it.child("share").children) {
                                    // 공유 이용자 읽기
                                    val shareEmailModel = ShareEmailModel(
                                        i.key.toString().replace("-at-", "@").replace("-dot-", "."),
                                        i.value.toString()
                                    )
                                    this.shareModelList.add(0, shareEmailModel)

                                    recyclerAdapterShare = RecyclerAdapterShare(this)
                                    recyclerAdapterShare.submitList(this.shareModelList)

                                    shareRecyclerView.apply {
                                        layoutManager =
                                            LinearLayoutManager(
                                                this@MainActivity,
                                                LinearLayoutManager.VERTICAL,
                                                false
                                            )
                                        adapter = recyclerAdapterShare
                                        Log.i("파이어베이스", "apply")
                                    }
                                }
                            }.addOnFailureListener {
                                toast("db read error")
                            }
                        }else{
                            toast("존재하지 않는 계정입니다.")
                        }
                }.addOnFailureListener{
                    toast("DB ERROR")
                }
            }
        }

        btn_my_list.setOnClickListener {
            layout_share_list.visibility = View.GONE
            layout_my_list.visibility = View.VISIBLE
            btn_my_list.isEnabled = false
            btn_share_list.isEnabled = true
        }


        btn_share_list.setOnClickListener {
            layout_share_list.visibility = View.VISIBLE
            layout_my_list.visibility = View.GONE
            btn_my_list.isEnabled = true
            btn_share_list.isEnabled = false
        }


    }
    ///////onCreate end



    //20210520
    fun setFragment() {
        val mapsFragment: MapsFragment= MapsFragment()

        var bundle = Bundle()
        bundle.putDouble("longitude", longitude)
        bundle.putDouble("latitude", latitude)
        bundle.putString("markedTime", markedTime)

        mapsFragment.arguments = bundle

        val transaction = supportFragmentManager.beginTransaction()
        transaction.add(R.id.map, mapsFragment)
        transaction.commit()
    }

    //반복실행
    private val mDelayHandler: Handler by lazy{
        Handler()
    }

    private fun periodicGpsLog(second: Long){
        mDelayHandler.postDelayed(::gpsPeriodic, second*1000)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun gpsPeriodic(){
        if(repeatBool) {
            gpsLog()
            periodicGpsLog(periodSecond)

            /* 리사이클러 업데이트 -- gpsLog 로 옮김
            recyclerAdapter = RecyclerAdapter()
            recyclerAdapter.submitList(this.modelList)

            logRecyclerView.apply {
                layoutManager =
                    LinearLayoutManager(this@MainActivity, LinearLayoutManager.VERTICAL, false)
                adapter = recyclerAdapter
                Log.i("파이어베이스", "apply")
            }
             */
        }
    }


    @RequiresApi(Build.VERSION_CODES.O)
    private fun gpsLog(){
        val lm = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        val isGPSEnabled: Boolean = lm.isProviderEnabled(LocationManager.GPS_PROVIDER)
        val isNetworkEnabled: Boolean = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

        if(Build.VERSION.SDK_INT >=29 &&
            ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ){
            toast("백그라운드 작동을 위해 권한을 항상 허용으로 설정해주세요.")
            ActivityCompat.requestPermissions(
                this@MainActivity,
                arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION),
                0
            )
        }
        if (Build.VERSION.SDK_INT >= 23 &&
            ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this@MainActivity,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                0
            )
            if (Build.VERSION.SDK_INT>=29) {backgroundPermission()}
        } else {
            when {
                isGPSEnabled -> {
                    var location =
                        lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)

                    var getLongitude = location?.longitude!!
                    var getLatitude = location.latitude
                    var getAddressLine = geocoder(getLatitude, getLongitude)

                    Log.d("BasicSyntax", "Longi = $getLongitude, Lati = $getLatitude")

                    // toast("현재위치: $getLatitude, $getLongitude")


                    val timestamp = LocalDateTime.now()
                    val dateTime =
                        timestamp.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                    val goodDateTime =
                        timestamp.format(DateTimeFormatter.ofPattern("yy.MM.dd HH:mm"))
                    // toast("$dateTime")

                    //// firebase realtime database test 20210506
                    val database = Firebase.database

                    data class Gpslog(val lon: Double? = null, val lat: Double? = null, val memo: String?="", val device: String?="", val addressLine: String?="")


                    fun writeNewGps(
                        userId: String,
                        gpstime: String,
                        longitude: Double,
                        latitude: Double,
                        memo: String,
                        device: String,
                        addressLine: String
                    ) {
                        val gpslog = Gpslog(longitude, latitude, memo, device, addressLine)
                        database.reference.child("users").child(userId).child(gpstime)
                            .setValue(gpslog)
                    }
                    writeNewGps(currentUserUid.toString(), dateTime, getLongitude, getLatitude, editText_memo.text.toString(),
                        // 디바이스 이름 넣기
                        Build.MODEL, getAddressLine)
                    //// test ends

                    //test frame fragment global var
                    longitude = getLongitude
                    latitude = getLatitude
                    markedTime = goodDateTime
                    //

                    // and let it be on the textview -test
                    var mGeocoder = Geocoder(applicationContext, Locale.KOREAN)
                    var mResultList: List<Address>? = null
                    //데이터베이스 상 회원의 gpslog 읽기
                    db.child("users").child(currentUserUid.toString()).get().addOnSuccessListener {
                        Log.i("파이어베이스", "Got value ${(it.childrenCount)}")


                        //////////// IMPORTANT ////////////
                        this.modelList.clear() //modelList 중복 입력 방지 임시방편
                        ///////////////////////////////////

                        for (i in it.children) {
                            var addressLine : String? = null
                            if(i.child("lat").exists()) {
                                try {

                                    mResultList = mGeocoder.getFromLocation(
                                        i.child("lat").value.toString().toDouble(),
                                        i.child("lon").value.toString().toDouble(), 1
                                    )
                                } catch (e: IOException) {
                                }
                                if (mResultList != null) {
                                    Log.i("파이어베이스", mResultList!![0].getAddressLine(0))
                                    addressLine = mResultList!![0].getAddressLine(0)
                                }

                                val gpsLogModel = GpsLogModel(
                                    i.key.toString(),
                                    i.child("lon").value.toString(),
                                    i.child("lat").value.toString(),
                                    i.child("addressLine").value.toString(),
                                    i.child("memo").value.toString()
                                )
                                this.modelList.add(0,gpsLogModel)

                                recyclerAdapter = RecyclerAdapter()
                                recyclerAdapter.submitList(this.modelList)

                                logRecyclerView.apply {
                                    layoutManager =
                                        LinearLayoutManager(this@MainActivity, LinearLayoutManager.VERTICAL, false)
                                    adapter = recyclerAdapter
                                    Log.i("파이어베이스", "apply")
                                }
                            }
                        }
                    }.addOnFailureListener {
                        toast("db read error")
                    }
                    //TV_Result.text="$goodDateTime\n $getLatitude, $getLongitude"
                    setFragment()
                }
                else -> {
                }

            }
        }
    }

    val gpsLocationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            val provider: String = location.provider
            val longitude: Double = location.longitude
            val latitude: Double = location.latitude
            val altitude: Double = location.altitude
        }

        override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {}
        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {}
    }

    private fun backgroundPermission(){
        ActivityCompat.requestPermissions(
            this@MainActivity,
            arrayOf(
                android.Manifest.permission.ACCESS_BACKGROUND_LOCATION,
            ), 2)
    }

    // 좌표 -> 주소
    fun geocoder(lat: Double=0.0, lon: Double=0.0): String {
        var mGeocoder = Geocoder(applicationContext, Locale.KOREAN)
        var mResultList: List<Address>? = null
        mResultList = mGeocoder.getFromLocation(lat, lon, 1)
        return mResultList!![0].getAddressLine(0)
    }


}
