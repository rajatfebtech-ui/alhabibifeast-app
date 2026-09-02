package com.alhabibifeast.app.data.model

import com.google.gson.annotations.SerializedName

data class AdminOrder(
    @SerializedName("order_id")      val orderId: String,
    @SerializedName("customer_name") val customerName: String = "",
    @SerializedName("customer_phone")val customerPhone: String = "",
    @SerializedName("address")       val address: String = "",
    @SerializedName("city")          val city: String = "",
    @SerializedName("pin")           val pin: String = "",
    @SerializedName("amount")        val amountPaisa: Int = 0,
    @SerializedName("total")         val total: Int = 0,
    @SerializedName("items")         val items: List<OrderItem> = emptyList(),
    @SerializedName("current_status")val currentStatus: String = "pending",
    @SerializedName("timestamp")     val timestamp: String = "",
) {
    val displayTotal get() = if (total > 0) total else amountPaisa / 100

    val statusLabel get() = when (currentStatus) {
        "pending", "pending_cod" -> "⏳ Pending"
        "confirmed"              -> "✅ Confirmed"
        "preparing"              -> "🍗 Preparing"
        "out_for_delivery"       -> "🛵 Out for Delivery"
        "delivered"              -> "📦 Delivered"
        "cancelled"              -> "❌ Cancelled"
        else                     -> currentStatus
    }

    val isPending get() = currentStatus in listOf("pending", "pending_cod")
    val isActive  get() = currentStatus !in listOf("delivered", "cancelled")
}

data class AdminOrdersResponse(
    val ok: Boolean,
    val orders: List<AdminOrder>,
    val count: Int,
)
