package com.nruge.iceinfo.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build

/**
 * Returns true if the device is currently connected to the "WIFIonICE" SSID.
 *
 * On Android 12+ (API 31) the SSID can be read from NetworkCapabilities.transportInfo
 * without needing ACCESS_FINE_LOCATION.
 * On Android 8–11 the deprecated WifiManager.connectionInfo() is used; without
 * ACCESS_FINE_LOCATION it returns "<unknown ssid>", in which case we fall back to
 * false (the hint text simply won't appear — no functional impact).
 */
fun isWIFIonICE(context: Context): Boolean {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val network = cm.activeNetwork ?: return false
    val caps = cm.getNetworkCapabilities(network) ?: return false
    if (!caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) return false

    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        // API 31+: no location permission needed
        val wifiInfo = caps.transportInfo as? WifiInfo ?: return false
        wifiInfo.ssid?.trim('"') == "WIFIonICE"
    } else {
        // API < 31: deprecated — needs ACCESS_FINE_LOCATION for real SSID.
        // Without the permission Android returns "<unknown ssid>"; we accept false gracefully.
        @Suppress("DEPRECATION")
        val wifiManager =
            context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        @Suppress("DEPRECATION")
        val ssid = wifiManager.connectionInfo?.ssid?.trim('"')
        ssid == "WIFIonICE"
    }
}
