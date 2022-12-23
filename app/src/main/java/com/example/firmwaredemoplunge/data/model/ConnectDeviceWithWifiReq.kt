package com.example.firmwaredemoplunge.data.model

import com.google.gson.annotations.SerializedName

class ConnectDeviceWithWifiReq {

    @SerializedName("wifi_ssid")
    var `wifi_ssid`: String? = null

    @SerializedName("wifi_pass")
    var `wifi_pass`: String? = null

}