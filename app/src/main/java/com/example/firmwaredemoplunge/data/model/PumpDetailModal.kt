package com.example.firmwaredemoplunge.data.model

data class PumpDetailModal(
    val cmd: Int,
    val commandId: String,
    val peripherals: Pehripherals,
    val version: Int
) {
    data class Pehripherals(
        val device_name: String,
        val state: Int,
        val value: Int
    )
}