package com.nruge.iceinfo.util

import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager

// Hosts tried in order; HTTP first since iceportal.de is the train's local portal
// and HTTPS may fail with chain validation errors depending on ICE firmware version.
val ICE_HOSTS = listOf("http://iceportal.de", "https://iceportal.de")

// Shared cookie jar so session cookies set during menu fetch are reused for orders.
val icePortalCookieJar = object : CookieJar {
    private val store = mutableMapOf<String, MutableList<Cookie>>()

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        val bucket = store.getOrPut(url.host) { mutableListOf() }
        cookies.forEach { new ->
            bucket.removeAll { it.name == new.name }
            bucket.add(new)
        }
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> =
        store[url.host]?.toList() ?: emptyList()
}

/**
 * OkHttp client that skips SSL chain validation for iceportal.de.
 *
 * iceportal.de is only reachable on the ICE train's captive WLAN and uses a
 * self-signed or incomplete certificate chain depending on the train firmware.
 * Trust-all is safe here because the host is a known local appliance, not the
 * public internet.
 */
fun buildIceOkHttpClient(): OkHttpClient {
    val trustAll = object : X509TrustManager {
        override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
        override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
        override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
    }
    val sslContext = SSLContext.getInstance("TLS").apply {
        init(null, arrayOf(trustAll), SecureRandom())
    }
    return OkHttpClient.Builder()
        .sslSocketFactory(sslContext.socketFactory, trustAll)
        .hostnameVerifier { _, _ -> true }
        .cookieJar(icePortalCookieJar)
        .build()
}

fun buildIceHttpClient(json: Json, logLevel: LogLevel = LogLevel.INFO): HttpClient =
    HttpClient(OkHttp) {
        engine { preconfigured = buildIceOkHttpClient() }
        install(ContentNegotiation) {
            json(json, contentType = io.ktor.http.ContentType.Any)
        }
        install(Logging) { level = logLevel }
        defaultRequest { header("Accept", "application/json") }
    }
