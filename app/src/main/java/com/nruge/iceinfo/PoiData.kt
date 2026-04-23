package com.nruge.iceinfo

data class PoiItem(
    val name: String,
    val type: String,
    val distance: Int,
    val latitude: Double,
    val longitude: Double,
    val description: String = ""
)

