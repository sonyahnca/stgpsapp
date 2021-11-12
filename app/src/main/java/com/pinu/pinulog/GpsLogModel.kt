package com.pinu.gpslanglongtest

import android.util.Log

class GpsLogModel(var logTime: String?=null, var lon: String?=null, var lat: String?=null, var gpsLogAddress: String?=null, var memo: String?=""){

    init {
        Log.i("파","gpslogmodel")
    }
}