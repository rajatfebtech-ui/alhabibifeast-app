package com.alhabibifeast.app.ui.menu

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.alhabibifeast.app.R
import com.alhabibifeast.app.data.CartManager
import com.alhabibifeast.app.data.model.Product
import com.bumptech.glide.Glide

class ProductAdapter(
    private var products: List<Product>,
    private val onAddToCart: (Product, Int) -> Unit,
) : RecyclerView.Adapter<ProductAdapter.VH>() {

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val image: ImageView  = view.findViewById(R.id.productImage)
        val badge: TextView   = view.findViewById(R.id.productBadge)
        val category: TextView = view.findViewById(R.id.productCategory)
        val name: TextView    = view.findViewById(R.id.productName)
        val desc: TextView    = view.findViewById(R.id.productDesc)
        val price: TextView   = view.findViewById(R.id.productPrice)
        val unit: TextView    = view.findViewById(R.id.productUnit)
        val btnDec: Button    = view.findViewById(R.id.btnDec)
        val btnInc: Button    = view.findViewById(R.id.btnInc)
        val tvQty: TextView   = view.findViewById(R.id.tvQty)
        val btnAdd: Button    = view.findViewById(R.id.btnAdd)
        val qtyRow: View      = view.findViewById(R.id.qtyRow)
        val oosLabel: TextView = view.findViewById(R.id.oosLabel)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_product, parent, false))

    override fun getItemCount() = products.size

    override fun onBindViewHolder(h: VH, position: Int) {
        val p   = products[position]
        val qty = CartManager.getQty(p.id)

        Glide.with(h.image).load(p.image).placeholder(R.drawable.ic_notification)
            .centerCrop().into(h.image)

        h.category.text = p.catLabel
        h.name.text     = p.name
        h.desc.text     = p.desc
        h.price.text    = "₹${p.price}"
        h.unit.text     = "/ ${p.unit}"

        // Badge
        when {
            p.isBestseller -> { h.badge.visibility = View.VISIBLE; h.badge.text = "Bestseller" }
            p.isPremium    -> { h.badge.visibility = View.VISIBLE; h.badge.text = "Premium" }
            else           -> h.badge.visibility = View.GONE
        }

        // Out of stock
        if (p.isOutOfStock) {
            h.oosLabel.visibility = View.VISIBLE
            h.btnAdd.visibility  = View.GONE
            h.qtyRow.visibility  = View.GONE
            return
        }
        h.oosLabel.visibility = View.GONE

        // Cart state
        refreshQtyUI(h, p, qty)

        h.btnAdd.setOnClickListener {
            onAddToCart(p, 1)
            refreshQtyUI(h, p, CartManager.getQty(p.id))
        }
        h.btnInc.setOnClickListener {
            CartManager.addItem(p, 1)
            refreshQtyUI(h, p, CartManager.getQty(p.id))
        }
        h.btnDec.setOnClickListener {
            CartManager.addItem(p, -1)
            refreshQtyUI(h, p, CartManager.getQty(p.id))
        }
    }

    private fun refreshQtyUI(h: VH, p: Product, qty: Int) {
        if (qty > 0) {
            h.btnAdd.visibility = View.GONE
            h.qtyRow.visibility = View.VISIBLE
            h.tvQty.text = qty.toString()
        } else {
            h.btnAdd.visibility = View.VISIBLE
            h.qtyRow.visibility = View.GONE
        }
    }

    fun updateProducts(newList: List<Product>) {
        products = newList
        notifyDataSetChanged()
    }
}
