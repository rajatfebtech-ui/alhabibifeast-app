package com.alhabibifeast.app.ui.orders

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.alhabibifeast.app.R
import com.alhabibifeast.app.data.CartManager
import com.alhabibifeast.app.data.api.ApiClient
import com.alhabibifeast.app.data.model.LocalOrder
import com.alhabibifeast.app.data.model.TrackResponse
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class OrdersFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(R.layout.fragment_orders, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val container = view.findViewById<LinearLayout>(R.id.ordersContainer)
        val emptyView = view.findViewById<TextView>(R.id.tvOrdersEmpty)
        val orders    = CartManager.getOrders()

        if (orders.isEmpty()) {
            emptyView.visibility  = View.VISIBLE
            container.visibility = View.GONE
            return
        }

        emptyView.visibility  = View.GONE
        container.visibility = View.VISIBLE

        orders.forEach { order -> addOrderCard(container, order) }
    }

    private fun addOrderCard(container: LinearLayout, order: LocalOrder) {
        val card = layoutInflater.inflate(R.layout.item_order, container, false)

        card.findViewById<TextView>(R.id.tvOrderId).text = "#${order.orderId}"
        card.findViewById<TextView>(R.id.tvOrderDate).text =
            SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(order.placedAt))
        card.findViewById<TextView>(R.id.tvOrderTotal).text = "₹${order.total}"
        card.findViewById<TextView>(R.id.tvOrderPayment).text = if (order.isCod) "Cash on Delivery" else "Online Paid"
        card.findViewById<TextView>(R.id.tvOrderItems).text =
            order.items.joinToString("  •  ") { "${it.name} ×${it.qty}" }

        val trackCard  = card.findViewById<View>(R.id.trackingCard)
        val btnTrack   = card.findViewById<TextView>(R.id.btnTrack)
        val trackSteps = card.findViewById<LinearLayout>(R.id.trackSteps)
        val tvEta      = card.findViewById<TextView>(R.id.tvEta)
        val progress   = card.findViewById<ProgressBar>(R.id.trackProgress)

        var tracking = false
        btnTrack.setOnClickListener {
            if (!tracking) {
                tracking = true
                trackCard.visibility = View.VISIBLE
                btnTrack.text = "Hide tracking"
                loadTracking(order.orderId, trackSteps, tvEta, progress)
            } else {
                tracking = false
                trackCard.visibility = View.GONE
                btnTrack.text = "Track order ›"
            }
        }

        container.addView(card)
    }

    private fun loadTracking(orderId: String, trackSteps: LinearLayout, tvEta: TextView, progress: ProgressBar) {
        lifecycleScope.launch {
            progress.visibility = View.VISIBLE
            try {
                val resp = ApiClient.api.trackOrder(orderId)
                progress.visibility = View.GONE
                renderTracking(resp, trackSteps, tvEta)
            } catch (_: Exception) {
                progress.visibility = View.GONE
                tvEta.text = "Could not fetch tracking. Check connection."
            }
        }
    }

    private fun renderTracking(resp: TrackResponse, trackSteps: LinearLayout, tvEta: TextView) {
        tvEta.text = resp.eta.ifEmpty { "Tracking unavailable" }
        trackSteps.removeAllViews()

        resp.steps.forEachIndexed { i, step ->
            val done   = !resp.cancelled && i <= resp.step
            val active = !resp.cancelled && i == resp.step

            val row = layoutInflater.inflate(R.layout.item_track_step, trackSteps, false)
            val dot  = row.findViewById<View>(R.id.stepDot)
            val tvLabel = row.findViewById<TextView>(R.id.stepLabel)
            val tvDesc  = row.findViewById<TextView>(R.id.stepDesc)

            tvLabel.text = step.label
            tvDesc.text  = step.desc

            val ctx = requireContext()
            when {
                active -> {
                    dot.background = ContextCompat.getDrawable(ctx, R.drawable.dot_active)
                    tvLabel.setTextColor(ContextCompat.getColor(ctx, R.color.terra))
                }
                done -> {
                    dot.background = ContextCompat.getDrawable(ctx, R.drawable.dot_done)
                    tvLabel.setTextColor(ContextCompat.getColor(ctx, R.color.ok))
                }
                else -> {
                    dot.background = ContextCompat.getDrawable(ctx, R.drawable.dot_pending)
                    tvLabel.setTextColor(ContextCompat.getColor(ctx, R.color.muted))
                }
            }

            trackSteps.addView(row)
        }
    }
}
