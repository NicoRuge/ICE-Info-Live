package com.nruge.iceinfo.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Returns true if the device is currently connected to the train's "WIFIonICE" network.
 *
 * The SSID is location-sensitive: since Android 12 apps only get the real SSID with
 * ACCESS_FINE_LOCATION (and a NetworkCallback registered with FLAG_INCLUDE_LOCATION_INFO) —
 * this app requests no location permission, so the SSID read below always yields
 * "<unknown ssid>" in practice. It is kept as a fast path in case a device does expose it.
 *
 * The reliable, permission-free detection is a DNS heuristic: inside the train network the
 * local DNS resolves iceportal.de to a private address (10.x/172.x), while on the public
 * internet it resolves to a public IP. The lookup is forced through the Wi-Fi [android.net.Network]
 * so an active mobile-data connection cannot answer instead.
 */
suspend fun isWIFIonICE(context: Context): Boolean = withContext(Dispatchers.IO) {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val network = cm.activeNetwork ?: return@withContext false
    val caps = cm.getNetworkCapabilities(network) ?: return@withContext false
    if (!caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) return@withContext false

    val ssid = (caps.transportInfo as? WifiInfo)?.ssid?.trim('"')
    if (ssid == "WIFIonICE") return@withContext true

    // DNS-Heuristik: iceportal.de über das WLAN auflösen — private IP ⇒ Zug-Netz
    withTimeoutOrNull(3_000L) {
        runCatching {
            network.getAllByName("iceportal.de").any {
                it.isSiteLocalAddress || it.isLinkLocalAddress
            }
        }.getOrDefault(false)
    } ?: false
}
