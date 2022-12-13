package com.example.firmwaredemoplunge.data.model

data class CreateThingResponse(
    val certificateArn: String,
    val certificatePem: String,
    val privateKey: String,
    val thingName: String
)