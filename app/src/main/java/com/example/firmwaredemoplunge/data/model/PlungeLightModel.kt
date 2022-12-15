package com.example.firmwaredemoplunge.data.model

data class PlungeLightModel(
    val cmd: Int,
    val commandId: String,
    val peripherals: Peripherals,
    val version: Int,
) {
    data class Peripherals(
        val deviceName: String,
        val state: Int,
    )
}