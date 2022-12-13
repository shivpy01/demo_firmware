package com.example.firmwaredemoplunge.data.model

import com.google.gson.annotations.SerializedName
import okhttp3.MultipartBody

class ReqRouter {
    @SerializedName("wifi_ssid")
    var `wifi_ssid` : String?=null

    @SerializedName("wifi_pass")
    var `wifi_pass` : String?=null

    @SerializedName("device_id")
    var `device_id` : String?=null

    @SerializedName("mqtt_addr")
    var `mqtt_addr` : String?=null

   /* @SerializedName("root_cert")
    var `root_cert` : MultipartBody.Part?=null

    @SerializedName("client_cert")
    var `client_cert` : MultipartBody.Part?=null

    @SerializedName("cert_key")
    var `cert_key` : MultipartBody.Part?=null*/
}