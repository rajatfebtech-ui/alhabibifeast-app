package com.alhabibifeast.app.rider

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.alhabibifeast.app.R
import com.alhabibifeast.app.data.api.ApiClient
import com.alhabibifeast.app.data.model.RiderOrder
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class RiderDashboardActivity : AppCompatActivity() {

    private val handler = Handler(Looper.getMainLooper())
    private var lastOrderIds = setOf<String>()
    private lateinit var riderId: String
    private lateinit var riderName: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rider_dashboard)

        val prefs = getSharedPreferences("ahf_rider", Context.MODE_PRIVATE)
        riderId   = prefs.getString("rider_id", "") ?: ""
        riderName = prefs.getString("rider_name", "Rider") ?: "Rider"

        if (riderId.isEmpty()) {
            startActivity(Intent(this, RiderLoginActivity::class.java))
            finish(); return
        }

        findViewById<TextView>(R.id.tvRiderName).text = "Welcome, $riderName"

        findViewById<Button>(R.id.btnRiderLogout).setOnClickListener {
            prefs.edit().clear().apply()
            startActivity(Intent(this, RiderLoginActivity::class.java))
            finish()
        }

        startPolling()
    }

    private fun startPolling() {
        pollOrders()
        handler.postDelayed(object : Runnable {
            override fun run() {
                pollOrders()
                handler.postDelayed(this, 10_000)
            }
        }, 10_000)
    }

    private fun pollOrders() {
        lifecycleScope.launch {
            try {
                val resp = ApiClient.api.getPendingOrders(riderId)
                if (resp.ok) renderOrders(resp.orders)
            } catch (_: Exception) {}
        }
    }

    private fun renderOrders(orders: List<RiderOrder>) {
        val container = findViewById<LinearLayout>(R.id.riderOrdersContainer)
        val tvEmpty   = findViewById<TextView>(R.id.tvRiderEmpty)
        val newIds    = orders.map { it.orderId }.toSet()

        // Vibrate for new orders
        if (newIds.isNotEmpty() && newIds != lastOrderIds) {
            val vib = getSystemService(android.content.Context.VIBRATOR_SERVICE) as android.os.Vibrator
            @Suppress("DEPRECATION")
            vib.vibrate(500)
        }
        lastOrderIds = newIds

        container.removeAllViews()
        if (orders.isEmpty()) {
            tvEmpty.visibility  = View.VISIBLE
            container.visibility = View.GONE
            return
        }
        tvEmpty.visibility  = View.GONE
        container.visibility = View.VISIBLE

        orders.forEach { order -> addOrderCard(container, order) }
    }

    private fun addOrderCard(container: LinearLayout, order: RiderOrder) {
        val card = LayoutInflater.from(this).inflate(R.layout.item_rider_order, container, false)
        card.findViewById<TextView>(R.id.tvRiderOrderId).text   = "#${order.orderId}"
        card.findViewById<TextView>(R.id.tvRiderCustomer).text  = order.customerName
        card.findViewById<TextView>(R.id.tvRiderPhone).text     = order.customerPhone
        card.findViewById<TextView>(R.id.tvRiderAddress).text   = order.address
        card.findViewById<TextView>(R.id.tvRiderTotal).text     = "₹${order.total}"
        card.findViewById<TextView>(R.id.tvRiderItems).text     =
            order.items.joinToString(" • ") { "${it.name}×${it.qty}" }
        card.findViewById<TextView>(R.id.tvRiderTime).text      =
            SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(order.placedAt))

        val btnAccept = card.findViewById<Button>(R.id.btnAcceptOrder)
        btnAccept.setOnClickListener {
            btnAccept.isEnabled = false
            btnAccept.text = "Accepting..."
            lifecycleScope.launch {
                try {
                    val resp = ApiClient.api.acceptOrder(order.orderId, riderId, riderName)
                    if (resp.ok) {
                        Toast.makeText(this@RiderDashboardActivity,
                            "✅ Order #${order.orderId} assigned to you!", Toast.LENGTH_LONG).show()
                        pollOrders()
                    } else {
                        Toast.makeText(this@RiderDashboardActivity,
                            resp.msg.ifEmpty { "Already taken by another rider" }, Toast.LENGTH_LONG).show()
                        btnAccept.isEnabled = true
                        btnAccept.text = "Accept"
                        pollOrders()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this@RiderDashboardActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    btnAccept.isEnabled = true
                    btnAccept.text = "Accept"
                }
            }
        }
        container.addView(card)
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }
}
