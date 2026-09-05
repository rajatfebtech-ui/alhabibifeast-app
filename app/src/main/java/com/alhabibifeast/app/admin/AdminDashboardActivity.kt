package com.alhabibifeast.app.admin

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.alhabibifeast.app.R
import com.alhabibifeast.app.data.api.ApiClient
import com.alhabibifeast.app.data.model.UpdateStatusRequest
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import kotlinx.coroutines.launch

class AdminDashboardActivity : AppCompatActivity() {

    companion object {
        const val ADMIN_TOKEN    = "ahf_admin_2024"
        private const val REFRESH_MS = 30_000L
    }

    private var allOrders    = listOf<com.alhabibifeast.app.data.model.AdminOrder>()
    private var activeFilter = "active"
    private lateinit var adapter: AdminOrderAdapter
    private lateinit var tvEmpty:  TextView
    private lateinit var progress: ProgressBar

    private val refreshHandler = Handler(Looper.getMainLooper())
    private val refreshRunnable = object : Runnable {
        override fun run() {
            silentRefresh()
            refreshHandler.postDelayed(this, REFRESH_MS)
        }
    }

    private val filters = listOf(
        "Active"    to "active",
        "Pending"   to "pending",
        "Confirmed" to "confirmed",
        "Preparing" to "preparing",
        "Dispatch"  to "out_for_delivery",
        "Delivered" to "delivered",
        "All"       to "all",
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_admin_dashboard)

        val rv       = findViewById<RecyclerView>(R.id.rvAdminOrders)
        val swipe    = findViewById<SwipeRefreshLayout>(R.id.swipeAdmin)
        val chips    = findViewById<ChipGroup>(R.id.adminFilterChips)
        progress     = findViewById(R.id.adminProgress)
        tvEmpty      = findViewById(R.id.tvAdminEmpty)
        val btnLogout= findViewById<Button>(R.id.btnAdminLogout)

        adapter = AdminOrderAdapter(emptyList()) { orderId, status ->
            updateStatus(orderId, status)
        }
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = adapter

        filters.forEach { (label, key) ->
            chips.addView(Chip(this).apply {
                text = label
                isCheckable = true
                isChecked   = key == activeFilter
                setOnClickListener { activeFilter = key; applyFilter() }
            })
        }

        swipe.setColorSchemeResources(R.color.terra)
        swipe.setOnRefreshListener {
            loadOrders(showProgress = false) { swipe.isRefreshing = false }
        }

        btnLogout.setOnClickListener {
            getSharedPreferences("ahf_admin", Context.MODE_PRIVATE).edit().clear().apply()
            OrderMonitorService.stop(this)
            finish()
        }

        OrderMonitorService.start(this)
        loadOrders(showProgress = true)
    }

    override fun onResume() {
        super.onResume()
        refreshHandler.postDelayed(refreshRunnable, REFRESH_MS)
    }

    override fun onPause() {
        super.onPause()
        refreshHandler.removeCallbacks(refreshRunnable)
    }

    private fun silentRefresh() {
        loadOrders(showProgress = false)
    }

    private fun loadOrders(showProgress: Boolean, onDone: (() -> Unit)? = null) {
        lifecycleScope.launch {
            if (showProgress) progress.visibility = View.VISIBLE
            try {
                val resp = ApiClient.api.getAdminOrders(ADMIN_TOKEN)
                if (resp.ok) {
                    allOrders = resp.orders
                    applyFilter()
                }
            } catch (e: Exception) {
                if (showProgress)
                    Toast.makeText(this@AdminDashboardActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                progress.visibility = View.GONE
                onDone?.invoke()
            }
        }
    }

    private fun applyFilter() {
        val filtered = when (activeFilter) {
            "all"    -> allOrders
            "active" -> allOrders.filter { it.isActive }
            else     -> allOrders.filter {
                it.currentStatus == activeFilter ||
                (activeFilter == "pending" && it.currentStatus == "pending_cod")
            }
        }
        adapter.updateOrders(filtered)
        tvEmpty.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
        tvEmpty.text = if (allOrders.isEmpty()) "No orders yet" else "No orders in this category"
    }

    private fun updateStatus(orderId: String, status: String) {
        lifecycleScope.launch {
            try {
                val result = ApiClient.api.updateOrderStatus(
                    UpdateStatusRequest(token = ADMIN_TOKEN, orderId = orderId, status = status)
                )
                if (result.ok) {
                    val label = when (status) {
                        "confirmed"        -> "Confirmed ✅"
                        "preparing"        -> "Preparing 🍗"
                        "out_for_delivery" -> "Out for Delivery 🛵"
                        "delivered"        -> "Delivered 📦"
                        else               -> status
                    }
                    Toast.makeText(this@AdminDashboardActivity, "#$orderId → $label", Toast.LENGTH_SHORT).show()
                    loadOrders(showProgress = false)
                } else {
                    Toast.makeText(this@AdminDashboardActivity, "Update failed", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@AdminDashboardActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
