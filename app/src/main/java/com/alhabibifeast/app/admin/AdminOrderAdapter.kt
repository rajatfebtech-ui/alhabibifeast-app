package com.alhabibifeast.app.admin

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.alhabibifeast.app.R
import com.alhabibifeast.app.data.model.AdminOrder

class AdminOrderAdapter(
    private var orders: List<AdminOrder>,
    private val onStatusChange: (orderId: String, newStatus: String) -> Unit,
) : RecyclerView.Adapter<AdminOrderAdapter.VH>() {

    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        val tvId      = v.findViewById<TextView>(R.id.tvOrderId)
        val tvStatus  = v.findViewById<TextView>(R.id.tvOrderStatus)
        val tvName    = v.findViewById<TextView>(R.id.tvCustomerName)
        val tvPhone   = v.findViewById<TextView>(R.id.tvCustomerPhone)
        val tvAddr    = v.findViewById<TextView>(R.id.tvOrderAddress)
        val tvItems   = v.findViewById<TextView>(R.id.tvOrderItems)
        val tvTotal   = v.findViewById<TextView>(R.id.tvOrderTotal)
        val btnConf   = v.findViewById<Button>(R.id.btnConfirmOrder)
        val btnPrep   = v.findViewById<Button>(R.id.btnPreparing)
        val btnDisp   = v.findViewById<Button>(R.id.btnOutForDelivery)
        val btnDel    = v.findViewById<Button>(R.id.btnDelivered)
        val btnCall   = v.findViewById<Button>(R.id.btnCallCustomer)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_admin_order, parent, false))

    override fun getItemCount() = orders.size

    override fun onBindViewHolder(h: VH, pos: Int) {
        val o = orders[pos]
        h.tvId.text     = "#${o.orderId}"
        h.tvStatus.text = o.statusLabel
        h.tvName.text   = "👤 ${o.customerName.ifBlank { "N/A" }}"
        h.tvPhone.text  = "📱 ${o.customerPhone.ifBlank { "N/A" }}"
        h.tvAddr.text   = "📍 ${o.address.ifBlank { "" }}, ${o.city} ${o.pin}".trim().trimEnd(',')
        h.tvTotal.text  = "Total: ₹${o.displayTotal}"

        val itemSummary = o.items.joinToString(", ") { "${it.name} × ${it.qty}" }
        h.tvItems.text = itemSummary.ifBlank { "Items not available" }

        // Status chip color
        val statusColor = when (o.currentStatus) {
            "pending", "pending_cod" -> 0xFFB71C1C.toInt()
            "confirmed"              -> 0xFF2E7D32.toInt()
            "preparing"              -> 0xFFE65100.toInt()
            "out_for_delivery"       -> 0xFF1565C0.toInt()
            "delivered"              -> 0xFF4A148C.toInt()
            else                     -> 0xFF757575.toInt()
        }
        h.tvStatus.setBackgroundColor(statusColor)

        h.btnConf.setOnClickListener { onStatusChange(o.orderId, "confirmed") }
        h.btnPrep.setOnClickListener { onStatusChange(o.orderId, "preparing") }
        h.btnDisp.setOnClickListener { onStatusChange(o.orderId, "out_for_delivery") }
        h.btnDel.setOnClickListener  { onStatusChange(o.orderId, "delivered") }

        h.btnCall.setOnClickListener {
            val phone = o.customerPhone.filter { it.isDigit() }
            if (phone.isNotEmpty()) {
                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
                h.itemView.context.startActivity(intent)
            }
        }
    }

    fun updateOrders(newOrders: List<AdminOrder>) {
        orders = newOrders
        notifyDataSetChanged()
    }
}
