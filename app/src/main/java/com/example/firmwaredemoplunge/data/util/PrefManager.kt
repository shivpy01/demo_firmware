package com.example.firmwaredemoplunge.data.util

import android.content.Context
import android.content.SharedPreferences


class PrefManager(
    val context: Context
) {

    var pref: SharedPreferences = context.getSharedPreferences("MyPref", 0) // 0 - for private mode
    var editor: SharedPreferences.Editor = pref.edit()

    fun saveAddedDevice(deviceName : String){
        editor.putString("SavedDevice", deviceName)
        editor.commit()
    }

    fun getAddedDevice() : String {
        return pref.getString("SavedDevice","")!!
    }

    fun clearAllData(){
        editor.clear()
        editor.commit()
    }

}