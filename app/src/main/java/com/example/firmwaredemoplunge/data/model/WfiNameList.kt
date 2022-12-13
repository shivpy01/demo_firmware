package com.example.firmwaredemoplunge.data.model

class WfiNameList : ArrayList<WfiNameList.WfiNameListItem>(){
    data class WfiNameListItem(
        val wifi_name: String,
        val wifi_rssi: Int
    )
}