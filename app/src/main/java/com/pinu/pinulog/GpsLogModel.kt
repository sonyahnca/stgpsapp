package com.pinu.pinulog

import android.util.Log

class GpsLogModel(var logTime: String?=null, var lon: String?=null, var lat: String?=null, var gpsLogAddress: String?=null){

    init {
        Log.i("파","gpslogmodel")
    }
}