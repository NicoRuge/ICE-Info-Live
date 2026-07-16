package com.nruge.iceinfo.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class OrderRequest(
    val orderedAt: String,
    val guest: OrderGuest,
    val products: List<OrderedProduct>
)

@Serializable
data class OrderGuest(
    val seat: Int,
    val coach: Int,
    val exitStation: String
)

@Serializable
data class OrderedProduct(
    val ecmId: Int,
    val productType: String,
    val orderedOption: OrderedOption? = null,
    val orderedExtras: List<JsonElement> = emptyList()
)

@Serializable
data class OrderedOption(
    val productType: String,
    val ecmId: Int
)

@Serializable
data class OrderResponse(
    val id: String,
    val createdAt: String = "",
    val status: OrderStatusResponse
)

@Serializable
data class OrderStatusResponse(
    val color: String = "",
    val description: String = "",
    val icon: String = "",
    val statusType: String = ""
) {
    val isOpen: Boolean     get() = statusType == "OPEN"
    val isAccepted: Boolean get() = statusType == "ACCEPTED"
    val isFinal: Boolean    get() = isAccepted // extend as more states are discovered
}

data class ActiveOrder(
    val id: String,
    val status: OrderStatusResponse
)
