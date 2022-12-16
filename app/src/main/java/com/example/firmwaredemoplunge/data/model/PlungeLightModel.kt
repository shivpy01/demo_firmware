package com.example.firmwaredemoplunge.data.model

data class PlungeLightModel(
    val device_name : String,
    val cmd: Int,
    val commandId: String,
    val peripherals: Peripherals,
    val version: Int,
) {
    data class Peripherals(
        val peripheral_name: String,
        val state: Int,
    )
}