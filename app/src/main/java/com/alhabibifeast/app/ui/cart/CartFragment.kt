package com.alhabibifeast.app.ui.cart

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.alhabibifeast.app.MainActivity
import com.alhabibifeast.app.R
import com.alhabibifeast.app.data.CartManager
import com.alhabibifeast.app.data.api.ApiClient
import com.alhabibifeast.app.data.model.CartItem
import com.alhabibifeast.app.data.model.CodOrderRequest
import com.alhabibifeast.app.data.model.CustomerInfo
import com.alhabibifeast.app.data.model.LocalOrder
import com.alhabibifeast.app.data.model.OrderItem
import android.view.WindowManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

class CartFragment : Fragment() {

    private val FREE_SHIP = 999
    private val SHIP_FEE  = 49
    private val WHATSAPP  = "917500000000"  // ← Change to real number

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(R.layout.fragment_cart, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        renderCart(view)
    }

    private fun renderCart(view: View) {
        val rv          = view.findViewById<RecyclerView>(R.id.rvCart) ?: return
        val tvSubtotal  = view.findViewById<TextView>(R.id.tvSubtotal) ?: return
        val tvShipping  = view.findViewById<TextView>(R.id.tvShipping) ?: return
        val tvTotal     = view.findViewById<TextView>(R.id.tvTotal) ?: return
        val btnCod      = view.findViewById<Button>(R.id.btnCod) ?: return
        val btnOnline   = view.findViewById<Button>(R.id.btnOnline) ?: return
        val btnWhatsApp = view.findViewById<Button>(R.id.btnWhatsapp) ?: return
        val emptyView   = view.findViewById<View>(R.id.cartEmpty) ?: return
        val checkoutCard= view.findViewById<View>(R.id.checkoutCard) ?: return

        lifecycleScope.launch {
            try {
                val resp  = ApiClient.api.getProducts()
                val items = CartManager.getCartItems(resp.products)

                if (items.isEmpty()) {
                    emptyView.visibility    = View.VISIBLE
                    checkoutCard.visibility = View.GONE
                    rv.visibility           = View.GONE
                    return@launch
                }

                emptyView.visibility    = View.GONE
                checkoutCard.visibility = View.VISIBLE
                rv.visibility           = View.VISIBLE

                val sub   = items.sumOf { it.subtotal }
                val ship  = if (sub >= FREE_SHIP) 0 else SHIP_FEE
                val total = sub + ship

                tvSubtotal.text = "₹$sub"
                tvShipping.text = if (ship == 0) "FREE" else "₹$ship"
                tvTotal.text    = "₹$total"

                if (isAdded) {
                    rv.layoutManager = LinearLayoutManager(requireContext())
                    rv.adapter = CartItemAdapter(items) { productId, qty ->
                        CartManager.setQty(productId, qty)
                        (activity as? MainActivity)?.updateCartBadge()
                        renderCart(view)
                    }
                    btnCod.setOnClickListener { showAddressSheet(items, total, true) }
                    btnOnline.setOnClickListener { openWebCheckout() }
                    btnWhatsApp.setOnClickListener { openWhatsApp(items, total) }
                }

            } catch (t: Throwable) {
                if (isAdded) {
                    try {
                        val msg = "CartFragment: ${t.javaClass.name}: ${t.message}\n" +
                            t.stackTrace.take(6).joinToString("\n") { "  at ${it.className}.${it.methodName}(${it.fileName}:${it.lineNumber})" }
                        requireContext().getSharedPreferences("ahf_crash", android.content.Context.MODE_PRIVATE)
                            .edit().putString("last", msg).commit()
                        Toast.makeText(requireContext(), "Error loading cart. Reopen app for details.", Toast.LENGTH_LONG).show()
                    } catch (_: Throwable) {}
                }
            }
        }
    }

    private fun showAddressSheet(items: List<CartItem>, total: Int, isCod: Boolean) {
        val sheet = BottomSheetDialog(requireContext(), com.google.android.material.R.style.Theme_Material3_Light_BottomSheetDialog)
        sheet.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        val sv = layoutInflater.inflate(R.layout.sheet_checkout, null)
        sheet.setContentView(sv)
        sheet.setOnShowListener {
            val bs = sheet.findViewById<android.widget.FrameLayout>(com.google.android.material.R.id.design_bottom_sheet)
            bs?.let { BottomSheetBehavior.from(it).state = BottomSheetBehavior.STATE_EXPANDED }
        }

        sv.findViewById<Button>(R.id.btnConfirmOrder).setOnClickListener {
            val name    = sv.findViewById<TextInputEditText>(R.id.etName).text.toString().trim()
            val phone   = sv.findViewById<TextInputEditText>(R.id.etPhone).text.toString().trim()
            val address = sv.findViewById<TextInputEditText>(R.id.etAddress).text.toString().trim()
            val city    = sv.findViewById<TextInputEditText>(R.id.etCity).text.toString().trim()
            val pin     = sv.findViewById<TextInputEditText>(R.id.etPin).text.toString().trim()

            if (name.isEmpty() || phone.isEmpty() || address.isEmpty() || pin.isEmpty()) {
                Toast.makeText(requireContext(), "Please fill all required fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (pin != "247667") {
                Toast.makeText(requireContext(), "Sorry, we deliver only in Roorkee (PIN 247667)", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            sheet.dismiss()
            placeCodOrder(name, phone, address, city, pin, items, total)
        }
        sheet.show()
    }

    private fun placeCodOrder(name: String, phone: String, address: String, city: String, pin: String, items: List<CartItem>, total: Int) {
        val orderId  = "AHF" + System.currentTimeMillis().toString().takeLast(8)
        val customer = CustomerInfo(name, phone, "", address, city, pin)
        val orderItems = items.map { OrderItem(it.product.id, it.product.name, it.qty, it.product.price) }

        lifecycleScope.launch {
            try {
                ApiClient.api.placeCodOrder(CodOrderRequest(
                    orderId  = orderId,
                    customer = mapOf("name" to name, "phone" to phone, "address" to address, "city" to city, "pin" to pin),
                    cart     = CartManager.getRawCartMap(),
                    total    = total,
                    items    = orderItems,
                ))
            } catch (_: Exception) {}

            CartManager.saveOrder(LocalOrder(orderId, orderItems, total, true, customer, System.currentTimeMillis()))
            CartManager.clearCart()
            (activity as? MainActivity)?.updateCartBadge()
            Toast.makeText(requireContext(), "Order placed! #$orderId\nWe'll call you before delivery.", Toast.LENGTH_LONG).show()
            view?.let { renderCart(it) }
        }
    }

    private fun openWebCheckout() {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://alhabibifeast.in/#/checkout")))
    }

    private fun openWhatsApp(items: List<CartItem>, total: Int) {
        val itemList = items.joinToString("\n") { "• ${it.product.name} × ${it.qty} — ₹${it.subtotal}" }
        val msg = "Hello! I'd like to place an order:\n$itemList\nTotal: ₹$total\n\nPlease confirm."
        val url = "https://wa.me/$WHATSAPP?text=${Uri.encode(msg)}"
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (_: Exception) {
            Toast.makeText(requireContext(), "WhatsApp not installed", Toast.LENGTH_SHORT).show()
        }
    }
}
