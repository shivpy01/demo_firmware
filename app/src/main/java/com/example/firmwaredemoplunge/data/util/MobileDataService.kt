package com.example.firmwaredemoplunge.data.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.util.Log


class MobileDataService : BroadcastReceiver() {
    var isDataConnected = false

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action.equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
            Log.d("NetworkCheckReceiver", "NetworkCheckReceiver invoked...")
            val noConnectivity = intent.getBooleanExtra(
                ConnectivityManager.EXTRA_NO_CONNECTIVITY, false)
            if (!noConnectivity) {
                isDataConnected = true
                Log.d("NetworkCheckReceiver", "connected")
            } else {
                isDataConnected = false
                Log.d("NetworkCheckReceiver", "disconnected")
            }
        }
    }
}