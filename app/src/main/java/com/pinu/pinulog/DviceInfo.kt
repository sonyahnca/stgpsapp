package com.pinu.gpslanglongtest

import android.content.Context
import android.os.Build
import android.provider.Settings

class DviceInfo(val context: Context) {
    /*
    // 디바이스 아이디
    fun getDeviceId(): String{
        return Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
    }
    */

    // 디바이스 모델
    fun getDeviceModel(): String{
        return Build.MODEL
    }
}