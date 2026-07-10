package com.alhabibifeast.app.ui.cart

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.alhabibifeast.app.R
import com.alhabibifeast.app.data.model.CartItem
import com.bumptech.glide.Glide

class CartItemAdapter(
    private val items: List<CartItem>,
    private val onQtyChange: (productId: String, newQty: Int) -> Unit,
) : RecyclerView.Adapter<CartItemAdapter.VH>() {

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val image: ImageView = view.findViewById(R.id.cartItemImage)
        val name: TextView   = view.findViewById(R.id.cartItemName)
        val unit: TextView   = view.findViewById(R.id.cartItemUnit)
        val price: TextView  = view.findViewById(R.id.cartItemPrice)
        val btnDec: Button   = view.findViewById(R.id.cartDec)
        val tvQty: TextView  = view.findViewById(R.id.cartQty)
        val btnInc: Button   = view.findViewById(R.id.cartInc)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_cart, parent, false))

    override fun getItemCount() = items.size

    override fun onBindViewHolder(h: VH, position: Int) {
        val item = items[position]
        val p    = item.product
        Glide.with(h.image).load(p.image).centerCrop().into(h.image)
        h.name.text  = p.name
        h.unit.text  = p.unit
        h.price.text = "₹${p.price * item.qty}"
        h.tvQty.text = item.qty.toString()
        h.btnInc.setOnClickListener { onQtyChange(p.id, item.qty + 1) }
        h.btnDec.setOnClickListener { onQtyChange(p.id, item.qty - 1) }
    }
}
