package com.nruge.iceinfo

import android.util.Log
import com.nruge.iceinfo.model.ApiProduct
import com.nruge.iceinfo.model.AvailabilityItem
import com.nruge.iceinfo.model.MenuCategory
import com.nruge.iceinfo.model.MenuItem
import com.nruge.iceinfo.model.MenuItemOption
import com.nruge.iceinfo.model.MENU_CATEGORY_ORDER
import com.nruge.iceinfo.util.ICE_HOSTS
import com.nruge.iceinfo.util.buildIceHttpClient
import io.ktor.client.call.*
import io.ktor.client.statement.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

data class MenuResult(
    val categories: List<MenuCategory>,
)

object MenuRepository {

    private const val API_PATH = "/bap/api/products"
    private const val AVAILABILITY_PATH = "/bap/api/availabilities"
    private val hosts = ICE_HOSTS

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    private val client = buildIceHttpClient(json)

    suspend fun fetchMenu(): MenuResult = withContext(Dispatchers.IO) {
        for (host in hosts) {
            try {
                val products = client.get("$host$API_PATH").body<List<ApiProduct>>()
                val order = MENU_CATEGORY_ORDER
                val categories = products
                    .filter { it.visible }
                    .groupBy { it.category }
                    .map { (cat, items) ->
                        MenuCategory(
                            title = cat,
                            items = items.map { p ->
                                MenuItem(
                                    id = p.ecmId,
                                    title = p.title,
                                    subject = p.description,
                                    imageUrl = p.imageUrl,
                                    eurPrice = p.prices.firstOrNull { it.currency == "EUR" }?.price,
                                    declarationKeys = p.declarations
                                        .map { it.shortDescription }
                                        .filter { it.isNotBlank() },
                                    visible = p.available,
                                    productType = p.productType,
                                    options = p.options.map { o ->
                                        MenuItemOption(
                                            id = o.ecmId,
                                            title = o.title,
                                            eurPrice = o.prices.firstOrNull { it.currency == "EUR" }?.price,
                                            available = o.available
                                        )
                                    }
                                )
                            }
                        )
                    }
                    .filter { it.items.isNotEmpty() }
                    .sortedBy { g -> order.indexOf(g.title).let { if (it < 0) Int.MAX_VALUE else it } }
                return@withContext MenuResult(categories)
            } catch (e: Exception) {
                Log.w("MenuRepo", "GET $host$API_PATH failed: ${e.message}")
            }
        }
        MenuResult(emptyList())
    }

    suspend fun fetchAvailabilities(): Map<Int, Boolean> = withContext(Dispatchers.IO) {
        for (host in hosts) {
            try {
                val items = client.get("$host$AVAILABILITY_PATH").body<List<AvailabilityItem>>()
                return@withContext items.associate { it.ecmId to (it.status == "AVAILABLE") }
            } catch (e: Exception) {
                Log.w("MenuRepo", "GET $host$AVAILABILITY_PATH failed: ${e.message}")
            }
        }
        emptyMap()
    }

    data class RawDebug(val productsRaw: String?, val productsError: String?,
                        val availabilitiesRaw: String?, val availabilitiesError: String?)

    fun endpointLabels(): List<String> = listOf("API::Menu", "API::Availabilities")

    suspend fun checkEndpoints(): List<TrainRepository.EndpointStatus> = withContext(Dispatchers.IO) {
        listOf(
            "API::Menu"          to API_PATH,
            "API::Availabilities" to AVAILABILITY_PATH,
        ).map { (label, path) ->
            var ok = false
            var detail = "Fehler"
            var body: String? = null
            for (host in hosts) {
                try {
                    val resp = client.get("$host$path")
                    ok = resp.status.isSuccess()
                    detail = if (ok) "OK" else "HTTP ${resp.status.value}"
                    body = resp.bodyAsText()
                    break
                } catch (e: Exception) {
                    detail = (e.message ?: e.javaClass.simpleName).take(60)
                }
            }
            TrainRepository.EndpointStatus(label, ok, detail, body)
        }
    }

    suspend fun fetchRaw(): RawDebug = withContext(Dispatchers.IO) {
        var productsRaw: String? = null
        var productsError: String? = null
        var availabilitiesRaw: String? = null
        var availabilitiesError: String? = null

        for (host in hosts) {
            try {
                productsRaw = client.get("$host$API_PATH").bodyAsText()
                break
            } catch (e: Exception) {
                productsError = "${e::class.simpleName}: ${e.message}"
                Log.w("MenuRepo", "fetchRaw products $host failed: ${e.message}")
            }
        }

        for (host in hosts) {
            try {
                availabilitiesRaw = client.get("$host$AVAILABILITY_PATH").bodyAsText()
                break
            } catch (e: Exception) {
                availabilitiesError = "${e::class.simpleName}: ${e.message}"
                Log.w("MenuRepo", "fetchRaw availabilities $host failed: ${e.message}")
            }
        }

        RawDebug(productsRaw, productsError, availabilitiesRaw, availabilitiesError)
    }
}
