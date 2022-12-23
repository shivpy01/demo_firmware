package com.example.firmwaredemoplunge.data.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.LOCATION_SERVICE
import android.content.Intent
import android.location.LocationManager
import android.util.Log


class LocationService : BroadcastReceiver() {
    var isGPSEnabled = false
    override fun onReceive(context: Context, intent: Intent) {
        try {
            val locationManager = context.getSystemService(LOCATION_SERVICE) as LocationManager
            isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
            Log.e("Location", "onReceive: $isGPSEnabled", )
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }
}