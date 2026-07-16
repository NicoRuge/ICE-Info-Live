package com.nruge.iceinfo

import android.util.Log
import com.nruge.iceinfo.model.*
import com.nruge.iceinfo.util.ICE_HOSTS
import com.nruge.iceinfo.util.buildIceHttpClient
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.time.Instant

object OrderRepository {

    private const val ORDERS_PATH = "/bap/api/orders"

    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }
    private val client = buildIceHttpClient(json)

    suspend fun placeOrder(
        seat: Int,
        coach: Int,
        exitStation: String,
        item: MenuItem,
        option: MenuItemOption? = null
    ): OrderResponse = withContext(Dispatchers.IO) {
        val request = OrderRequest(
            orderedAt = Instant.now().toString(),
            guest = OrderGuest(seat = seat, coach = coach, exitStation = exitStation),
            products = listOf(
                OrderedProduct(
                    ecmId = item.id,
                    productType = item.productType,
                    orderedOption = option?.let { OrderedOption(productType = "ARTICLE", ecmId = it.id) }
                )
            )
        )
        var lastException: Exception? = null
        for (host in ICE_HOSTS) {
            try {
                return@withContext client.post("$host$ORDERS_PATH") {
                    contentType(ContentType.Application.Json)
                    setBody(request)
                }.body<OrderResponse>()
            } catch (e: Exception) {
                Log.w("OrderRepo", "POST $host$ORDERS_PATH failed: ${e.message}")
                lastException = e
            }
        }
        throw lastException ?: IllegalStateException("Order failed")
    }

    suspend fun getOrderStatus(orderId: String): OrderStatusResponse = withContext(Dispatchers.IO) {
        var lastException: Exception? = null
        for (host in ICE_HOSTS) {
            try {
                return@withContext client.get("$host$ORDERS_PATH/$orderId/status").body<OrderStatusResponse>()
            } catch (e: Exception) {
                Log.w("OrderRepo", "GET status failed: ${e.message}")
                lastException = e
            }
        }
        throw lastException ?: IllegalStateException("Status fetch failed")
    }
}
