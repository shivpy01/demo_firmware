package com.example.firmwaredemoplunge.data.model

data class TemperartureDetailModel(
    val device_name : String,
    val cmd: Int,
    val commandId: String,
    val peripherals: Pehripherals,
    val version: Int
) {
    data class Pehripherals(
        val peripheral_name: String,
        val value: Int
    )
}