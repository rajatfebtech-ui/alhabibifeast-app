package com.alhabibifeast.app.ui.orders

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.alhabibifeast.app.R

class TrackingFragment : Fragment() {

    companion object {
        fun newInstance(orderId: String) = TrackingFragment().apply {
            arguments = Bundle().apply { putString("order_id", orderId) }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(R.layout.fragment_tracking, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val orderId = arguments?.getString("order_id") ?: ""
        view.findViewById<TextView>(R.id.tvOrderId)?.text = "Order #$orderId"
    }
}
