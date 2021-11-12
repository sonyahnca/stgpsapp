package com.pinu.gpslanglongtest

import android.Manifest
import android.content.Context
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
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.maps.GoogleMap
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

import org.jetbrains.anko.alert
import org.jetbrains.anko.noButton
import org.jetbrains.anko.toast
import org.jetbrains.anko.yesButton

import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

import java.io.IOException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.collections.ArrayList


class MainActivity : AppCompatActivity() {


    var longitude: Double = 1.0
    var latitude: Double = 1.0
    var markedTime: String? = null

    var periodSecond: Long = 0
    var repeatBool: Boolean = false

    //리사이클러뷰 및 지도 fragment 용
    var modelList=ArrayList<GpsLogModel>()
    private lateinit var recyclerAdapter: RecyclerAdapter

    val db = Firebase.database.reference

    private val currentUser=FirebaseAuth.getInstance().currentUser?.email
    private val currentUserUid=FirebaseAuth.getInstance().currentUser?.uid

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

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
        } else {
            when {
                isGPSEnabled -> {
                    lm.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                        1000,
                        1F,
                        gpsLocationListener)
                }
                else -> {
                }
            }
        }

        var mGeocoder = Geocoder(applicationContext, Locale.KOREAN)
        var mResultList: List<Address>? = null
        //데이터베이스 상 회원의 gpslog 읽기
        db.child("users").child(currentUserUid.toString()).get().addOnSuccessListener {
            Log.i("파이어베이스", "Got value ${(it.childrenCount)}")

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
                        addressLine
                    )
                    this.modelList.add(0,gpsLogModel)
                }
            }
        }.addOnFailureListener {
            toast("db read error")
        }

//
        loginButton.setOnClickListener {
            recyclerAdapter = RecyclerAdapter()
            recyclerAdapter.submitList(this.modelList)

            logRecyclerView.apply {
                layoutManager =
                    LinearLayoutManager(this@MainActivity, LinearLayoutManager.VERTICAL, false)
                adapter = recyclerAdapter
                Log.i("파이어베이스", "apply")
            }

        }
        if(!currentUser.isNullOrEmpty())
            loginButton.visibility=View.GONE
        else
            startActivity(Intent(this, LoginActivity::class.java))

        btn_logout.setOnClickListener {
            Firebase.auth.signOut()
            startActivity(Intent(this, LoginActivity::class.java))
        }

        setFragment()

        //val binding = ActivityMainBinding.inflate(layoutInflater)

        switch1.setOnCheckedChangeListener{CompoundButton, onSwitch ->
            if (onSwitch){
                if(editText_second.text.isBlank())
                {
                    toast("시간을 입력해주세요.")
                    switch1.isChecked=false
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
            else{
                toast("기록을 종료합니다.")
                repeatBool=false
                editText_second.isEnabled = true
                editText_min.isEnabled = true
                radioButtonCon.isEnabled = true
                radioButtonPart.isEnabled = true
            }

        }
        radioButtonGroup.setOnCheckedChangeListener{group, checkedId ->
            when(checkedId){
                R.id.radioButtonCon -> textLogPart.visibility = View.GONE
                R.id.radioButtonPart -> textLogPart.visibility = View.VISIBLE
            }
        }
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


    }
    ///////oncreate end






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
        } else {
            when {
                isGPSEnabled -> {
                    var location =
                        lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)

                    var getLongitude = location?.longitude!!
                    var getLatitude = location.latitude

                    Log.d("BasicSyntax", "Longi = $getLongitude, Lati = $getLatitude")
                    toast("현재위치: $getLatitude, $getLongitude")


                    val timestamp = LocalDateTime.now()
                    val dateTime =
                        timestamp.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                    val goodDateTime =
                        timestamp.format(DateTimeFormatter.ofPattern("yy.MM.dd HH:mm"))
                    toast("$dateTime")

                    //// firebase realtime database test 20210506
                    val database = Firebase.database

                    data class Gpslog(val lon: Double? = null, val lat: Double? = null, val memo: String?="")


                    fun writeNewGps(
                        userId: String,
                        gpstime: String,
                        longitude: Double,
                        latitude: Double,
                        memo: String
                    ) {
                        val gpslog = Gpslog(longitude, latitude, memo)
                        database.reference.child("users").child(userId).child(gpstime)
                            .setValue(gpslog)
                    }
                    writeNewGps(currentUserUid.toString(), dateTime, getLongitude, getLatitude, editText_memo.text.toString())

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
                                    addressLine,
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

}
