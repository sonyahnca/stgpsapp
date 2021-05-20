package com.pinu.pinulog

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.android.synthetic.main.activity_main.*

import android.util.Log
import androidx.annotation.RequiresApi

import org.jetbrains.anko.alert
import org.jetbrains.anko.noButton
import org.jetbrains.anko.toast
import org.jetbrains.anko.yesButton

import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter


class MainActivity : AppCompatActivity() {

    var flongitude: Double = 1.0
    var flatitude: Double = 1.0
    var markedTime: String? = null

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setFragment()

        val lm = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        button?.setOnClickListener {
            //check permission var
            val isGPSEnabled: Boolean = lm.isProviderEnabled(LocationManager.GPS_PROVIDER)

            if (Build.VERSION.SDK_INT >= 23 && ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this@MainActivity, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 0)
            }
            else {
                if(isGPSEnabled){
                    val location = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                    var longitude = location?.longitude!!
                    var latitude = location.latitude
                    Log.d("BasicSyntax", "Lati = $latitude, Longi = $longitude")
                    toast("현재위치: $latitude, $longitude")
                    val timestamp= LocalDateTime.now()
                    val dateTime=timestamp.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                    val goodDateTime=timestamp.format(DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm:ss"))
                    toast("$dateTime")
                    //// firebase realtime database test 20210506
                    val database = Firebase.database
                    val myRef = database.getReference("$dateTime")
                        myRef.setValue("$latitude, $longitude")
                    //// test ends

                    // test frame fragment global var
                    flongitude = longitude
                    flatitude = latitude
                    markedTime = goodDateTime
                    //

                    // on the textview -test
                    Textview1.text="$goodDateTime\n $latitude, $longitude"

                    setFragment()
                }

                /*
                //for updates by time and distance
                lm.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                        1000, //s as time
                        1F,  //metre as distance
                        gpsLocationListener)

                //lm.removeUpdates(gpsLocationListener)*/
                //for stop
            }
        }
    }

    // for updates frequently
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

    fun setFragment() {
        val mapsFragment: MapsFragment= MapsFragment()

        var bundle = Bundle()
        bundle.putDouble("longitude", flongitude)
        bundle.putDouble("latitude", flatitude)
        bundle.putString("markedTime", markedTime)
        mapsFragment.arguments = bundle

        val transaction = supportFragmentManager.beginTransaction()
        transaction.add(R.id.map, mapsFragment)
        transaction.commit()
    }
}