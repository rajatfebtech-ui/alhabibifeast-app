package com.alhabibifeast.app.data.model

import com.google.gson.annotations.SerializedName

data class CartItem(
    val product: Product,
    var qty: Int,
) {
    val subtotal get() = product.price * qty
}

data class OrderItem(
    val id: String,
    val name: String,
    val qty: Int,
    val price: Int,
)

data class CustomerInfo(
    val name: String,
    val phone: String,
    val email: String,
    val address: String,
    val city: String,
    val pin: String,
    val notes: String = "",
)

data class LocalOrder(
    val orderId: String,
    val items: List<OrderItem>,
    val total: Int,
    val isCod: Boolean,
    val customer: CustomerInfo,
    val placedAt: Long,
    var status: String = "placed",
)

// Track order API response
data class TrackStep(
    val key: String,
    val label: String,
    val desc: String,
)

data class TrackResponse(
    val ok: Boolean,
    @SerializedName("order_id") val orderId: String = "",
    val status: String = "",
    val step: Int = 0,
    val cancelled: Boolean = false,
    val eta: String = "",
    val steps: List<TrackStep> = emptyList(),
    val payment: String = "",
    val total: Int = 0,
    val items: List<OrderItem> = emptyList(),
    @SerializedName("placed_at") val placedAt: Long = 0,
    val msg: String = "",
)

data class CodOrderRequest(
    @SerializedName("order_id") val orderId: String,
    val customer: Map<String, String>,
    val cart: Map<String, Int>,
    val total: Int,
    val items: List<OrderItem>,
)

data class ApiResult(val ok: Boolean, val msg: String = "")

data class DeliveryLocationResponse(
    val ok: Boolean,
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    @com.google.gson.annotations.SerializedName("order_id") val orderId: String = "",
    @com.google.gson.annotations.SerializedName("updated_at") val updatedAt: String? = null,
    val default: Boolean = false,
)
