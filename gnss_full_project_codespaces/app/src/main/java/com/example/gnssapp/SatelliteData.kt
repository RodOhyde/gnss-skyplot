package com.example.gnssapp

data class SatelliteData(
    val az: Float,
    val el: Float,
    val snr: Float,
    val used: Boolean,
    val constellation: Int,
    val svid: Int
)
