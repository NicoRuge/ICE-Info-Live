package com.nruge.iceinfo.model

import kotlinx.serialization.Serializable

// --- API response models (flat array from /bap/api/products) ---

@Serializable
data class ApiProduct(
    val ecmId: Int = 0,
    val category: String = "",
    val title: String = "",
    val description: String = "",
    val visible: Boolean = true,
    val available: Boolean = true,
    val imageUrl: String = "",
    val declarations: List<ApiDeclaration> = emptyList(),
    val prices: List<ApiPrice> = emptyList(),
    val productType: String = "ARTICLE",
    val options: List<ApiProductOption> = emptyList()
)

@Serializable
data class ApiProductOption(
    val ecmId: Int = 0,
    val title: String = "",
    val productType: String = "ARTICLE",
    val prices: List<ApiPrice> = emptyList(),
    val available: Boolean = true
)

@Serializable
data class ApiDeclaration(
    val shortDescription: String = "",
    val description: String = ""
)

@Serializable
data class ApiPrice(
    val currency: String = "",
    val price: Double = 0.0
)

@Serializable
data class AvailabilityItem(
    val ecmId: Int = 0,
    val status: String = "",
    val visible: Boolean = true
)

// --- Internal UI models ---

data class MenuItemOption(
    val id: Int,
    val title: String,
    val eurPrice: Double?,
    val available: Boolean
)

data class MenuItem(
    val id: Int,
    val title: String,
    val subject: String,
    val imageUrl: String,
    val eurPrice: Double?,
    val declarationKeys: List<String>,
    val visible: Boolean,
    val productType: String = "ARTICLE",
    val options: List<MenuItemOption> = emptyList()
) {
    val isOrderable: Boolean get() = visible && (options.isEmpty() || options.any { it.available })
    val hasOptions: Boolean get() = productType == "OPTION_ARTICLE" && options.isNotEmpty()
}

data class MenuCategory(
    val title: String,
    val items: List<MenuItem>
)

val MENU_CATEGORY_ORDER = listOf(
    "Aktion", "Snacks", "Hauptgerichte", "Frühstück", "Süß & salzig",
    "Suppe", "Kindermenü", "Vegetarisch", "Vegan", "Heißgetränke",
    "Kaltgetränke", "Alkoholische Getränke"
)
