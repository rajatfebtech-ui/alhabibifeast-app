package com.alhabibifeast.app.data.model

import com.google.gson.annotations.SerializedName

data class Product(
    val id: String,
    val cat: String,
    @SerializedName("catLabel") val catLabel: String,
    val name: String,
    val unit: String,
    val price: Int,
    val stock: Int = 99,
    val tags: List<String> = emptyList(),
    val desc: String,
    val image: String,
) {
    val isBestseller get() = tags.contains("bestseller")
    val isPremium get() = tags.contains("premium")
    val isOutOfStock get() = stock == 0
}

data class ProductsResponse(
    val ok: Boolean,
    val products: List<Product>,
    val count: Int,
)
