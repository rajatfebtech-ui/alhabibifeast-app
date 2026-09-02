package com.alhabibifeast.app.data.model

import com.google.gson.annotations.SerializedName

data class UpdateStatusRequest(
    val token: String,
    @SerializedName("order_id") val orderId: String,
    val status: String,
)
