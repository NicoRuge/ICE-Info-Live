package com.nruge.iceinfo.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager

fun isWIFIonICE(context: Context): Boolean {
    val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val network = connectivityManager.activeNetwork ?: return false
    val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
    
    if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        @Suppress("DEPRECATION")
        val wifiInfo = wifiManager.connectionInfo
        val ssid = wifiInfo.ssid?.replace("\"", "")
        
        if (ssid == "WIFIonICE") return true
        
        // Android 10+ SSID protection check
        // Often in ICE trains, the local domain resolves or we have a specific gateway
        // But for now, the SSID check is what was requested.
    }
    return false
}
