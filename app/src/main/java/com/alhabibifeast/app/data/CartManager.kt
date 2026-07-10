package com.alhabibifeast.app.data

import android.content.Context
import android.content.SharedPreferences
import com.alhabibifeast.app.data.model.CartItem
import com.alhabibifeast.app.data.model.LocalOrder
import com.alhabibifeast.app.data.model.Product
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object CartManager {
    private const val PREF_CART   = "ahf_cart"
    private const val PREF_ORDERS = "ahf_orders"
    private const val KEY_ITEMS   = "items"
    private const val KEY_ORDERS  = "orders"

    private val gson = Gson()
    private lateinit var cartPrefs: SharedPreferences
    private lateinit var orderPrefs: SharedPreferences

    fun init(context: Context) {
        cartPrefs  = context.getSharedPreferences(PREF_CART,   Context.MODE_PRIVATE)
        orderPrefs = context.getSharedPreferences(PREF_ORDERS, Context.MODE_PRIVATE)
    }

    // ---- Cart ----
    private fun loadCartItems(): MutableMap<String, Int> {
        val json = cartPrefs.getString(KEY_ITEMS, "{}") ?: "{}"
        val type = object : TypeToken<MutableMap<String, Int>>() {}.type
        return gson.fromJson(json, type) ?: mutableMapOf()
    }

    private fun saveCartItems(map: Map<String, Int>) {
        cartPrefs.edit().putString(KEY_ITEMS, gson.toJson(map)).apply()
    }

    fun addItem(product: Product, qty: Int = 1) {
        val map = loadCartItems()
        map[product.id] = (map[product.id] ?: 0) + qty
        if ((map[product.id] ?: 0) < 1) map.remove(product.id)
        saveCartItems(map)
    }

    fun setQty(productId: String, qty: Int) {
        val map = loadCartItems()
        if (qty < 1) map.remove(productId) else map[productId] = qty
        saveCartItems(map)
    }

    fun removeItem(productId: String) {
        val map = loadCartItems()
        map.remove(productId)
        saveCartItems(map)
    }

    fun getCartItems(products: List<Product>): List<CartItem> {
        val map = loadCartItems()
        return map.entries.mapNotNull { (id, qty) ->
            val p = products.find { it.id == id } ?: return@mapNotNull null
            CartItem(p, qty)
        }
    }

    fun getQty(productId: String): Int = loadCartItems()[productId] ?: 0

    fun getTotalCount(): Int = loadCartItems().values.sum()

    fun getSubtotal(products: List<Product>): Int =
        getCartItems(products).sumOf { it.subtotal }

    fun getRawCartMap(): Map<String, Int> = loadCartItems()

    fun clearCart() { cartPrefs.edit().remove(KEY_ITEMS).apply() }

    // ---- Orders ----
    fun saveOrder(order: LocalOrder) {
        val list = getOrders().toMutableList()
        list.add(0, order)
        orderPrefs.edit().putString(KEY_ORDERS, gson.toJson(list)).apply()
    }

    fun getOrders(): List<LocalOrder> {
        val json = orderPrefs.getString(KEY_ORDERS, "[]") ?: "[]"
        val type = object : TypeToken<List<LocalOrder>>() {}.type
        return gson.fromJson(json, type) ?: emptyList()
    }
}
